import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper; // Add Jackson for JSON processing

public class Main {
    private static final String DB_URL = "jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db";
    private static CutrePool connectionsPool;
    private static List<String> tickers;
    private static StringBuilder htmlResponse = new StringBuilder();
    private static ExecutorService analysisExecutor;

    public static void main(String[] args) throws SQLException {
        ensureDataDirectoryExists();
        long startTime = System.nanoTime();

        try {
            connectionsPool = new CutrePool(DB_URL);
            SQLiteDB.initializeDatabase(connectionsPool);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (connectionsPool != null) {
            Downloader.setConnectionPool(connectionsPool);
            ExecutorService downloaderExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

            // Get Ticklers once and use it throughout the application
            tickers = Downloader.getAllTickers();

            // Start the HTTP server
            startHttpServer();

            // Proceed with downloading and processing tickers in separate threads
            try {
                long tickersStartTime = System.nanoTime();
                try {
                    TickerScraper.processTickers(connectionsPool.getConnection(), "https://www.mse.mk/en/issuers/free-market");
                    TickerScraper.processTickers(connectionsPool.getConnection(), "https://www.mse.mk/en/issuers/JSC-with-special-reporting-obligations");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                long tickersEndTime = System.nanoTime();
                System.out.println("Time to get tickers: " + (tickersEndTime - tickersStartTime) / 1_000_000 + " ms");

                for (String ticker : tickers) {
                    downloaderExecutor.submit(() -> {
                        try {
                            Downloader.populateHistoricalData(ticker);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }

                downloaderExecutor.shutdown();
                downloaderExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                long downloadEndTime = System.nanoTime();
                System.out.println("Time to download historical data: " + (downloadEndTime - tickersStartTime) / 1_000_000_000 + " s");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.nanoTime();
        System.out.println("Total time to populate database: " + (endTime - startTime) / 1_000_000_000 + " s");
        analysisExecutor = Executors.newCachedThreadPool();
    }

    private static void ensureDataDirectoryExists() {
        File dataDirectory = new File("src/resources");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdir();
        }
    }

    private static void startHttpServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            ClassLoader classLoader = Main.class.getClassLoader();

            server.createContext("/", new FileHandler(new File(URLDecoder.decode(classLoader.getResource("index.html").getFile(), "UTF-8")).getPath(), "text/html"));
            server.createContext("/signals", new SignalsHandler());
            server.createContext("/tickers", new TickersHandler());
            server.createContext("/css.css", new FileHandler(new File(URLDecoder.decode(classLoader.getResource("css.css").getFile(), "UTF-8")).getPath(), "text/css"));
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("Server started on port 8080");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class FileHandler implements HttpHandler {
        private final String fileName;
        private final String contentType;

        public FileHandler(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Handling request for " + fileName);
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("File not found: " + fileName);
                String response = "404 (Not Found)\n";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            Headers h = exchange.getResponseHeaders();
            h.add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                Files.copy(file.toPath(), os);
            }
        }
    }

    static class SignalsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());

            String ticker = params.get("ticker");
            String period = params.get("period");

            if (ticker == null || period == null) {
                String response = "400 (Bad Request)\n";
                exchange.sendResponseHeaders(400, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            htmlResponse.setLength(0);
            htmlResponse.append("<html><head><link rel=\"stylesheet\" href=\"/css.css\"></head><body>");
            System.out.println("Handling /signals request for ticker: " + ticker + ", period: " + period);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("Processing ticker: " + ticker);
                htmlResponse.append(getAnalysisResults(ticker, period));
            }, analysisExecutor);

            future.thenRun(() -> {
                htmlResponse.append("</body></html>");
                System.out.println("Generated HTML response:\n" + htmlResponse.toString());
                try {
                    exchange.sendResponseHeaders(200, htmlResponse.toString().getBytes(StandardCharsets.UTF_8).length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(htmlResponse.toString().getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private String getAnalysisResults(String ticker, String period) {
            return Analysis.runAnalysis(ticker, period);
        }

        private Map<String, String> queryToMap(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }
    }


    static class TickersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonResponse = "";

            try {
                jsonResponse = objectMapper.writeValueAsString(tickers);
            } catch (Exception e) {
                e.printStackTrace();
                jsonResponse = "[]";
            }

            exchange.sendResponseHeaders(200, jsonResponse.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonResponse.getBytes());
            }
        }
    }
}

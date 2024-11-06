import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        ensureDataDirectoryExists();
        final String DB_URL = "jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db";
        long startTime = System.nanoTime();
        CutrePool connectionsPool = null;
        try {
            connectionsPool = new CutrePool(DB_URL);
            SQLiteDB.initializeDatabase(connectionsPool);
        }catch (SQLException e){
            e.printStackTrace();
        }

        if(connectionsPool != null) {
            try {
                // Step 1: Get tickers
                long tickersStartTime = System.nanoTime();
                try (Connection dbConnection = connectionsPool.getConnection()){
                    TickerScraper.processTickers(dbConnection, "https://www.mse.mk/en/issuers/free-market");
                    TickerScraper.processTickers(dbConnection, "https://www.mse.mk/en/issuers/JSC-with-special-reporting-obligations");
                }
                long tickersEndTime = System.nanoTime();
                System.out.println("Time to get tickers: " + (tickersEndTime - tickersStartTime) / 1_000_000 + " ms");

                // Step 2: Download historical data (multithreaded)
                Downloader.setConnectionPool(connectionsPool);
                List<String> tickers = Downloader.getAllTickers();
                int numThreads = Runtime.getRuntime().availableProcessors();
                ExecutorService executor = Executors.newFixedThreadPool(numThreads);

                long downloadStartTime = System.nanoTime();
                for (String ticker : tickers) {
                    CutrePool finalConnectionsPool = connectionsPool;
                    executor.submit(() -> {
                        final Connection[] connection = {null};
                        try{
                           connection[0] = finalConnectionsPool.getConnection();
                           Downloader.populateHistoricalData(ticker);
                        }catch (SQLException e){
                           e.printStackTrace();
                        }finally {
                           //zatvori konekcija na kraj
                           if(connection[0]!=null){
                               finalConnectionsPool.returnConnection(connection[0]);
                           }
                        }
                    });
                }

                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                long downloadEndTime = System.nanoTime();
                System.out.println("Time to download historical data: " + (downloadEndTime - downloadStartTime) / 1_000_000_000 + " s");
            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.nanoTime();
        System.out.println("Total time to populate database: " + (endTime - startTime) / 1_000_000_000 + " s");
    }

    private static void ensureDataDirectoryExists() {
        File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
            dataDirectory.mkdir();
        }
    }
}

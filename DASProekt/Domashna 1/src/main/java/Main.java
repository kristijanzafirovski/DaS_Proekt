import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        // Initialize the database and ensure data directory exists
        SQLiteDB.initializeDatabase();
        ensureDataDirectoryExists();

        long startTime = System.nanoTime();

        try (Connection dbConnection = SQLiteDB.getConnection()) {
            // Step 1: Get tickers
            long tickersStartTime = System.nanoTime();
            TickerScraper.processTickers(dbConnection, "https://www.mse.mk/en/issuers/free-market");
            TickerScraper.processTickers(dbConnection, "https://www.mse.mk/en/issuers/JSC-with-special-reporting-obligations");
            long tickersEndTime = System.nanoTime();
            System.out.println("Time to get tickers: " + (tickersEndTime - tickersStartTime) / 1_000_000 + " ms");

            // Step 2: Download historical data
            List<String> tickers = Downloader.getAllTickers(dbConnection);
            int numThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

            long downloadStartTime = System.nanoTime();
            for (String ticker : tickers) {
                executor.submit(() -> {
                    try {
                        Downloader.populateHistoricalData(ticker, dbConnection);
                    } catch (SQLException e) {
                        e.printStackTrace();
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

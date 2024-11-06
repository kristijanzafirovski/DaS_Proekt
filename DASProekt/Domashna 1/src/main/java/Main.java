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
        long startTime = System.nanoTime();

        CutrePool connectionsPool = null;
        try {
            connectionsPool = new CutrePool("jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db");
            SQLiteDB.initializeDatabase(connectionsPool);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (connectionsPool != null) {
            Downloader.setConnectionPool(connectionsPool);
            ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            try {
                // Step 1: Get tickers
                long tickersStartTime = System.nanoTime();
                try {
                    TickerScraper.processTickers(connectionsPool.getConnection(), "https://www.mse.mk/en/issuers/free-market");
                    TickerScraper.processTickers(connectionsPool.getConnection(), "https://www.mse.mk/en/issuers/JSC-with-special-reporting-obligations");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                long tickersEndTime = System.nanoTime();
                System.out.println("Time to get tickers: " + (tickersEndTime - tickersStartTime) / 1_000_000 + " ms");

                // Step 2: Download historical data (multithreaded)
                List<String> tickers = Downloader.getAllTickers();
                long downloadStartTime = System.nanoTime();
                for (String ticker : tickers) {
                    executor.submit(() -> {
                        try {
                            Downloader.populateHistoricalData(ticker);
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

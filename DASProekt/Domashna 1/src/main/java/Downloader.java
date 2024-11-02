import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.sqlite.SQLiteErrorCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class Downloader {
    private static final Object lock = new Object();
    public static List<String> getAllTickers(Connection connection) {
        List<String> tickers = new ArrayList<>();
        String query = "SELECT ticker FROM tickers";
        try (PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tickers.add(rs.getString("ticker"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickers;
    }
    public static void populateHistoricalData(String ticker, Connection connection) throws SQLException {
        LocalDate lastUpdateDate = lastUpdate(ticker, connection);
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);

        if (lastUpdateDate == null || !lastUpdateDate.equals(endDate)) {
            System.out.println("Populating historical data for ticker: " + ticker);
            LocalDate startDate = (lastUpdateDate != null) ? lastUpdateDate.plusDays(1) : endDate.minus(Period.ofYears(10));

            String url = "https://www.mse.mk/en/stats/symbolhistory/" + ticker;

            try {
                for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusYears(1)) {
                    LocalDate rangeEndDate = date.plusYears(1).isAfter(endDate) ? endDate : date.plusYears(1);

                    String fromDate = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).replace("/", "%2F");
                    String toDate = rangeEndDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")).replace("/", "%2F");


                    // Construct the request with the given dates and ticker
                    String formPayload = "FromDate=" + fromDate + "&ToDate=" + toDate + "&Code=" + ticker;
                    sendPostRequest(url, formPayload, ticker);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Ticker: " + ticker + " is up to date, skipping");
        }
    }
    private static void sendPostRequest(String urlString, String formPayload, String ticker) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        // Send form payload
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = formPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // Read the response
        try (InputStream inputStream = connection.getInputStream(); Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }
            processResponse(response.toString(), ticker);
        }
    }
    private static void processResponse(String response, String ticker) {
        // Parse the HTML response to extract the data
        Document doc = Jsoup.parse(response);
        Elements rows = doc.select("tr");

        // Iterate over each row in the table
        for (Element row : rows) {
            Elements columns = row.select("td");
            if (columns.size() < 9) continue; // Ensure there are enough columns

            // Extract data from each column
            String dateStr = columns.get(0).text();
            String lastTradePriceStr = columns.get(1).text();
            String maxPriceStr = columns.get(2).text();
            String minPriceStr = columns.get(3).text();
            String avgPriceStr = columns.get(4).text();
            String percentageChangeStr = columns.get(5).text();
            String volumeStr = columns.get(6).text();
            String turnoverBestStr = columns.get(7).text();
            String totalTurnoverStr = columns.get(8).text();

            // Convert to appropriate data types
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            double lastTradePrice = lastTradePriceStr.isEmpty() ? 0 : Double.parseDouble(lastTradePriceStr);
            double maxPrice = maxPriceStr.isEmpty() ? 0 : Double.parseDouble(maxPriceStr);
            double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
            double avgPrice = avgPriceStr.isEmpty() ? 0 : Double.parseDouble(avgPriceStr);
            double percentageChange = percentageChangeStr.isEmpty() ? 0 : Double.parseDouble(percentageChangeStr);
            int volume = volumeStr.isEmpty() ? 0 : Integer.parseInt(volumeStr);
            double turnoverBest = turnoverBestStr.isEmpty() ? 0 : Double.parseDouble(turnoverBestStr);
            double totalTurnover = totalTurnoverStr.isEmpty() ? 0 : Double.parseDouble(totalTurnoverStr);

            // Save the data to the database
            int retries = 0;
            while (true) {
                try (Connection connection = SQLiteDB.getConnection()) {
                    saveHistoricalData(connection, ticker, date, lastTradePrice, maxPrice, minPrice, avgPrice, percentageChange, volume, turnoverBest, totalTurnover);
                    break;
                } catch (SQLException e) {
                    if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code && retries < 5) {
                        retries++;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry", ex);
                        }
                    } else {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }


    private static void saveHistoricalData(Connection connection, String ticker, LocalDate date, double lastTradePrice, double maxPrice, double minPrice, double avgPrice, double percentageChange, int volume, double turnoverBest, double totalTurnover) throws SQLException {
        String query = "INSERT INTO historical_prices (ticker, date, last_trade_price, max_price, min_price, avg_price, percentage_change, volume, turnover_best, total_turnover) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ticker);
            stmt.setString(2, date.toString());
            stmt.setDouble(3, lastTradePrice);
            stmt.setDouble(4, maxPrice);
            stmt.setDouble(5, minPrice);
            stmt.setDouble(6, avgPrice);
            stmt.setDouble(7, percentageChange);
            stmt.setInt(8, volume);
            stmt.setDouble(9, turnoverBest);
            stmt.setDouble(10, totalTurnover);
            stmt.executeUpdate();
            System.out.println("Inserted historical data for: " + ticker + " on " + date);
        }
    }

    private static LocalDate lastUpdate(String ticker, Connection connection) throws SQLException {
        String query = "SELECT date FROM historical_prices WHERE ticker = ? ORDER BY date DESC LIMIT 1";
        LocalDate lastUpdateDate = null;
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dateStr = rs.getString("date");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    lastUpdateDate = LocalDate.parse(dateStr, formatter);
                }
            }
        }
        return lastUpdateDate;
    }

}


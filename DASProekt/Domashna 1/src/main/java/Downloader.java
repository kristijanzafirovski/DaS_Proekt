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
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Downloader {
    private static final long RETRY_DELAY_MS = 100;
    private static CutrePool connectionPool;

    public static void setConnectionPool(CutrePool pool) {
        connectionPool = pool;
    }

    public static List<String> getAllTickers() throws SQLException {
        List<String> tickers = new ArrayList<>();
        String query = "SELECT ticker FROM tickers";
        try (Connection connection = connectionPool.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                tickers.add(rs.getString("ticker"));
            }
        }
        return tickers;
    }

    public static void populateHistoricalData(String ticker) throws SQLException {
        LocalDate lastUpdateDate = null;
        try (Connection connection = connectionPool.getConnection()) {
            lastUpdateDate = lastUpdate(ticker, connection);
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.minusDays(1);

        // prezemi ako ne postoi ili dodadi
        if (lastUpdateDate == null || !lastUpdateDate.equals(endDate)) {
            System.out.println("Downloading data for: " + ticker);
            LocalDate startDate = (lastUpdateDate != null) ? lastUpdateDate.plusDays(1) : endDate.minus(Period.ofYears(10));

            String url = "https://www.mse.mk/en/stats/symbolhistory/" + ticker;

            try {

                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusYears(1)) {
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
            System.out.println("Ticker: " + ticker + " up to date");
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
            System.out.println("payload range sent: " + formPayload);
        }

        // Read the response
        try (InputStream inputStream = connection.getInputStream(); Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {
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
            String lastTradePriceStr = columns.get(1).text().replace(",","");
            String maxPriceStr = columns.get(2).text().replace(",","");
            String minPriceStr = columns.get(3).text().replace(",","");
            String avgPriceStr = columns.get(4).text().replace(",","");
            String percentageChangeStr = columns.get(5).text().replace(",","");
            String volumeStr = columns.get(6).text().replace(",","");
            String turnoverBestStr = columns.get(7).text().replace(",","");
            String totalTurnoverStr = columns.get(8).text().replace(",","");
            // Convert to data
            double lastTradePrice = lastTradePriceStr.isEmpty() ? 0 : Double.parseDouble(lastTradePriceStr);
            double maxPrice = maxPriceStr.isEmpty() ? 0 : Double.parseDouble(maxPriceStr);
            double minPrice = minPriceStr.isEmpty() ? 0 : Double.parseDouble(minPriceStr);
            double avgPrice = avgPriceStr.isEmpty() ? 0 : Double.parseDouble(avgPriceStr);
            double percentageChange = percentageChangeStr.isEmpty() ? 0 : Double.parseDouble(percentageChangeStr);
            int volume = volumeStr.isEmpty() ? 0 : Integer.parseInt(volumeStr);
            double turnoverBest = turnoverBestStr.isEmpty() ? 0 : Double.parseDouble(turnoverBestStr);
            double totalTurnover = totalTurnoverStr.isEmpty() ? 0 : Double.parseDouble(totalTurnoverStr);
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("M/d/yyyy"));
            // Save data
            saveHistoricalData(ticker, date, lastTradePrice, maxPrice, minPrice, avgPrice, percentageChange, volume, turnoverBest, totalTurnover);

        }
    }

    private static void saveHistoricalData(String ticker, LocalDate date, double lastTradePrice, double maxPrice, double minPrice, double avgPrice, double percentageChange, int volume, double turnoverBest, double totalTurnover) {
        boolean success = false;

        while (!success) {

            Connection connection = null;
            try {
                connection = connectionPool.getConnection();
                String query = "INSERT INTO historical_prices (ticker, date, last_trade_price, max_price, min_price, avg_price, percentage_change, volume, turnover_best, total_turnover) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, ticker);
                    stmt.setString(2, date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    stmt.setString(3, NumberFormat.getNumberInstance(Locale.US).format(lastTradePrice));
                    stmt.setString(4, NumberFormat.getNumberInstance(Locale.US).format(maxPrice));
                    stmt.setString(5, NumberFormat.getNumberInstance(Locale.US).format(minPrice));
                    stmt.setString(6, NumberFormat.getNumberInstance(Locale.US).format(avgPrice));
                    stmt.setString(7, NumberFormat.getNumberInstance(Locale.US).format(percentageChange));
                    stmt.setInt(8, volume);
                    stmt.setString(9, NumberFormat.getNumberInstance(Locale.US).format(turnoverBest));
                    stmt.setString(10, NumberFormat.getNumberInstance(Locale.US).format(totalTurnover));
                    stmt.executeUpdate();
                    System.out.println("Inserted data for: " + ticker + " on " + date);
                }
                success = true;
            } catch (SQLException e) {
                if (e.getErrorCode() == SQLiteErrorCode.SQLITE_BUSY.code) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ex);
                    }
                } else {
                    e.printStackTrace();
                    break;
                }
            }
        }

        if (!success) {
            System.err.println("Failed to insert historical data for ticker: " + ticker + " on date: " + date);
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

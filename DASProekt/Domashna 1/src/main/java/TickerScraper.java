import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TickerScraper {
    public static void processTickers(Connection dbConnection, String url) {
        int newTickers = 0;
        try {
            Document site = fetchSiteDocument(url);
            Elements content = site.select("tr");
            for (Element row : content) {
                Elements columns = row.select("td");
                if (columns.size() < 2) continue; // Skip rows with insufficient columns
                String ticker = columns.get(0).text();
                String fullName = columns.get(1).text();
                if (!doesTickerExist(dbConnection, ticker)) {
                    insertTicker(dbConnection, ticker, fullName);
                } else {
                    newTickers++;
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
        if(newTickers != 0) System.out.println("Added " + newTickers + " new tickers");
        else System.out.println("All tickers up to date");
    }

    private static Document fetchSiteDocument(String link) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        connection.setFixedLengthStreamingMode(0);

        try (InputStream inputStream = connection.getInputStream()) {
            return Jsoup.parse(inputStream, "UTF-8", link);
        }
    }

    private static boolean doesTickerExist(Connection con, String ticker) throws SQLException {
        String command = "SELECT COUNT(*) FROM tickers WHERE ticker = ?";
        try (PreparedStatement stmt = con.prepareStatement(command)) {
            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }
        }
    }

    private static void insertTicker(Connection connection, String ticker, String fullName) throws SQLException {
        String query = "INSERT INTO tickers (ticker, full_name) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ticker);
            stmt.setString(2, fullName);
            stmt.executeUpdate();
            System.out.println("Inserted: " + ticker + " - " + fullName);
        }
    }
}

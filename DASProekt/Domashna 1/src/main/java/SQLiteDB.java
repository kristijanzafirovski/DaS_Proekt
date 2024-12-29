import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDB {
    private static final String DB_URL = "jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db";
    private static CutrePool connectionPool;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void initializeDatabase(CutrePool pool) {
        connectionPool = pool;
        System.out.println(DB_URL);
        try (Connection conn = connectionPool.getConnection()) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                stmt.execute("PRAGMA journal_mode=WAL;");
                String sql = "CREATE TABLE IF NOT EXISTS tickers (" +
                        "ticker TEXT PRIMARY KEY," +
                        "full_name TEXT NOT NULL" +
                        ")";
                stmt.execute(sql);
                String createHistoricalPricesTable = "CREATE TABLE IF NOT EXISTS historical_prices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "ticker TEXT NOT NULL," +
                        "date DATE NOT NULL,"+
                        "last_trade_price REAL,"+
                        "max_price REAL,"+
                        "min_price REAL,"+
                        "avg_price REAL,"+
                        "percentage_change REAL,"+
                        "volume INTEGER,"+
                        "turnover_best REAL,"+
                        "total_turnover REAL,"+
                        "FOREIGN KEY (ticker) REFERENCES tickers(ticker)" + ")";
                stmt.execute(createHistoricalPricesTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connectionPool == null) {
            throw new SQLException("Connection pool is not initialized.");
        }
        return connectionPool.getConnection();
    }

    public static void returnConnection(Connection connection) {
        connectionPool.returnConnection(connection);
    }

    public static List<Tick> getHistoricalPrices(String ticker) {
        List<Tick> historicalPrices = new ArrayList<>();
        String query = "SELECT date, last_trade_price, max_price, min_price, avg_price, volume FROM historical_prices WHERE ticker = ? ORDER BY date";
        try (Connection connection = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, ticker);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = LocalDate.parse(rs.getString("date"), DATE_FORMATTER);
                    double lastTradePrice = rs.getDouble("last_trade_price");
                    double maxPrice = rs.getDouble("max_price");
                    double minPrice = rs.getDouble("min_price");
                    double avgPrice = rs.getDouble("avg_price");
                    int volume = rs.getInt("volume");
                    historicalPrices.add(new Tick(date, lastTradePrice, maxPrice, minPrice, avgPrice, volume));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return historicalPrices;
    }
}

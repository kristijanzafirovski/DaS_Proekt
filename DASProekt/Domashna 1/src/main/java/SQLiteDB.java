import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDB {
    private static final String DB_URL = "jdbc:sqlite:D:\\Finki\\DASProekt\\Domashna 1\\data\\ticker-db";
    private static CutrePool connectionPool;

    public static void initializeDatabase(CutrePool pool) {
        connectionPool = pool;
        System.out.println(DB_URL);
        try (Connection conn = connectionPool.getConnection()) {
            if (conn != null) {
                // Enable WAL mode
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
}

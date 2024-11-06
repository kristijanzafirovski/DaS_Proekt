import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

public class CutrePool {
    String connString;

    static final int INITIAL_CAPACITY = 50;
    LinkedList<Connection> pool = new LinkedList<>();

    public CutrePool(String connString) throws SQLException {
        this.connString = connString;
        for (int i = 0; i < INITIAL_CAPACITY; i++) {
            pool.add(DriverManager.getConnection(connString));
        }
    }

    public synchronized Connection getConnection() throws SQLException {
        if (pool.isEmpty()) {
            pool.add(DriverManager.getConnection(connString));
        }
        return pool.pop();
    }

    public synchronized void returnConnection(Connection connection) {
        pool.push(connection);
    }
}

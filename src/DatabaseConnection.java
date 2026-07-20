import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/atm_db"; // Replace 'atm_db' with your database name
    private static final String USER = "root"; // Replace 'root' with your MySQL username
    private static final String PASSWORD = "root"; // Replace '' with your MySQL password

    public static Connection connect() {
        try {
            // Load the MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish the connection
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
            return connection;
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found. Include it in your library path.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database. Check your credentials and URL.");
            e.printStackTrace();
        }
        return null;
    }
}

import java.sql.*;
import java.util.Random;
import java.util.Scanner;

public class ATM {
    public static void main(String[] args) {
        Connection conn = DatabaseConnection.connect();
        if (conn == null) {
            System.out.println("Failed to connect to the database. Exiting...");
            return;
        }

        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n=== ATM Menu ===");
            System.out.println("1. Withdraw (Card)");
            System.out.println("2. Initiate Cardless Withdrawal");
            System.out.println("3. Complete Cardless Withdrawal");
            System.out.println("4. View Balance");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();

            switch (choice) {
                case 1 -> withdraw(scanner, conn);
                case 2 -> initiateCardless(scanner, conn);
                case 3 -> completeCardless(scanner, conn);
                case 4 -> viewBalance(scanner, conn);
                case 5 -> System.out.println("Thank you for using the ATM. Goodbye!");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 5);

        scanner.close();
    }

    private static void withdraw(Scanner scanner, Connection conn) {
        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();
        System.out.print("Enter PIN: ");
        int pin = scanner.nextInt();

        if (authenticateUser(userId, pin, conn)) {
            System.out.print("Enter amount to withdraw: ");
            double amount = scanner.nextDouble();
            System.out.print("Enter account type (Saving/Current): ");
            String accountType = scanner.next();

            try {
                String query = "SELECT balance FROM Users WHERE user_id = ? AND account_type = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, userId);
                pstmt.setString(2, accountType);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    if (balance >= amount) {
                        String updateQuery = "UPDATE Users SET balance = balance - ? WHERE user_id = ? AND account_type = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setDouble(1, amount);
                        updateStmt.setInt(2, userId);
                        updateStmt.setString(3, accountType);
                        updateStmt.executeUpdate();

                        System.out.println("Withdrawal successful.");
                    } else {
                        System.out.println("Insufficient balance.");
                    }
                } else {
                    System.out.println("Account type mismatch or account does not exist.");
                }
            } catch (Exception e) {
                System.out.println("Error during withdrawal: " + e.getMessage());
            }
        } else {
            System.out.println("Authentication failed.");
        }
    }

    private static void initiateCardless(Scanner scanner, Connection conn) {
        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();
        System.out.print("Enter PIN: ");
        int pin = scanner.nextInt();

        if (authenticateUser(userId, pin, conn)) {
            System.out.print("Enter amount for cardless withdrawal: ");
            double amount = scanner.nextDouble();
            System.out.print("Enter account type (Saving/Current): ");
            String accountType = scanner.next();

            try {
                String checkQuery = "SELECT balance FROM Users WHERE user_id = ? AND account_type = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setInt(1, userId);
                checkStmt.setString(2, accountType);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getDouble("balance") >= amount) {
                    int otp = new Random().nextInt(900000) + 100000;

                    String insertQuery = "INSERT INTO Cardless (user_id, otp, amount, account_type, otp_expiration) VALUES (?, ?, ?, ?, NOW() + INTERVAL 10 MINUTE)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, otp);
                    insertStmt.setDouble(3, amount);
                    insertStmt.setString(4, accountType);
                    insertStmt.executeUpdate();

                    System.out.println("Cardless withdrawal initiated. OTP: " + otp);
                } else {
                    System.out.println("Insufficient balance.");
                }
            } catch (SQLException e) {
                System.out.println("Error during cardless initiation: " + e.getMessage());
            }
        } else {
            System.out.println("Authentication failed.");
        }
    }

    private static void completeCardless(Scanner scanner, Connection conn) {
        System.out.print("Enter OTP: ");
        int otp = scanner.nextInt();

        try {
            String checkQuery = "SELECT user_id, amount, account_type FROM Cardless WHERE otp = ? AND status = 'Pending' AND otp_expiration > NOW()";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, otp);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                double amount = rs.getDouble("amount");
                String accountType = rs.getString("account_type");

                String updateUserQuery = "UPDATE Users SET balance = balance - ? WHERE user_id = ? AND account_type = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserQuery);
                updateUserStmt.setDouble(1, amount);
                updateUserStmt.setInt(2, userId);
                updateUserStmt.setString(3, accountType);
                updateUserStmt.executeUpdate();

                String updateCardlessQuery = "UPDATE Cardless SET status = 'Completed' WHERE otp = ?";
                PreparedStatement updateCardlessStmt = conn.prepareStatement(updateCardlessQuery);
                updateCardlessStmt.setInt(1, otp);
                updateCardlessStmt.executeUpdate();

                System.out.println("Cardless withdrawal completed successfully!");
            } else {
                System.out.println("Invalid or expired OTP.");
            }
        } catch (SQLException e) {
            System.out.println("Error during cardless completion: " + e.getMessage());
        }
    }

    private static void viewBalance(Scanner scanner, Connection conn) {
        System.out.print("Enter User ID: ");
        int userId = scanner.nextInt();
        System.out.print("Enter PIN: ");
        int pin = scanner.nextInt();

        if (authenticateUser(userId, pin, conn)) {
            try {
                String query = "SELECT balance, account_type FROM Users WHERE user_id = ?";
                PreparedStatement pstmt = conn.prepareStatement(query);
                pstmt.setInt(1, userId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    System.out.println("Account Type: " + rs.getString("account_type"));
                    System.out.println("Balance: " + rs.getDouble("balance"));
                }
            } catch (SQLException e) {
                System.out.println("Error fetching balance: " + e.getMessage());
            }
        } else {
            System.out.println("Authentication failed.");
        }
    }

    private static boolean authenticateUser(int userId, int pin, Connection conn) {
        try {
            String query = "SELECT * FROM Users WHERE user_id = ? AND pin = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, pin);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println("Authentication error: " + e.getMessage());
            return false;
        }
    }
}

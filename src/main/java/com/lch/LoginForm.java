package com.lch;

import com.lch.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.sql.*;

public class LoginForm extends JFrame {

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false&createDatabaseIfNotExist=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginForm() {
        initializeUI();
    }

    private void initializeUI() {
        createUsersTableIfNotExists();
        addAdmin();
        createLoginHisIfNotExists();
        setTitle("User Login");
        setSize(300, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new GridLayout(3, 2));

        mainPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        mainPanel.add(usernameField);

        mainPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        mainPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> loginUser());

        mainPanel.add(loginButton);

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private void createLoginHisIfNotExists() {
        try (Connection connection = establishConnection()) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS login_history ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "user_id INT NOT NULL,"
                    + "login_time TIMESTAMP NOT NULL,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id))";

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addAdmin() {
        if (!isAdminAlreadyExists()) {
            try (Connection connection = establishConnection()) {
                String query = "INSERT INTO users (name, age, phone_number, status, password, role) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, "admin");
                    statement.setInt(2, 20);
                    statement.setString(3, "0372290831");
                    statement.setString(4, "Normal");
                    statement.setString(5, "123456789");
                    statement.setString(6, "admin");
                    statement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isAdminAlreadyExists() {
        try (Connection connection = establishConnection()) {
            String query = "SELECT COUNT(*) FROM users WHERE name = 'admin'";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void createUsersTableIfNotExists() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255) NOT NULL,"
                    + "age INT NOT NULL,"
                    + "phone_number VARCHAR(15) NOT NULL,"
                    + "status VARCHAR(10) NOT NULL,"
                    + "password VARCHAR(255) NOT NULL,"
                    + "role VARCHAR(255) NOT NULL,"
                    + "profile_picture LONGBLOB)";

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableQuery);
            }

            // Check if the admin user exists and has a profile picture
            if (!isAdminAlreadyExists()) {
                addAdminWithProfilePicture();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addAdminWithProfilePicture() {
        try (Connection connection = establishConnection()) {
            String query = "INSERT INTO users (name, age, phone_number, status, password, role, profile_picture) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, "admin");
                statement.setInt(2, 20);
                statement.setString(3, "0372290831");
                statement.setString(4, "Normal");
                statement.setString(5, "123456789");
                statement.setString(6, "admin");

                // Set the profile picture as a BLOB
                InputStream profilePictureStream = getClass().getClassLoader().getResourceAsStream("pic/default.png");
                if (profilePictureStream != null) {
                    statement.setBlob(7, profilePictureStream);
                } else {
                    statement.setNull(7, Types.BLOB);
                }

                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loginUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        User user = getUserFromDatabase(username, password);

        if (user != null) {
            String role = user.getRole();
            String status = user.getStatus();
            addLoginHistory(user.getId());
            if ("Normal".equals(status)){
                openUserAccountManagement(role, user);
            } else {
                JOptionPane.showMessageDialog(this, "Your account is locked, please contact with admin for more information");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.");
        }
    }

    private void addLoginHistory(int userId) {
        try (Connection connection = establishConnection()) {
            String query = "INSERT INTO login_history (user_id, login_time) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                // Set login_time to current timestamp
                statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                statement.setInt(1, userId);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openUserAccountManagement(String role, User user) {
        SwingUtilities.invokeLater(() -> {
            UserAccountManagement userAccountManagement = new UserAccountManagement(role, user);
            userAccountManagement.setVisible(true);
        });
        dispose();
    }

    private User getUserFromDatabase(String username, String password) {
        try (Connection connection = establishConnection()) {
            String query = "SELECT * FROM users WHERE name = ? AND password = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                statement.setString(2, password);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        String name = resultSet.getString("name");
                        int age = resultSet.getInt("age");
                        String phoneNumber = resultSet.getString("phone_number");
                        String status = resultSet.getString("status");
                        String role = resultSet.getString("role");
                        byte[] profilepic = resultSet.getBytes("profile_picture");
                        return new User(id, name, age, phoneNumber, status, password, role,profilepic);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Connection establishConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

}

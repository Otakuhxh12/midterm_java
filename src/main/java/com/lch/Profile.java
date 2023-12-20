package com.lch;

import com.lch.model.LoginHistoryEntry;
import com.lch.model.User;

import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class Profile extends JPanel {

    private User loggedInUser;
    private JLabel profilePictureLabel;

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false&createDatabaseIfNotExist=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";


    public Profile(User user) {
        this.loggedInUser = user;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // Create a main panel to hold the components
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Display user information
        JPanel userInfoPanel = new JPanel(new GridLayout(0, 1, 10, 10)); // Single column layout
        userInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("User Information"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        addUserInfoLabels(userInfoPanel);

        // Display profile picture on the left
        byte[] profilePictureData = loggedInUser.getProfilePicture();
        if (profilePictureData != null && profilePictureData.length > 0) {
            try {
                ImageIcon profilePictureIcon = new ImageIcon(profilePictureData);
                Image scaledImage = profilePictureIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);
                profilePictureLabel = new JLabel(scaledIcon);

                // Add some space around the picture
                JPanel picturePanel = new JPanel(new BorderLayout());
                picturePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                picturePanel.add(profilePictureLabel, BorderLayout.CENTER);

                mainPanel.add(picturePanel, BorderLayout.WEST);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        mainPanel.add(Box.createRigidArea(new Dimension(20, 0)), BorderLayout.CENTER);

        mainPanel.add(userInfoPanel, BorderLayout.CENTER);

        JButton changePictureButton = new JButton("Change Picture");
        changePictureButton.addActionListener(e -> handleChangePicture());

        JButton loginHistoryButton = new JButton("Login History");
        loginHistoryButton.addActionListener(e -> showLoginHistory());

        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(changePictureButton);
        buttonsPanel.add(loginHistoryButton);

        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);


        JLabel label = new JLabel("Profile", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 25));

        add(label, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    private void showLoginHistory() {
        List<LoginHistoryEntry> loginHistory = getLoginHistory(loggedInUser.getId());

        if (!loginHistory.isEmpty()) {
            // Display the login history, you can use JOptionPane or create a custom dialog
            StringBuilder historyText = new StringBuilder("Login History:\n");
            for (LoginHistoryEntry entry : loginHistory) {
                historyText.append(entry.getLoginTime()).append("\n");
            }

            JOptionPane.showMessageDialog(this, historyText.toString(), "Login History", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No login history available.", "Login History", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private List getLoginHistory(int userId) {
        List<LoginHistoryEntry> loginHistory = new ArrayList<>();
        try (Connection connection = establishConnection()) {
            String query = "SELECT * FROM login_history WHERE user_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        Timestamp loginTime = resultSet.getTimestamp("login_time");
                        loginHistory.add(new LoginHistoryEntry(loginTime));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return loginHistory;

    }

    private void handleChangePicture() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] newProfilePictureData = readFileToByteArray(selectedFile);
                // Update the user's profile picture in the database
                // For demonstration purposes, assume you have a method like updateUserProfilePicture(byte[] newProfilePictureData) in your database API
                // updateUserProfilePicture(loggedInUser.getId(), newProfilePictureData);

                // Update the displayed profile picture
                updateProfilePicture(newProfilePictureData);
            } catch (IOException e) {
                e.printStackTrace();
                // Handle IO exception
            }
        }
    }

    private void updateProfilePicture(byte[] newProfilePictureData) {
        if (newProfilePictureData != null && newProfilePictureData.length > 0) {
            try {
                updateUserProfilePicture(loggedInUser.getId(), newProfilePictureData);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to update profile picture in the database.");
                return;
            }

            // Update the displayed profile picture
            ImageIcon newProfilePictureIcon = new ImageIcon(newProfilePictureData);
            Image scaledImage = newProfilePictureIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaledImage);
            profilePictureLabel.setIcon(scaledIcon);
            JOptionPane.showMessageDialog(this, "Profile picture updated successfully.");
        }
    }

    private void updateUserProfilePicture(int userId, byte[] newProfilePictureData) throws SQLException {
        try (Connection connection = establishConnection()) {
            String query = "UPDATE users SET profile_picture = ? WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setBytes(1, newProfilePictureData);
                statement.setInt(2, userId);
                statement.executeUpdate();
            }
        }
    }

    private byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }

    private void addUserInfoLabels(JPanel panel) {
        addLabelToPanel("Name:", loggedInUser.getName(), panel);
        addLabelToPanel("Age:", String.valueOf(loggedInUser.getAge()), panel);
        addLabelToPanel("Phone Number:", loggedInUser.getPhoneNumber(), panel);
        addLabelToPanel("Status:", loggedInUser.getStatus(), panel);
        addLabelToPanel("Role:", loggedInUser.getRole(), panel);
    }

    private void addLabelToPanel(String labelText, String value, JPanel panel) {
        JLabel label = new JLabel(labelText, SwingConstants.LEFT);
        label.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueLabel = new JLabel(value, SwingConstants.LEFT);
        valueLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel labelPanel = new JPanel(new BorderLayout());
        labelPanel.add(label, BorderLayout.WEST);
        labelPanel.add(valueLabel, BorderLayout.CENTER);

        panel.add(labelPanel);
    }

    public static Connection establishConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }
}

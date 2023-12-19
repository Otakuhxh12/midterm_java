package com.lch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginForm() {
        initializeUI();
    }

    private void initializeUI() {
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
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginUser();
            }
        });

        mainPanel.add(loginButton);

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private void loginUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        // TODO: Validate credentials against the database
        if (validateUser(username, password)) {
            SwingUtilities.invokeLater(() -> {
                UserAccountManagement userAccountManagement = new UserAccountManagement();
                userAccountManagement.setVisible(true);
            });
            dispose(); // Close the login window
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.");
        }
    }

    private boolean validateUser(String username, String password) {
        // TODO: Implement database validation logic
        // For demonstration purposes, let's assume a user with username "admin" and password "admin" is valid
        return "admin".equals(username) && "admin".equals(password);
    }


}

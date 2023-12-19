package com.lch;

import com.lch.UserAccountManagement;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

        JPanel panel = new JPanel();
        placeComponents(panel);
        add(panel);

        setLocationRelativeTo(null); // Center the frame
    }

    private void placeComponents(JPanel panel) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel userLabel = new JLabel("Username:");
        usernameField = new JTextField(20);

        JLabel passwordLabel = new JLabel("Password:");
        passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                char[] passwordChars = passwordField.getPassword();
                String password = new String(passwordChars);

                if (authenticateUser(username, password)) {
                    JOptionPane.showMessageDialog(null, "Login Successful!");
                    openUserAccountManagement();
                    dispose(); // Close the login form after successful login
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid username or password. Please try again.");
                }

                // Clear fields after login attempt
                usernameField.setText("");
                passwordField.setText("");
            }
        });

        panel.add(userLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(loginButton);
    }

    private boolean authenticateUser(String username, String password) {
        // For demonstration purposes, use hardcoded credentials
        return username.equals("admin") && password.equals("123456" +
                "");
        // In a real application, you would validate credentials against your database
    }

    private void openUserAccountManagement() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UserAccountManagement().setVisible(true);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginForm().setVisible(true);
            }
        });
    }
}

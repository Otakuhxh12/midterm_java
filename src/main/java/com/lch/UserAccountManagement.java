// UserAccountManagement class

package com.lch;

import com.lch.model.User;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserAccountManagement extends JFrame {

    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton deleteUserButton;
    private JButton editUserButton;

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false&createDatabaseIfNotExist=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    public UserAccountManagement() {
        initializeUI();
        createUsersTableIfNotExists();
        displayUserList();
    }

    private void createUsersTableIfNotExists() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255) NOT NULL,"
                    + "age INT NOT NULL,"
                    + "phone_number VARCHAR(15) NOT NULL,"
                    + "status VARCHAR(10) NOT NULL)";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        setTitle("User Account Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // User Table
        String[] columnNames = {"User Id", "Username", "Phone Number", "Age", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Cells are not editable by default
                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Allow only single-row selection

        // Add a ListSelectionListener to detect when a row is selected
        userTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRowIndex = userTable.getSelectedRow();
                editUserButton.setEnabled(selectedRowIndex != -1); // Enable "Edit User" button when a row is selected
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(userTable);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addUserButton = new JButton("Add User");
        editUserButton = new JButton("Edit User"); // Make it a class variable
        deleteUserButton = new JButton("Delete User");
        JButton viewUserInfoButton = new JButton("View User Info");
        JButton manageStudentsButton = new JButton("Manage Students");

        manageStudentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openStudentManagement();
            }
        });
        buttonPanel.add(manageStudentsButton);

        // Disable the "Edit User" button initially
        editUserButton.setEnabled(false);

        // Add ActionListener to "Edit User" button
        editUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedUser();
            }
        });

        addUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show a dialog with input fields for user information
                User newUser = showUserInputDialog();
                if (newUser != null) {
                    addUserToDatabase(newUser);

                }
            }
        });

        deleteUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedUser();
            }
        });

        viewUserInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRowIndex = userTable.getSelectedRow();
                if (selectedRowIndex != -1) {
                    User selectedUser = getUserFromTable(selectedRowIndex);

                    // Create a panel with non-editable fields for viewing
                    JPanel viewPanel = new JPanel(new GridLayout(5, 2));
                    JTextField nameField = createNonEditableTextField(selectedUser.getName());
                    JTextField ageField = createNonEditableTextField(String.valueOf(selectedUser.getAge()));
                    JTextField phoneNumberField = createNonEditableTextField(selectedUser.getPhoneNumber());
                    JTextField statusField = createNonEditableTextField(selectedUser.getStatus());

                    viewPanel.add(new JLabel("Name:"));
                    viewPanel.add(nameField);
                    viewPanel.add(new JLabel("Age:"));
                    viewPanel.add(ageField);
                    viewPanel.add(new JLabel("Phone Number:"));
                    viewPanel.add(phoneNumberField);
                    viewPanel.add(new JLabel("Status:"));
                    viewPanel.add(statusField);

                    JOptionPane.showConfirmDialog(null, viewPanel, "View User Information", JOptionPane.DEFAULT_OPTION);
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to view.");
                }
            }
        });

        buttonPanel.add(addUserButton);
        buttonPanel.add(viewUserInfoButton);
        buttonPanel.add(editUserButton);
        buttonPanel.add(deleteUserButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null); // Center the frame
    }

    private Connection establishConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
    }

    private void openStudentManagement() {
        StudentManagement studentManagement = new StudentManagement();
        studentManagement.setVisible(true);
    }

    private void addUserToDatabase(User user) {
        try (Connection connection = establishConnection()) {
            String query = "INSERT INTO users (name, age, phone_number, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getName());
                statement.setInt(2, user.getAge());
                statement.setString(3, user.getPhoneNumber());
                statement.setString(4, user.getStatus());
                statement.executeUpdate();

                // Retrieve the generated ID and set it in the User object
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Failed to retrieve generated ID.");
                    }
                }
            }
            displayUserList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<User> retrieveUserListFromDatabase() {
        List<User> userList = new ArrayList<>();
        try (Connection connection = establishConnection()) {
            String query = "SELECT * FROM users";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    int age = resultSet.getInt("age");
                    String phoneNumber = resultSet.getString("phone_number");
                    String status = resultSet.getString("status");
                    userList.add(new User(id, name, age, phoneNumber, status));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    private void displayUserList() {
        List<User> userListData = retrieveUserListFromDatabase();
        for (User user : userListData) {
            addUserToTable(user);
        }
    }

    private void deleteSelectedUserFromDatabase(User user) {
        try (Connection connection = establishConnection()) {
            String query = "DELETE FROM users WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, user.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedUser() {
        int selectedRowIndex = userTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            User selectedUser = getUserFromTable(selectedRowIndex);
            deleteSelectedUserFromDatabase(selectedUser);
            tableModel.removeRow(selectedRowIndex);
        } else {
            JOptionPane.showMessageDialog(this, "Select a user to delete.");
        }
    }

    private void editSelectedUserInDatabase(User user) {
        try (Connection connection = establishConnection()) {
            String queryUpdate = "UPDATE users SET name = ?, age = ?, phone_number = ?, status = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {
                updateStatement.setString(1, user.getName());
                updateStatement.setInt(2, user.getAge());
                updateStatement.setString(3, user.getPhoneNumber());
                updateStatement.setString(4, user.getStatus());
                updateStatement.setInt(5, user.getId());

                int rowsUpdated = updateStatement.executeUpdate();
                System.out.println("Rows Updated in Database: " + rowsUpdated);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void editSelectedUser() {
        int selectedRowIndex = userTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            User selectedUser = getUserFromTable(selectedRowIndex);
            User editedUser = showEditUserInputDialog(selectedUser);
            if (editedUser != null) {
                updateUserInTable(selectedRowIndex, editedUser);
                editSelectedUserInDatabase(editedUser);
            }
        } else {
            JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to edit.");
        }
    }

    private void updateUserInTable(int rowIndex, User user) {
        tableModel.setValueAt(user.getName(), rowIndex, 1);
        tableModel.setValueAt(user.getPhoneNumber(), rowIndex, 2);
        tableModel.setValueAt(user.getAge(), rowIndex, 3);
        tableModel.setValueAt(user.getStatus(), rowIndex, 4);
    }

    private User showEditUserInputDialog(User user) {
        JTextField nameField = new JTextField(user.getName());
        JTextField ageField = new JTextField(String.valueOf(user.getAge()));
        JTextField phoneNumberField = new JTextField(user.getPhoneNumber());

        // Combo box for status selection
        String[] statusOptions = {"Normal", "Locked"};
        JComboBox<String> statusComboBox = new JComboBox<>(statusOptions);
        statusComboBox.setSelectedItem(user.getStatus());

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneNumberField);
        panel.add(new JLabel("Status:"));
        panel.add(statusComboBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit User Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int age = Integer.parseInt(ageField.getText());
                // Validate phone number format
                if (phoneNumberField.getText().matches("0\\d{9}")) {
                    return new User(user.getId(), nameField.getText(), age, phoneNumberField.getText(), (String) statusComboBox.getSelectedItem());
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Phone number must begin with 0 and be followed by 9 digits.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(UserAccountManagement.this, "Invalid input for age.");
            }
        }
        return null;
    }


    private JTextField createNonEditableTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setEditable(false);
        return textField;
    }

    private User showUserInputDialog() {
        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();
        JTextField phoneNumberField = new JTextField();

        // Combo box for status selection
        String[] statusOptions = {"Normal", "Locked"};
        JComboBox<String> statusComboBox = new JComboBox<>(statusOptions);

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneNumberField);
        panel.add(new JLabel("Status:"));
        panel.add(statusComboBox);

        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Add New User", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText();
                String ageText = ageField.getText();
                String phoneNumber = phoneNumberField.getText();
                String status = (String) statusComboBox.getSelectedItem();

                // Validate that none of the fields are empty
                if (!name.isEmpty() && !ageText.isEmpty() && !phoneNumber.isEmpty()) {
                    try {
                        int age = Integer.parseInt(ageText);

                        // Validate phone number format
                        if (phoneNumber.matches("0\\d{9}")) {
                            return new User(0, name, age, phoneNumber, status);
                        } else {
                            JOptionPane.showMessageDialog(UserAccountManagement.this, "Phone number must begin with 0 and be followed by 9 digits.");
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(UserAccountManagement.this, "Invalid input for age.");
                    }
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Please fill in all user information.");
                }
            } else {
                // User canceled the dialog
                return null;
            }
        }
    }

    private void addUserToTable(User newUser) {
        boolean userExists = false;

        // Check if the user already exists in the table
        for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
            int existingUserId = (int) tableModel.getValueAt(rowIndex, 0);
            if (existingUserId == newUser.getId()) {
                // Update existing row
                updateUserInTable(rowIndex, newUser);
                userExists = true;
                break;
            }
        }

        // If the user doesn't exist, add a new row
        if (!userExists) {
            Object[] rowData = {newUser.getId(), newUser.getName(), newUser.getPhoneNumber(), newUser.getAge(), newUser.getStatus()};
            tableModel.addRow(rowData);
        }
    }

    private User getUserFromTable(int rowIndex) {
        int id = (int) userTable.getValueAt(rowIndex, 0);
        String name = (String) userTable.getValueAt(rowIndex, 1);
        String phoneNumber = (String) userTable.getValueAt(rowIndex, 2);
        int age = (int) userTable.getValueAt(rowIndex, 3);
        String status = (String) userTable.getValueAt(rowIndex, 4);

        return new User(id, name, age, phoneNumber, status);
    }

}

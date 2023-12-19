package com.lch;


import com.lch.model.User;
import javax.swing.JOptionPane;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class UserAccountManagement extends JFrame {

    private JTable userTable;
    private DefaultTableModel tableModel;

    public UserAccountManagement() {
        initializeUI();
        displayUserList();
    }

    private void initializeUI() {
        setTitle("User Account Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // User Table
        String[] columnNames = {"Username", "Phone Number", "Age", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0);
        userTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(userTable);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addUserButton = new JButton("Add User");
        JButton editUserButton = new JButton("Edit User");
        JButton deleteUserButton = new JButton("Delete User");
        JButton viewUserInfoButton = new JButton("View User Info");

        addUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show a dialog with input fields for user information
                User newUser = showUserInputDialog();
                if (newUser != null) {
                    addUserToTable(newUser);
                }
            }
        });

        deleteUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedUser();
            }
        });

        editUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedUser();
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

    private void editSelectedUser() {
        int selectedRowIndex = userTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            User selectedUser = getUserFromTable(selectedRowIndex);
            User editedUser = showEditUserInputDialog(selectedUser);

            if (editedUser != null) {
                // Update the data in the existing row
                userTable.setValueAt(editedUser.getName(), selectedRowIndex, 0);
                userTable.setValueAt(editedUser.getPhoneNumber(), selectedRowIndex, 1);
                userTable.setValueAt(editedUser.getAge(), selectedRowIndex, 2);
                userTable.setValueAt(editedUser.getStatus(), selectedRowIndex, 3);
            }
        } else {
            JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to edit.");
        }
    }

    private User showEditUserInputDialog(User user) {
        JTextField nameField = new JTextField(user.getName());
        JTextField ageField = new JTextField(String.valueOf(user.getAge()));
        JTextField phoneNumberField = new JTextField(user.getPhoneNumber());
        JTextField statusField = new JTextField(user.getStatus());

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneNumberField);
        panel.add(new JLabel("Status:"));
        panel.add(statusField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit User Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int age = Integer.parseInt(ageField.getText());

                // Validate phone number format
                if (phoneNumberField.getText().matches("0\\d{9}")) {
                    return new User(nameField.getText(), age, phoneNumberField.getText(), statusField.getText());
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Phone number must begin with 0 and be followed by 9 digits.");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(UserAccountManagement.this, "Invalid input for age.");
            }
        }
        return null;
    }

    private void displayUserList() {
        // Example: Fetch user list from the database (replace this with your logic)
        List<User> userListData = retrieveUserList();

        // Example: Display user list in the table
        for (User user : userListData) {
            addUserToTable(user);
        }
    }

    private List<User> retrieveUserList() {
        // Example: Replace this with your logic to fetch the user list from the database
        // For demonstration, return a hardcoded list
        List<User> userList = new ArrayList<>();
        userList.add(new User("User1", 25, "0123456789", "Normal"));
        userList.add(new User("User2", 30, "0987654321", "Locked"));
        userList.add(new User("User3", 22, "0555555555", "Normal"));
        return userList;
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
                            return new User(name, age, phoneNumber, status);
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

    private void addUserToTable(User user) {
        Object[] rowData = {user.getName(), user.getPhoneNumber(), user.getAge(), user.getStatus()};
        tableModel.addRow(rowData);
    }

    private User getUserFromTable(int rowIndex) {
        String name = (String) userTable.getValueAt(rowIndex, 0);
        String phoneNumber = (String) userTable.getValueAt(rowIndex, 1);
        int age = (int) userTable.getValueAt(rowIndex, 2);
        String status = (String) userTable.getValueAt(rowIndex, 3);

        return new User(name, age, phoneNumber, status);
    }

    private void deleteSelectedUser() {
        int selectedRowIndex = userTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            tableModel.removeRow(selectedRowIndex);
        } else {
            JOptionPane.showMessageDialog(this, "Select a user to delete.");
        }
    }

    private JTextField createNonEditableTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setEditable(false);
        return textField;
    }
}

package com.lch;

import com.lch.model.LoginHistoryEntry;
import com.lch.model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserAccountManagement extends JFrame {

    private JTabbedPane tabbedPane;
    private JTable userTable;
    private JButton logoutButton;
    private DefaultTableModel tableModel;
    private JButton deleteUserButton;
    private JButton editUserButton;

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false&createDatabaseIfNotExist=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private String loggedInUserRole;
    private User LoggedInUser;

    public UserAccountManagement(String role, User user) {
        this.loggedInUserRole = role;
        this.LoggedInUser = user;
        initializeUI();
        displayUserList();
    }

    private void initializeUI() {
        setTitle("User Account Management");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        // User Tab
        JPanel userPanel = createUserPanel();
        tabbedPane.addTab("User Management", userPanel);

        // Student Tab
        JPanel studentPanel = createStudentPanel(loggedInUserRole);
        tabbedPane.addTab("Student Management", studentPanel);

        // Profile Tab
        JPanel profilePanel = new Profile(LoggedInUser);
        tabbedPane.addTab("Profile", profilePanel);

        add(tabbedPane);
        setLocationRelativeTo(null);
    }

    private JPanel createStudentPanel(String loggedInUser) {
        StudentManagement studentManagement = new StudentManagement(loggedInUser);


        JPanel studentPanel = new JPanel();
        studentPanel.setLayout(new BorderLayout());


        studentPanel.add(studentManagement.getContentPane(), BorderLayout.CENTER);


        return studentPanel;
    }

    private JPanel createUserPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JButton viewHistoryButton = new JButton("View History");
        viewHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewLoginHistory();
            }
        });

        JTextField searchField = new JTextField("Enter Name or Role or Status");
        JButton searchButton = new JButton("Search");
        searchField.setForeground(Color.GRAY);

        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Enter Name or Role or Status")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        searchField.setPreferredSize(new Dimension(200, 25));
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Handle search button click
                String searchText = searchField.getText();
                performSearch(searchText);
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);


        mainPanel.add(searchPanel, BorderLayout.NORTH);

        logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogout();
            }
        });
        // User Table
        String[] columnNames = {"User Id", "Username", "Phone Number", "Age", "Status", "Role"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {

                return false;
            }
        };
        userTable = new JTable(tableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        userTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRowIndex = userTable.getSelectedRow();
                editUserButton.setEnabled(selectedRowIndex != -1); }
        });

        JScrollPane tableScrollPane = new JScrollPane(userTable);

        int userIdColumnIndex = 0;
        TableColumn userIdColumn = userTable.getColumnModel().getColumn(userIdColumnIndex);
        userIdColumn.setMinWidth(0);
        userIdColumn.setMaxWidth(0);
        userIdColumn.setPreferredWidth(0);
        userIdColumn.setResizable(false);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("User Account List");
        Font titleFont = titledBorder.getTitleFont();
        titleFont = titleFont.deriveFont(titleFont.getSize() + 5.0f);
        titledBorder.setTitleFont(titleFont);
        titledBorder.setTitleJustification(TitledBorder.CENTER);
        tableScrollPane.setBorder(titledBorder);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addUserButton = new JButton("Add User");
        editUserButton = new JButton("Edit User");
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


        editUserButton.setEnabled(false);


        editUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedUser();
            }
        });

        addUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ("admin".equals(loggedInUserRole)) {
                    User newUser = showUserInputDialog();
                    if (newUser != null) {
                        addUserToDatabase(newUser);
                    }
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "You do not have the required role to add users.");
                }
            }
        });

        deleteUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedUser();
            }
        });

        // ...

        viewUserInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRowIndex = userTable.getSelectedRow();
                if (selectedRowIndex != -1) {
                    User selectedUser = getUserFromTable(selectedRowIndex);

                    // Retrieve the profile picture from the database
                    byte[] profilePictureBytes = getProfilePictureFromDatabase(selectedUser.getId());

                    // Create a panel with non-editable fields for viewing
                    JPanel viewPanel = new JPanel(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();

                    // Add profile picture to the left
                    if (profilePictureBytes != null) {
                        ImageIcon profilePictureIcon = new ImageIcon(profilePictureBytes);
                        // Resize the profile picture
                        Image scaledProfilePicture = profilePictureIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                        ImageIcon scaledProfilePictureIcon = new ImageIcon(scaledProfilePicture);
                        JLabel profilePictureLabel = new JLabel(scaledProfilePictureIcon);
                        gbc.gridx = 0;
                        gbc.gridy = 0;
                        gbc.gridheight = 5;  // Span 5 rows
                        gbc.insets = new Insets(0, 0, 0, 10);  // Add a small distance to the right
                        gbc.anchor = GridBagConstraints.LINE_START;
                        viewPanel.add(profilePictureLabel, gbc);
                    }

                    // Add user information to the right
                    gbc.gridx = 1;
                    gbc.gridy = 0;
                    gbc.gridheight = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    viewPanel.add(new JLabel("Name:"), gbc);
                    gbc.gridy++;
                    viewPanel.add(new JLabel("Age:"), gbc);
                    gbc.gridy++;
                    viewPanel.add(new JLabel("Phone Number:"), gbc);
                    gbc.gridy++;
                    viewPanel.add(new JLabel("Status:"), gbc);
                    gbc.gridy++;
                    viewPanel.add(new JLabel("Role:"), gbc);

                    JTextField nameField = createNonEditableTextField(selectedUser.getName());
                    JTextField ageField = createNonEditableTextField(String.valueOf(selectedUser.getAge()));
                    JTextField phoneNumberField = createNonEditableTextField(selectedUser.getPhoneNumber());
                    JTextField statusField = createNonEditableTextField(selectedUser.getStatus());
                    JTextField roleField = createNonEditableTextField(selectedUser.getRole());

                    gbc.gridx = 2;
                    gbc.gridy = 0;
                    gbc.gridheight = 1;
                    gbc.anchor = GridBagConstraints.LINE_START;
                    viewPanel.add(nameField, gbc);
                    gbc.gridy++;
                    viewPanel.add(ageField, gbc);
                    gbc.gridy++;
                    viewPanel.add(phoneNumberField, gbc);
                    gbc.gridy++;
                    viewPanel.add(statusField, gbc);
                    gbc.gridy++;
                    viewPanel.add(roleField, gbc);

                    JOptionPane.showConfirmDialog(null, viewPanel, "User Info", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to view.");
                }
            }
        });


        buttonPanel.add(viewUserInfoButton);
        if ("admin".equals(loggedInUserRole)) {
            buttonPanel.add(editUserButton);
            buttonPanel.add(deleteUserButton);
            buttonPanel.add(addUserButton);
            buttonPanel.add(viewHistoryButton);
        }
        logoutButton.setBackground(new Color(255, 0, 0));
        buttonPanel.add(logoutButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private byte[] getProfilePictureFromDatabase(int userId) {
        try (Connection connection = establishConnection()) {
            String query = "SELECT profile_picture FROM users WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBytes("profile_picture");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void viewLoginHistory() {
        int selectedRowIndex = userTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            User selectedUser = getUserFromTable(selectedRowIndex);
            List<LoginHistoryEntry> loginHistory = getLoginHistory(selectedUser.getId());

            // Display the login history in a dialog
            showLoginHistoryDialog(loginHistory);
        } else {
            JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to view login history.");
        }
    }

    private void showLoginHistoryDialog(List<LoginHistoryEntry> loginHistory) {
        JPanel historyPanel = new JPanel(new GridLayout(loginHistory.size(), 1));

        for (LoginHistoryEntry entry : loginHistory) {
            JLabel entryLabel = new JLabel("Login Time: " + entry.getLoginTime());
            historyPanel.add(entryLabel);
        }

        JScrollPane scrollPane = new JScrollPane(historyPanel);
        JOptionPane.showConfirmDialog(null, scrollPane, "Login History", JOptionPane.DEFAULT_OPTION);

    }

    private List<LoginHistoryEntry> getLoginHistory(int userId) {
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

    private void performSearch(String searchText) {
        if (searchText.isEmpty()) {
            displayUserList();
        } else {
            List<User> searchResults = searchUsersInDatabase(searchText);
            searchResults.removeIf(s -> s.getName().equals("admin"));

            updateTableBasedOnSearch(searchResults);
        }
    }

    private void updateTableBasedOnSearch(List<User> searchResults) {
        tableModel.setRowCount(0);
        for (User user : searchResults) {
            addUserToTable(user);
        }
    }

    private List<User> searchUsersInDatabase(String searchText) {
        List<User> userList = retrieveUserListFromDatabase();
        List<User> searchResults = new ArrayList<>();

        for (User user : userList) {
            if (userMatchesSearch(user, searchText)) {
                searchResults.add(user);
            }
        }

        return searchResults;
    }

    private boolean userMatchesSearch(User user, String searchText) {
        String lowerCaseSearchText = searchText.toLowerCase();

        return user.getName().toLowerCase().contains(lowerCaseSearchText)
                || user.getRole().toLowerCase().contains(lowerCaseSearchText)
                || user.getStatus().toLowerCase().contains(lowerCaseSearchText);

    }

    private void handleLogout() {
        int confirmDialogResult = JOptionPane.showConfirmDialog(
                UserAccountManagement.this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirmDialogResult == JOptionPane.YES_OPTION) {
            dispose();  // Close the current frame
            // Open the login form
            SwingUtilities.invokeLater(() -> {
                LoginForm loginForm = new LoginForm();
                loginForm.setVisible(true);
            });
        }
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
            String query = "INSERT INTO users (name, age, phone_number, status, password, role, profile_picture) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, user.getName());
                statement.setInt(2, user.getAge());
                statement.setString(3, user.getPhoneNumber());
                statement.setString(4, user.getStatus());
                statement.setString(5, user.getPassword());
                statement.setString(6, user.getRole());
                statement.setBytes(7, user.getProfilePicture());
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
                    String password = resultSet.getString("password");
                    String role = resultSet.getString("role");
                    byte[] profile_pic = resultSet.getBytes("profile_picture");
                    userList.add(new User(id, name, age, phoneNumber, status, password, role,profile_pic));
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
            if (!user.Equals(LoggedInUser) && !user.getName().equals("admin")) {
                addUserToTable(user);
            }
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

            // Only allow admin to delete users
            if ("admin".equals(loggedInUserRole)) {
                // Show a confirmation dialog
                int confirmDialogResult = JOptionPane.showConfirmDialog(
                        UserAccountManagement.this,
                        "Are you sure you want to delete the selected user?",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);

                if (confirmDialogResult == JOptionPane.YES_OPTION) {
                    // User confirmed, proceed with deletion
                    deleteSelectedUserFromDatabase(selectedUser);
                    tableModel.removeRow(selectedRowIndex);
                }
            } else {
                JOptionPane.showMessageDialog(UserAccountManagement.this, "You do not have the required role to delete users.");
            }
        } else {
            JOptionPane.showMessageDialog(UserAccountManagement.this, "Select a user to delete.");
        }
    }

    private void editSelectedUserInDatabase(User user) {
        try (Connection connection = establishConnection()) {
            String queryUpdate = "UPDATE users SET name = ?, age = ?, phone_number = ?, status = ?, role = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {
                updateStatement.setString(1, user.getName());
                updateStatement.setInt(2, user.getAge());
                updateStatement.setString(3, user.getPhoneNumber());
                updateStatement.setString(4, user.getStatus());
                updateStatement.setString(5, user.getRole());
                updateStatement.setInt(6, user.getId());

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

            // Only allow admin to edit users
            if ("admin".equals(loggedInUserRole)) {
                User editedUser = showEditUserInputDialog(selectedUser);
                if (editedUser != null) {
                    updateUserInTable(selectedRowIndex, editedUser);
                    editSelectedUserInDatabase(editedUser);
                }
            } else {
                JOptionPane.showMessageDialog(UserAccountManagement.this, "You do not have the required role to edit users.");
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
        tableModel.setValueAt(user.getRole(),rowIndex,5);
    }

    private User showEditUserInputDialog(User user) {
        String[] roleOptions = {"manager", "employee"};
        JComboBox<String> roleComboBox = new JComboBox<>(roleOptions);
        roleComboBox.setSelectedItem(user.getRole());
        JTextField nameField = new JTextField(user.getName());
        JTextField ageField = new JTextField(String.valueOf(user.getAge()));
        JTextField phoneNumberField = new JTextField(user.getPhoneNumber());



        String[] statusOptions = {"Normal", "Locked"};
        JComboBox<String> statusComboBox = new JComboBox<>(statusOptions);
        statusComboBox.setSelectedItem(user.getStatus());

        JPanel panel = new JPanel(new GridLayout(7, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneNumberField);
        panel.add(new JLabel("Role:"));
        panel.add(roleComboBox);
        panel.add(new JLabel("Status:"));
        panel.add(statusComboBox);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit User Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int age = Integer.parseInt(ageField.getText());

                if (phoneNumberField.getText().matches("0\\d{9}")) {
                    return new User(user.getId(), nameField.getText(), age, phoneNumberField.getText(), (String) statusComboBox.getSelectedItem(), "", (String) roleComboBox.getSelectedItem(), null);
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

        String[] roleOptions = {"manager", "employee"};
        JComboBox<String> roleComboBox = new JComboBox<>(roleOptions);
        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();
        JTextField phoneNumberField = new JTextField();
        JPasswordField passwordField = new JPasswordField();



        String[] statusOptions = {"Normal", "Locked"};
        JComboBox<String> statusComboBox = new JComboBox<>(statusOptions);
        ImageIcon defaultProfileIcon = new ImageIcon(getClass().getClassLoader().getResource("pic/default.png"));

        JPanel panel = new JPanel(new GridLayout(6, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Phone Number:"));
        panel.add(phoneNumberField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleComboBox);
        panel.add(new JLabel("Status:"));
        panel.add(statusComboBox);


        while (true) {
            int result = JOptionPane.showConfirmDialog(null, panel, "Add New User", JOptionPane.OK_CANCEL_OPTION);
            if (result == JOptionPane.OK_OPTION) {
                String name = nameField.getText();
                String ageText = ageField.getText();
                String phoneNumber = phoneNumberField.getText();
                String password = new String(passwordField.getPassword());
                String role = (String) roleComboBox.getSelectedItem();
                String status = (String) statusComboBox.getSelectedItem();

                if (!name.isEmpty() && !ageText.isEmpty() && !phoneNumber.isEmpty() && !password.isEmpty() && !role.isEmpty()) {
                    try {
                        int age = Integer.parseInt(ageText);


                        if (phoneNumber.matches("0\\d{9}")) {
                            byte[] defaultProfilePictureBytes = getDefaultProfilePictureBytes(defaultProfileIcon);
                            return new User(0, name, age, phoneNumber, status, password, role,defaultProfilePictureBytes);
                        } else {
                            JOptionPane.showMessageDialog(UserAccountManagement.this, "Phone number must begin with 0 and be followed by 9 digits.");
                        }
                    } catch (NumberFormatException e) {
                        JOptionPane.showMessageDialog(UserAccountManagement.this, "Invalid input for age.");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    JOptionPane.showMessageDialog(UserAccountManagement.this, "Please fill in all user information.");
                }
            } else {

                return null;
            }
        }
    }

    private byte[] getDefaultProfilePictureBytes(ImageIcon defaultProfileIcon) throws IOException {
        BufferedImage image = new BufferedImage(defaultProfileIcon.getIconWidth(), defaultProfileIcon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
        defaultProfileIcon.paintIcon(null, image.getGraphics(), 0, 0);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        return baos.toByteArray();
    }

    private void addUserToTable(User newUser) {
        boolean userExists = false;

        // Check if the user already exists in the table
        for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
            int existingUserId = (int) tableModel.getValueAt(rowIndex, 0);
            if (existingUserId == newUser.getId()) {

                updateUserInTable(rowIndex, newUser);
                userExists = true;
                break;
            }
        }


        if (!userExists) {
            Object[] rowData = {newUser.getId(), newUser.getName(), newUser.getPhoneNumber(), newUser.getAge(), newUser.getStatus(), newUser.getRole()};
            tableModel.addRow(rowData);
        }
    }

    private User getUserFromTable(int rowIndex) {
        int id = (int) userTable.getValueAt(rowIndex, 0);
        String name = (String) userTable.getValueAt(rowIndex, 1);
        String phoneNumber = (String) userTable.getValueAt(rowIndex, 2);
        int age = (int) userTable.getValueAt(rowIndex, 3);
        String status = (String) userTable.getValueAt(rowIndex, 4);
        String role = (String) userTable.getValueAt(rowIndex, 5);
        return new User(id, name, age, phoneNumber, status,"", role,null);
    }

}

package com.lch;

import com.lch.model.Student;
import com.lch.model.Certificate;
import com.opencsv.CSVWriter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDetail extends JFrame {
    private Student student;

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private JTable certificateTable;

    private JButton addCertificateButton;
    private JButton editCertificateButton;
    private JButton deleteCertificateButton;
    private DefaultTableModel certificateTableModel;

    private JButton importCertificateButton;

    private JButton exportCertificatesButton;
    private String userRole;

    public StudentDetail(Student student, String userRole) {
        this.student = student;
        this.userRole = userRole;
        initializeUI();
        createTablesIfNotExist();
        displayStudentInfo();
        displayCertificateList();
    }



    private void initializeUI() {
        setTitle("Student Detail");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addCertificateButton = new JButton("Add Certificate");
        editCertificateButton = new JButton("Edit Certificate");
        deleteCertificateButton = new JButton("Delete Certificate");
        importCertificateButton = new JButton("Import Certificates");
        exportCertificatesButton = new JButton("Export Certificates");

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel studentInfoPanel = createStudentInfoPanel();
        JPanel certificatePanel = createCertificatePanel();

        mainPanel.add(studentInfoPanel, BorderLayout.WEST);
        mainPanel.add(certificatePanel, BorderLayout.CENTER);


        addCertificateButton.addActionListener(e -> {
            showAddCertificateDialog();
        });

        exportCertificatesButton.addActionListener(e -> {
            exportCertificates();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());

        if("admin".equals(userRole) || "manager".equals(userRole )){
            buttonPanel.add(addCertificateButton);
            buttonPanel.add(editCertificateButton);
            buttonPanel.add(deleteCertificateButton);
            buttonPanel.add(importCertificateButton);
            buttonPanel.add(exportCertificatesButton);
        }

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        deleteCertificateButton.addActionListener(e -> {
            int selectedRow = certificateTable.getSelectedRow();

            if (selectedRow != -1) {
                int confirmDialogResult = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete this certificate?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);

                if (confirmDialogResult == JOptionPane.YES_OPTION) {
                    String certificateName = (String) certificateTable.getValueAt(selectedRow, 0);
                    String description = (String) certificateTable.getValueAt(selectedRow, 1);

                    // Delete the certificate from the table and the database
                    deleteCertificateFromTableAndDatabase(certificateName, description);

                    // Show a message to inform the user about the deletion
                    JOptionPane.showMessageDialog(this, "Certificate deleted successfully.", "Deletion Successful", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a certificate to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        importCertificateButton.addActionListener(e -> {
            showImportCertificateDialog();
        });


        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private void exportCertificates() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Certificates to CSV");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            if (!filePath.toLowerCase().endsWith(".csv")) {
                selectedFile = new File(filePath + ".csv");
            }

            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            // Call a method to export certificates to the CSV file
            exportCertificatesToCSV(selectedFile);
        }
    }

    private void exportCertificatesToCSV(File csvFile) {
        try (Writer writer = new FileWriter(csvFile);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.NO_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            // Write custom CSV header
            String[] header = {"Certificate Name", "Description"};
            csvWriter.writeNext(header);

            // Write certificate data to CSV
            for (int i = 0; i < certificateTableModel.getRowCount(); i++) {
                String certificateName = (String) certificateTableModel.getValueAt(i, 0);
                String description = (String) certificateTableModel.getValueAt(i, 1);

                String[] rowData = {certificateName, description};
                csvWriter.writeNext(rowData);
            }

            JOptionPane.showMessageDialog(this, "Certificates exported to CSV file successfully.");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting certificates to CSV file.");
        }
    }

    private void showImportCertificateDialog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            importCertificatesFromFile(selectedFile);
            // Update the display after importing certificates
            displayCertificateList();
        }
    }

    private void importCertificatesFromFile(File csvFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the first line
                }

                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String certificateName = parts[0].trim();
                    String description = parts[1].trim();


                    addCertificateToTableAndDatabase(certificateName, description);
                } else {
                    System.err.println("Invalid line format in CSV file: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deleteCertificateFromTableAndDatabase(String certificateName, String description) {
        int selectedRow = certificateTable.getSelectedRow();
        certificateTableModel.removeRow(selectedRow);

        // Delete the certificate from the database
        deleteCertificateFromDatabase(student.getId(), certificateName, description);


    }

    private void deleteCertificateFromDatabase(int studentId, String certificateName, String description) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String deleteCertificateQuery = "DELETE FROM certificates " +
                    "WHERE student_id = ? AND certificate_name = ? AND description = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteCertificateQuery)) {
                preparedStatement.setInt(1, studentId);
                preparedStatement.setString(2, certificateName);
                preparedStatement.setString(3, description);

                // Execute the delete query
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected <= 0) {
                    // Handle the case where the deletion didn't affect any rows
                    // (e.g., show an error message)
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            // Handle the exception (e.g., show an error message)
        }

    }

    private void showAddCertificateDialog() {
        JDialog addCertificateDialog = new JDialog(this, "Add Certificate", true);
        addCertificateDialog.setSize(300, 200);
        addCertificateDialog.setLayout(new GridLayout(4, 2));

        // Components for the dialog
        JTextField certificateNameField = new JTextField();
        JTextField descriptionField = new JTextField();
        JButton addButton = new JButton("Add");

        // Add components to the dialog
        addCertificateDialog.add(new JLabel("Certificate Name:"));
        addCertificateDialog.add(certificateNameField);
        addCertificateDialog.add(new JLabel("Description:"));
        addCertificateDialog.add(descriptionField);
        addCertificateDialog.add(new JLabel()); // Empty label for spacing
        addCertificateDialog.add(addButton);

        // Add action listener to the "Add" button in the dialog
        addButton.addActionListener(e -> {
            // Get values from the input fields
            String certificateName = certificateNameField.getText();
            String description = descriptionField.getText();

            // Validate input (you can add more validation as needed)

            // Add the new certificate to the table and the database
            Certificate newCertificate = addCertificateToTableAndDatabase(certificateName, description);

            // Close the dialog
            addCertificateDialog.dispose();
        });

        editCertificateButton.addActionListener(e -> {

            int selectedRow = certificateTable.getSelectedRow();

            if (selectedRow != -1) {

                String certificateName = (String) certificateTable.getValueAt(selectedRow, 0);
                String description = (String) certificateTable.getValueAt(selectedRow, 1);

                 showEditCertificateDialog(certificateName, description);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a certificate to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Set the location of the dialog relative to the main frame
        addCertificateDialog.setLocationRelativeTo(this);

        // Make the dialog visible
        addCertificateDialog.setVisible(true);
    }

    private void showEditCertificateDialog(String certificateName, String description) {
        JDialog editCertificateDialog = new JDialog(this, "Edit Certificate", true);
        editCertificateDialog.setSize(300, 200);
        editCertificateDialog.setLayout(new GridLayout(4, 2));

        // Components for the dialog
        JTextField editedCertificateNameField = new JTextField(certificateName);
        JTextField editedDescriptionField = new JTextField(description);
        JButton updateButton = new JButton("Update");

        // Add components to the dialog
        editCertificateDialog.add(new JLabel("Certificate Name:"));
        editCertificateDialog.add(editedCertificateNameField);
        editCertificateDialog.add(new JLabel("Description:"));
        editCertificateDialog.add(editedDescriptionField);
        editCertificateDialog.add(new JLabel()); // Empty label for spacing
        editCertificateDialog.add(updateButton);

        // Add action listener to the "Update" button in the dialog
        updateButton.addActionListener(e -> {
            // Get values from the input fields
            String updatedCertificateName = editedCertificateNameField.getText();
            String updatedDescription = editedDescriptionField.getText();

            // Validate input (you can add more validation as needed)

            // Update the certificate in the table and the database
            updateCertificateInTableAndDatabase(certificateName, description, updatedCertificateName, updatedDescription);

            // Close the dialog
            editCertificateDialog.dispose();
        });

        // Set the location of the dialog relative to the main frame
        editCertificateDialog.setLocationRelativeTo(this);

        // Make the dialog visible
        editCertificateDialog.setVisible(true);
    }

    private void updateCertificateInTableAndDatabase(String oldCertificateName, String oldDescription, String updatedCertificateName, String updatedDescription) {
        int selectedRow = certificateTable.getSelectedRow();
        certificateTable.setValueAt(updatedCertificateName, selectedRow, 0);
        certificateTable.setValueAt(updatedDescription, selectedRow, 1);

        // Update the certificate in the database
        updateCertificateInDatabase(student.getId(), oldCertificateName, oldDescription,
                updatedCertificateName, updatedDescription);

    }

    private void updateCertificateInDatabase(int studentId, String oldCertificateName, String oldDescription, String updatedCertificateName, String updatedDescription) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String updateCertificateQuery = "UPDATE certificates " +
                    "SET certificate_name = ?, description = ? " +
                    "WHERE student_id = ? AND certificate_name = ? AND description = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateCertificateQuery)) {
                preparedStatement.setString(1, updatedCertificateName);
                preparedStatement.setString(2, updatedDescription);
                preparedStatement.setInt(3, studentId);
                preparedStatement.setString(4, oldCertificateName);
                preparedStatement.setString(5, oldDescription);

                // Execute the update query
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected <= 0) {
                    // Handle the case where the update didn't affect any rows
                    // (e.g., show an error message)
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message)
        }

    }

    private Certificate addCertificateToTableAndDatabase(String certificateName, String description) {
        Object[] rowData = {certificateName, description};
        certificateTableModel.addRow(rowData);

        int generatedId = insertCertificateIntoDatabase(student.getId(), certificateName, description);

        return new Certificate(generatedId, student.getId(), certificateName, description);

    }

    private int insertCertificateIntoDatabase(int studentId, String certificateName, String description) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String insertCertificateQuery = "INSERT INTO certificates (student_id, certificate_name, description) " +
                    "VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertCertificateQuery, Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, studentId);
                preparedStatement.setString(2, certificateName);
                preparedStatement.setString(3, description);

                // Execute the query and get the generated keys
                int rowsAffected = preparedStatement.executeUpdate();
                if (rowsAffected > 0) {
                    ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Return the generated ID
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message)
        }
        return -1; // Return -1 if insertion fails
    }

    private void createTablesIfNotExist() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {

            // Create certificates table
            String createCertificatesTableQuery = "CREATE TABLE IF NOT EXISTS certificates ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "student_id INT NOT NULL,"
                    + "certificate_name VARCHAR(255) NOT NULL,"
                    + "description TEXT NOT NULL,"
                    + "FOREIGN KEY (student_id) REFERENCES students(id))";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createCertificatesTableQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private JPanel createStudentInfoPanel() {
        JPanel studentInfoPanel = new JPanel(new GridLayout(5, 2));
        studentInfoPanel.setBorder(BorderFactory.createTitledBorder("Student Information"));

        JTextField idField = createNonEditableTextField(String.valueOf(student.getId()));
        JTextField nameField = createNonEditableTextField(student.getName());
        JTextField ageField = createNonEditableTextField(String.valueOf(student.getAge()));
        JTextField emailField = createNonEditableTextField(student.getEmail());

        studentInfoPanel.add(new JLabel("Student ID:"));
        studentInfoPanel.add(idField);
        studentInfoPanel.add(new JLabel("Name:"));
        studentInfoPanel.add(nameField);
        studentInfoPanel.add(new JLabel("Age:"));
        studentInfoPanel.add(ageField);
        studentInfoPanel.add(new JLabel("Email:"));
        studentInfoPanel.add(emailField);

        return studentInfoPanel;
    }

    private JPanel createCertificatePanel() {
        JPanel certificatePanel = new JPanel(new BorderLayout());
        certificatePanel.setBorder(BorderFactory.createTitledBorder("Certificates"));

        String[] columnNames = {"Certificate Name", "Description"};
        certificateTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        certificateTable = new JTable(certificateTableModel);
        JScrollPane certificateScrollPane = new JScrollPane(certificateTable);

        certificatePanel.add(certificateScrollPane, BorderLayout.CENTER);

        return certificatePanel;
    }

    private void displayStudentInfo() {
        // Display student information in the left panel
        // (Already implemented in createStudentInfoPanel())
    }

    private void displayCertificateList() {
        // Retrieve certificate details from the database based on the student ID
        List<Certificate> certificateList = retrieveCertificateListFromDatabase(student.getId());

        // Clear the existing table data
        certificateTableModel.setRowCount(0);

        // Add certificate details to the table
        for (Certificate certificate : certificateList) {
            addCertificateToTable(certificate);
        }
    }

    private List<Certificate> retrieveCertificateListFromDatabase(int studentId) {
        List<Certificate> certificateList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String retrieveCertificatesQuery = "SELECT * FROM certificates WHERE student_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(retrieveCertificatesQuery)) {
                preparedStatement.setInt(1, studentId);

                // Execute the query and get the result set
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int certificateId = resultSet.getInt("id");
                        String certificateName = resultSet.getString("certificate_name");
                        String description = resultSet.getString("description");

                        // Create Certificate objects and add them to the list
                        Certificate certificate = new Certificate(certificateId, studentId, certificateName, description);
                        certificateList.add(certificate);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle the exception (e.g., show an error message)
        }

        return certificateList;
    }

    private void addCertificateToTable(Certificate certificate) {
        // Add certificate details to the table
        Object[] rowData = {certificate.getCertificateName(), certificate.getDescription()};
        certificateTableModel.addRow(rowData);
    }

    // Helper method to create non-editable text fields
    private JTextField createNonEditableTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setEditable(false);
        return textField;
    }
}

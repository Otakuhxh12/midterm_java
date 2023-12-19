package com.lch;

import com.lch.model.Certificate;
import com.lch.model.Student;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentManagement extends JFrame {
    private Student selectedStudent;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JButton deleteStudentButton;
    private JButton editStudentButton;

    private static final String JDBC_URL = "jdbc:mysql://localhost/midterm_javaswing?useSSL=false";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    public StudentManagement() {
        initializeUI();
        createStudentsTableIfNotExists();
        displayStudentList();
    }

    private void createStudentsTableIfNotExists() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS students ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255) NOT NULL,"
                    + "age INT NOT NULL)";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(createTableQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        setTitle("Student Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        String[] columnNames = {"Student Id", "Name", "Age"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRowIndex = studentTable.getSelectedRow();
                editStudentButton.setEnabled(selectedRowIndex != -1);
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(studentTable);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addStudentButton = new JButton("Add Student");
        editStudentButton = new JButton("Edit Student");
        deleteStudentButton = new JButton("Delete Student");
        JButton viewStudentInfoButton = new JButton("View Student Info");
        editStudentButton.setEnabled(false);

        addStudentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Student newStudent = showStudentInputDialog();
                if (newStudent != null) {
                    addStudentToDatabase(newStudent);
                }
            }
        });

        deleteStudentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedStudent();
            }
        });

        editStudentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editSelectedStudent();
            }
        });

        viewStudentInfoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRowIndex = studentTable.getSelectedRow();
                if (selectedRowIndex != -1) {
                    Student selectedStudent = getStudentFromTable(selectedRowIndex);
                    showStudentInfo(selectedStudent);
                } else {
                    JOptionPane.showMessageDialog(StudentManagement.this, "Select a student to view.");
                }
            }
        });


        buttonPanel.add(addStudentButton);
        buttonPanel.add(viewStudentInfoButton);
        buttonPanel.add(editStudentButton);
        buttonPanel.add(deleteStudentButton);
        JButton closeWindowButton = createCloseButton();
        buttonPanel.add(closeWindowButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
    }
    private JButton createCloseButton() {
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Close the current JFrame (StudentManagement window)
                dispose();
            }
        });
        return closeButton;
    }
    private void showStudentInfo(Student student) {
        JPanel viewPanel = new JPanel(new GridLayout(4, 2));

        JTextField idField = createNonEditableTextField(String.valueOf(student.getId()));
        JTextField nameField = createNonEditableTextField(student.getName());
        JTextField ageField = createNonEditableTextField(String.valueOf(student.getAge()));

        viewPanel.add(new JLabel("Student ID:"));
        viewPanel.add(idField);
        viewPanel.add(new JLabel("Name:"));
        viewPanel.add(nameField);
        viewPanel.add(new JLabel("Age:"));
        viewPanel.add(ageField);

        JButton certificatesButton = new JButton("Certificates");
        viewPanel.add(new JLabel("Certificates:"));
        viewPanel.add(certificatesButton);

        certificatesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayStudentList();
                openCertificatesForStudent(student);
            }
        });

        JOptionPane.showConfirmDialog(null, viewPanel, "Student Information", JOptionPane.DEFAULT_OPTION);
    }

    private void openCertificatesForStudent(Student student) {
        JDialog certificateDialog = new JDialog(this, "Certificates for " + student.getName(), Dialog.ModalityType.APPLICATION_MODAL);
        certificateDialog.setSize(600, 400);
        certificateDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel certificatePanel = new JPanel(new BorderLayout());

        String[] columnNames = {"Certificate ID", "Certificate Name", "Description"};
        DefaultTableModel certificateTableModel = new DefaultTableModel(columnNames, 0);
        JTable certificateTable = new JTable(certificateTableModel);

        refreshCertificateTable(certificateTableModel, student);

        JScrollPane certificateScrollPane = new JScrollPane(certificateTable);
        certificatePanel.add(certificateScrollPane, BorderLayout.CENTER);

        JButton addCertificateButton = new JButton("Add Certificate");
        JButton editCertificateButton = new JButton("Edit Certificate");
        JButton deleteCertificateButton = new JButton("Delete Certificate");
        JButton viewCertificateButton = new JButton("View Certificate");

        addCertificateButton.addActionListener(e -> addCertificate(student, certificateTableModel));
        editCertificateButton.addActionListener(e -> editCertificate(certificateTable, certificateTableModel));
        deleteCertificateButton.addActionListener(e -> deleteCertificate(certificateTable, certificateTableModel));
        viewCertificateButton.addActionListener(e -> viewCertificate(certificateTable));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addCertificateButton);
        buttonPanel.add(editCertificateButton);
        buttonPanel.add(deleteCertificateButton);
        buttonPanel.add(viewCertificateButton);

        certificatePanel.add(buttonPanel, BorderLayout.SOUTH);

        certificateDialog.add(certificatePanel);
        certificateDialog.setLocationRelativeTo(null);
        certificateDialog.setVisible(true);
    }

    private void refreshCertificateTable(DefaultTableModel model, Student student) {
        model.setRowCount(0);
        List<Certificate> certificates = retrieveCertificatesForStudent(student);

        for (Certificate certificate : certificates) {
            Object[] rowData = {certificate.getId(), certificate.getCertificateName(), certificate.getDescription()};
            model.addRow(rowData);
        }
    }

    private List<Certificate> retrieveCertificatesForStudent(Student student) {
        List<Certificate> certificateList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String query = "SELECT * FROM certificates WHERE student_id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, student.getId());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        int certificateId = resultSet.getInt("id");
                        String certificateName = resultSet.getString("certificate_name");
                        String description = resultSet.getString("description");
                        certificateList.add(new Certificate(certificateId, certificateName, description));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return certificateList;
    }

    private void addCertificate(Student student, DefaultTableModel model) {
        String certificateName = JOptionPane.showInputDialog(this, "Enter Certificate Name:");
        if (certificateName != null && !certificateName.trim().isEmpty()) {
            String description = JOptionPane.showInputDialog(this, "Enter Description:");
            if (description != null && !description.trim().isEmpty()) {
                try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                    String query = "INSERT INTO certificates (student_id, certificate_name, description) VALUES (?, ?, ?)";
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setInt(1, student.getId());
                        statement.setString(2, certificateName);
                        statement.setString(3, description);
                        int rowsInserted = statement.executeUpdate();
                        if (rowsInserted > 0) {
                            JOptionPane.showMessageDialog(this, "Certificate added successfully");
                            refreshCertificateTable(model, student);
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to add certificate");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Description cannot be empty");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Certificate name cannot be empty");
        }
    }

    private void editCertificate(JTable certificateTable, DefaultTableModel model) {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow != -1) {
            int certificateId = (int) model.getValueAt(selectedRow, 0);
            String certificateName = JOptionPane.showInputDialog(this, "Edit Certificate Name:", model.getValueAt(selectedRow, 1));
            if (certificateName != null && !certificateName.trim().isEmpty()) {
                String description = JOptionPane.showInputDialog(this, "Edit Description:", model.getValueAt(selectedRow, 2));
                if (description != null && !description.trim().isEmpty()) {
                    try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                        String query = "UPDATE certificates SET certificate_name = ?, description = ? WHERE id = ?";
                        try (PreparedStatement statement = connection.prepareStatement(query)) {
                            statement.setString(1, certificateName);
                            statement.setString(2, description);
                            statement.setInt(3, certificateId);
                            int rowsUpdated = statement.executeUpdate();
                            if (rowsUpdated > 0) {
                                JOptionPane.showMessageDialog(this, "Certificate updated successfully");
                                refreshCertificateTable(model, getSelectedStudent());
                            } else {
                                JOptionPane.showMessageDialog(this, "Failed to update certificate");
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Description cannot be empty");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Certificate name cannot be empty");
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a certificate to edit");
        }
    }

    private void deleteCertificate(JTable certificateTable, DefaultTableModel model) {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow != -1) {
            int certificateId = (int) model.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this certificate?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
                    String query = "DELETE FROM certificates WHERE id = ?";
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.setInt(1, certificateId);
                        int rowsDeleted = statement.executeUpdate();
                        if (rowsDeleted > 0) {
                            JOptionPane.showMessageDialog(this, "Certificate deleted successfully");
                            refreshCertificateTable(model, getSelectedStudent());
                        } else {
                            JOptionPane.showMessageDialog(this, "Failed to delete certificate");
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a certificate to delete");
        }
    }

    private Student getSelectedStudent() {
        int selectedRowIndex = studentTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            return getStudentFromTable(selectedRowIndex);
        }
        return null;
    }

    private void viewCertificate(JTable certificateTable) {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow != -1) {
            int certificateId = (int) certificateTable.getValueAt(selectedRow, 0);
            String certificateName = (String) certificateTable.getValueAt(selectedRow, 1);
            String description = (String) certificateTable.getValueAt(selectedRow, 2);
            JOptionPane.showMessageDialog(this, "Certificate ID: " + certificateId
                    + "\nCertificate Name: " + certificateName
                    + "\nDescription: " + description);
        } else {
            JOptionPane.showMessageDialog(this, "Select a certificate to view");
        }
    }


    // Helper method to create non-editable text fields
    private JTextField createNonEditableTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setEditable(false);
        return textField;
    }

    // Helper method to show input dialog for adding a new student
    private Student showStudentInputDialog() {
        JTextField nameField = new JTextField();
        JTextField ageField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add New Student", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                int age = Integer.parseInt(ageField.getText());
                return new Student(0, name, age);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(StudentManagement.this, "Invalid input for age.");
            }
        }
        return null;
    }

    private void addStudentToDatabase(Student student) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String query = "INSERT INTO students (name, age) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, student.getName());
                statement.setInt(2, student.getAge());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        student.setId(generatedKeys.getInt(1));
                    } else {
                        throw new SQLException("Failed to retrieve generated ID.");
                    }
                }
            }
            displayStudentList();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<Student> retrieveStudentListFromDatabase() {
        List<Student> studentList = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String query = "SELECT * FROM students";
            try (Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    int age = resultSet.getInt("age");
                    studentList.add(new Student(id, name, age));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return studentList;
    }

    private void displayStudentList() {
        tableModel.setRowCount(0);

        List<Student> studentListData = retrieveStudentListFromDatabase();
        for (Student student : studentListData) {
            addStudentToTable(student);
        }
    }

    private void deleteSelectedStudentFromDatabase(Student student) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String query = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, student.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedStudent() {
        int selectedRowIndex = studentTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            Student selectedStudent = getStudentFromTable(selectedRowIndex);
            deleteSelectedStudentFromDatabase(selectedStudent);
            tableModel.removeRow(selectedRowIndex);
        } else {
            JOptionPane.showMessageDialog(this, "Select a student to delete.");
        }
    }

    private void editSelectedStudentInDatabase(Student student) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String queryUpdate = "UPDATE students SET name = ?, age = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {
                updateStatement.setString(1, student.getName());
                updateStatement.setInt(2, student.getAge());
                updateStatement.setInt(3, student.getId());
                int rowsUpdated = updateStatement.executeUpdate();
                System.out.println("Rows Updated in Database: " + rowsUpdated);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void editSelectedStudent() {
        int selectedRowIndex = studentTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            Student selectedStudent = getStudentFromTable(selectedRowIndex);
            Student editedStudent = showEditStudentInputDialog(selectedStudent);
            if (editedStudent != null) {
                updateStudentInTable(selectedRowIndex, editedStudent);
                editSelectedStudentInDatabase(editedStudent);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a student to edit.");
        }
    }

    private void updateStudentInTable(int rowIndex, Student student) {
        tableModel.setValueAt(student.getId(), rowIndex, 0);
        tableModel.setValueAt(student.getName(), rowIndex, 1);
        tableModel.setValueAt(student.getAge(), rowIndex, 2);
    }

    private Student showEditStudentInputDialog(Student student) {
        JTextField idField = new JTextField(String.valueOf(student.getId()));
        JTextField nameField = new JTextField(student.getName());
        JTextField ageField = new JTextField(String.valueOf(student.getAge()));

        JPanel panel = new JPanel(new GridLayout(5, 2));
        panel.add(new JLabel("ID:"));
        panel.add(idField);
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit Student Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText());
                String name = nameField.getText();
                int age = Integer.parseInt(ageField.getText());
                return new Student(id, name, age);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input for ID or age.");
            }
        }
        return null;
    }

    private void addStudentToTable(Student newStudent) {
        Object[] rowData = {newStudent.getId(), newStudent.getName(), newStudent.getAge()};
        tableModel.addRow(rowData);
    }

    private Student getStudentFromTable(int rowIndex) {
        int id = (int) tableModel.getValueAt(rowIndex, 0);
        String name = (String) tableModel.getValueAt(rowIndex, 1);
        int age = (int) tableModel.getValueAt(rowIndex, 2);
        return new Student(id, name, age);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StudentManagement studentManagement = new StudentManagement();
            studentManagement.setVisible(true);
        });
    }
}

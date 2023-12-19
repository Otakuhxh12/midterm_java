package com.lch;

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
        JButton closeWindowButton = createCloseButton();
        buttonPanel.add(closeWindowButton);

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

        JOptionPane.showConfirmDialog(null, viewPanel, "Student Information", JOptionPane.DEFAULT_OPTION);
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new StudentManagement().setVisible(true);
            }
        });
    }
}

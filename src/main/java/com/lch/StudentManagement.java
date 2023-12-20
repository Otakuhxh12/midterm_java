package com.lch;

import com.lch.model.Student;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.*;
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
    private String userRole;

    public StudentManagement(String userRole) {
        this.userRole = userRole;
        initializeUI();
        createStudentsTableIfNotExists();
        displayStudentList();
    }

    private void createStudentsTableIfNotExists() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String createTableQuery = "CREATE TABLE IF NOT EXISTS students ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(255) NOT NULL,"
                    + "age INT NOT NULL,"
                    + "email VARCHAR(255) NOT NULL)";
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
        JButton importStudentsButton = new JButton("Import Students");

        JPanel mainPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField("Enter Name or Email or Age");
        JButton searchButton = new JButton("Search");
        searchField.setForeground(Color.GRAY);
        searchField.setPreferredSize(new Dimension(200, 25));

        // Add focus listener to remove the text when the user clicks on the field
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Enter Name or Email or Age")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });

        importStudentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importStudentsFromCSV();
            }
        });

        JButton exportStudentsButton = new JButton("Export Students");

        exportStudentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportStudentsToCSV();
            }
        });

        String[] columnNames = {"Student id", "Name", "Age", "Email"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchText = searchField.getText();
                if (!searchText.isEmpty()) {
                    filterStudentList(searchText);
                } else {
                    displayStudentList(); // If search field is empty, display all students
                }
            }
        });



        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(studentTable.getModel());
        studentTable.setRowSorter(sorter);

        // Add a selection listener to handle sorting when column headers are clicked
        studentTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int colIndex = studentTable.columnAtPoint(evt.getPoint());
                toggleSortOrder(colIndex);
            }
        });

        studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int selectedRowIndex = studentTable.getSelectedRow();
                editStudentButton.setEnabled(selectedRowIndex != -1);
            }
        });

        int studentIdColumnIndex = 0;
        TableColumn studentIdColumn = studentTable.getColumnModel().getColumn(studentIdColumnIndex);
        studentIdColumn.setMinWidth(0);
        studentIdColumn.setMaxWidth(0);
        studentIdColumn.setPreferredWidth(0);
        studentIdColumn.setResizable(false);

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

                    StudentDetail studentDetailFrame = new StudentDetail(selectedStudent, userRole);
                    studentDetailFrame.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(StudentManagement.this, "Select a student to view.");
                }
            }
        });

        buttonPanel.add(viewStudentInfoButton);

        if ("admin".equals(userRole) || "manager".equals(userRole)) {
            buttonPanel.add(addStudentButton);
            buttonPanel.add(editStudentButton);
            buttonPanel.add(deleteStudentButton);
            buttonPanel.add(importStudentsButton);
            buttonPanel.add(exportStudentsButton);
        }


        mainPanel.add(searchPanel, BorderLayout.NORTH);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private void toggleSortOrder(int columnIndex) {

        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) studentTable.getRowSorter();
        List<RowSorter.SortKey> sortKeys = new ArrayList<>(sorter.getSortKeys());

        // Check if the clicked column is already the primary sorted column
        boolean isPrimarySortColumn = sortKeys.size() > 0 && sortKeys.get(0).getColumn() == columnIndex;

        if (isPrimarySortColumn) {
            // Toggle the order of the primary sort column
            sortKeys.set(0, new RowSorter.SortKey(columnIndex, toggleSortOrder(sortKeys.get(0).getSortOrder())));
        } else {
            // Add a new primary sort key for the clicked column
            sortKeys.clear();
            sortKeys.add(new RowSorter.SortKey(columnIndex, SortOrder.ASCENDING));
        }

        sorter.setSortKeys(sortKeys);

    }

    private SortOrder toggleSortOrder(SortOrder currentSortOrder) {
        // Toggle the sorting order
        return (currentSortOrder == SortOrder.ASCENDING) ? SortOrder.DESCENDING : SortOrder.ASCENDING;
    }

    private void exportStudentsToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose location to save CSV file");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // Ensure the file has a .csv extension
            if (!filePath.toLowerCase().endsWith(".csv")) {
                selectedFile = new File(filePath + ".csv");
            }

            // Call a method to write the student data to the CSV file
            writeStudentsToCSV(selectedFile);
        }
    }

    private void writeStudentsToCSV(File csvFile) {
        try (Writer writer = new FileWriter(csvFile);
             CSVWriter csvWriter = new CSVWriter(writer,
                     CSVWriter.DEFAULT_SEPARATOR,
                     CSVWriter.NO_QUOTE_CHARACTER,
                     CSVWriter.NO_ESCAPE_CHARACTER,
                     CSVWriter.DEFAULT_LINE_END)) {

            // Write custom CSV header
            String[] header = {"Name", "Age", "Email"};
            csvWriter.writeNext(header);

            // Write student data to CSV
            List<Student> studentListData = retrieveStudentListFromDatabase();
            for (Student student : studentListData) {
                String[] rowData = {
                        student.getName(),
                        String.valueOf(student.getAge()),
                        student.getEmail()
                };
                csvWriter.writeNext(rowData);
            }

            JOptionPane.showMessageDialog(this, "Students exported to CSV file successfully.");

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting students to CSV file.");
        }
    }

    private void importStudentsFromCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CSV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());

            // Call a method to read and import data from the CSV file
            importStudentsFromCSVFile(selectedFile);
        }
    }

    private void importStudentsFromCSVFile(File csvFile) {
        try (Reader reader = new FileReader(csvFile);
             CSVReader csvReader = new CSVReader(reader)) {

            List<String[]> csvData = csvReader.readAll();

            boolean skipHeader = true;


            for (String[] row : csvData) {
                if (skipHeader) {
                    skipHeader = false;
                    continue; // Skip the header row
                }

                if (row.length == 3) {
                    String name = row[0].trim();
                    int age = Integer.parseInt(row[1].trim());
                    String email = row[2].trim();

                    // Create a new Student object
                    Student newStudent = new Student(0, name, age, email);

                    // Add the student to the database
                    addStudentToDatabase(newStudent);
                } else {
                    // Handle invalid row format (not enough columns)
                    System.out.println("Invalid row format in CSV file.");
                }
            }

            // Refresh the displayed student list after importing
            displayStudentList();

        } catch (IOException | CsvException | NumberFormatException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error importing students from CSV file.");
        }
    }

    private void filterStudentList(String searchText) {
        List<Student> filteredList = new ArrayList<>();
        for (Student student : retrieveStudentListFromDatabase()) {
            if (containsIgnoreCase(student.getName(), searchText) ||
                    String.valueOf(student.getAge()).contains(searchText) ||
                    containsIgnoreCase(student.getEmail(), searchText)) {
                filteredList.add(student);
            }
        }
        displayFilteredStudentList(filteredList);
    }

    private boolean containsIgnoreCase(String source, String target) {
        return source.toLowerCase().contains(target.toLowerCase());
    }


    private void displayFilteredStudentList(List<Student> filteredList) {
        tableModel.setRowCount(0);
        for (Student student : filteredList) {
            addStudentToTable(student);
        }
    }

    private void showStudentInfo(Student student) {
        JPanel viewPanel = new JPanel(new GridLayout(5, 2));

        JTextField idField = createNonEditableTextField(String.valueOf(student.getId()));
        JTextField nameField = createNonEditableTextField(student.getName());
        JTextField ageField = createNonEditableTextField(String.valueOf(student.getAge()));
        JTextField emailField = createNonEditableTextField(student.getEmail());

        viewPanel.add(new JLabel("Student ID:"));
        viewPanel.add(idField);
        viewPanel.add(new JLabel("Name:"));
        viewPanel.add(nameField);
        viewPanel.add(new JLabel("Age:"));
        viewPanel.add(ageField);
        viewPanel.add(new JLabel("Email:"));
        viewPanel.add(emailField);

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
        JTextField emailField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add New Student", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                int age = Integer.parseInt(ageField.getText());
                String email = emailField.getText();
                return new Student(0, name, age, email);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(StudentManagement.this, "Invalid input for age.");
            }
        }
        return null;
    }

    private void addStudentToDatabase(Student student) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String query = "INSERT INTO students (name, age, email) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, student.getName());
                statement.setInt(2, student.getAge());
                statement.setString(3, student.getEmail());
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
                    String email = resultSet.getString("email");
                    studentList.add(new Student(id, name, age, email));
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
            int dialogResult = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete this student?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                Student selectedStudent = getStudentFromTable(selectedRowIndex);
                deleteSelectedStudentFromDatabase(selectedStudent);
                tableModel.removeRow(selectedRowIndex);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a student to delete.");
        }
    }

    private void editSelectedStudentInDatabase(Student student) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            String queryUpdate = "UPDATE students SET name = ?, age = ?, email = ? WHERE id = ?";
            try (PreparedStatement updateStatement = connection.prepareStatement(queryUpdate)) {
                updateStatement.setString(1, student.getName());
                updateStatement.setInt(2, student.getAge());
                updateStatement.setString(3, student.getEmail());
                updateStatement.setInt(4, student.getId());

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
                // Update student in the table
                updateStudentInTable(selectedRowIndex, editedStudent);

                // Update student in the database
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
        tableModel.setValueAt(student.getEmail(), rowIndex, 3);
    }

    private Student showEditStudentInputDialog(Student student) {
        JTextField nameField = new JTextField(student.getName());
        JTextField ageField = new JTextField(String.valueOf(student.getAge()));
        JTextField emailField = new JTextField(student.getEmail());

        JPanel panel = new JPanel(new GridLayout(4, 2));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Age:"));
        panel.add(ageField);
        panel.add(new JLabel("Email:"));
        panel.add(emailField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit Student Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText();
                int age = Integer.parseInt(ageField.getText());
                String email = emailField.getText();
                return new Student(student.getId(), name, age, email);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid input for age.");
            }
        }
        return null;
    }

    private void addStudentToTable(Student newStudent) {
        Object[] rowData = {newStudent.getId(), newStudent.getName(), newStudent.getAge(), newStudent.getEmail()};
        tableModel.addRow(rowData);
    }

    private Student getStudentFromTable(int rowIndex) {
        int modelRowIndex = studentTable.convertRowIndexToModel(rowIndex);
        int id = (int) tableModel.getValueAt(modelRowIndex, 0);
        String name = (String) tableModel.getValueAt(modelRowIndex, 1);
        int age = (int) tableModel.getValueAt(modelRowIndex, 2);
        String email = (String) tableModel.getValueAt(modelRowIndex, 3);
        return new Student(id, name, age, email);
    }
}

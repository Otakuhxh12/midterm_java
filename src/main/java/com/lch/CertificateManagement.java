package com.lch;

import com.lch.model.Certificate;
import com.lch.model.Student;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class CertificateManagement extends JFrame {
    private JTable certificateTable;
    private Student student;
    private DefaultTableModel tableModel;

    public CertificateManagement(Student student) {
        this.student = student;
        initializeUI();
        displayCertificateList();
    }

    private void initializeUI() {
        setTitle("Certificate Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout());

        String[] columnNames = {"Certificate Id", "Certificate Name", "Description"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        certificateTable = new JTable(tableModel);
        certificateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScrollPane = new JScrollPane(certificateTable);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addCertificateButton = new JButton("Add Certificate");
        JButton editCertificateButton = new JButton("Edit Certificate");
        JButton deleteCertificateButton = new JButton("Delete Certificate");

        addCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Certificate newCertificate = showCertificateInputDialog();
                if (newCertificate != null) {
                    addCertificateToTable(newCertificate);
                }
            }
        });

        editCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRowIndex = certificateTable.getSelectedRow();
                if (selectedRowIndex != -1) {
                    Certificate selectedCertificate = getCertificateFromTable(selectedRowIndex);
                    Certificate editedCertificate = showEditCertificateInputDialog(selectedCertificate);
                    if (editedCertificate != null) {
                        updateCertificateInTable(selectedRowIndex, editedCertificate);
                    }
                } else {
                    JOptionPane.showMessageDialog(CertificateManagement.this, "Select a certificate to edit.");
                }
            }
        });

        deleteCertificateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedCertificate();
            }
        });

        buttonPanel.add(addCertificateButton);
        buttonPanel.add(editCertificateButton);
        buttonPanel.add(deleteCertificateButton);

        mainPanel.add(tableScrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private Certificate showCertificateInputDialog() {
        JTextField certificateNameField = new JTextField();
        JTextField descriptionField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Certificate Name:"));
        panel.add(certificateNameField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Add New Certificate", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String certificateName = certificateNameField.getText();
            String description = descriptionField.getText();
            return new Certificate(getNextCertificateId(), certificateName, description);
        }
        return null;
    }

    private Certificate showEditCertificateInputDialog(Certificate certificate) {
        JTextField certificateNameField = new JTextField(certificate.getCertificateName());
        JTextField descriptionField = new JTextField(certificate.getDescription());

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Certificate Name:"));
        panel.add(certificateNameField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Edit Certificate Information", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String certificateName = certificateNameField.getText();
            String description = descriptionField.getText();
            return new Certificate(certificate.getId(), certificateName, description);
        }
        return null;
    }

    private void addCertificateToTable(Certificate certificate) {
        Object[] rowData = {certificate.getId(), certificate.getCertificateName(), certificate.getDescription()};
        tableModel.addRow(rowData);
    }

    private void updateCertificateInTable(int rowIndex, Certificate certificate) {
        tableModel.setValueAt(certificate.getId(), rowIndex, 0);
        tableModel.setValueAt(certificate.getCertificateName(), rowIndex, 1);
        tableModel.setValueAt(certificate.getDescription(), rowIndex, 2);
    }

    private Certificate getCertificateFromTable(int rowIndex) {
        int id = (int) tableModel.getValueAt(rowIndex, 0);
        String certificateName = (String) tableModel.getValueAt(rowIndex, 1);
        String description = (String) tableModel.getValueAt(rowIndex, 2);
        return new Certificate(id, certificateName, description);
    }

    private void deleteSelectedCertificate() {
        int selectedRowIndex = certificateTable.getSelectedRow();
        if (selectedRowIndex != -1) {
            tableModel.removeRow(selectedRowIndex);
        } else {
            JOptionPane.showMessageDialog(this, "Select a certificate to delete.");
        }
    }

    private int getNextCertificateId() {
        // Implement logic to get the next available certificate ID
        // You can use a variable or retrieve the ID from a database
        // For this example, using a simple increment
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            return 1;
        } else {
            int maxId = (int) tableModel.getValueAt(rowCount - 1, 0);
            return maxId + 1;
        }
    }

    private void displayCertificateList() {

    }

}

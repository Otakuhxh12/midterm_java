package com.lch.model;

public class Certificate {
    private int certificateId;
    private int studentId;
    private String certificateName;
    private String description;

    public Certificate(int certificateId, int studentId, String certificateName, String description) {
        this.certificateId = certificateId;
        this.studentId = studentId;
        this.certificateName = certificateName;
        this.description = description;
    }

    public int getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(int certificateId) {
        this.certificateId = certificateId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getCertificateName() {
        return certificateName;
    }

    public void setCertificateName(String certificateName) {
        this.certificateName = certificateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "certificateId=" + certificateId +
                ", studentId=" + studentId +
                ", certificateName='" + certificateName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}

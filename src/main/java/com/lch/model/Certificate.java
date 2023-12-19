package com.lch.model;

public class Certificate {
    private int id;
    private String certificateName;
    private String description;

    public Certificate(int id, String certificateName, String description) {
        this.id = id;
        this.certificateName = certificateName;
        this.description = description;
    }

    // Getters and setters for certificate properties
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

}

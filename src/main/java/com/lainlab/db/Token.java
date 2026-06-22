package com.lainlab.db;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@MappedEntity("tokens")
@Serdeable
public class Token {

    @Id
    @GeneratedValue
    private Long id;

    private String token;
    private int license_no;
    private String license_org;
    private String email;
    private int balance;
    private int total;
    private boolean active;
    private boolean admin;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    // getters/setters
    //
    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getBalance() {
        return balance;
    }

    public void setBalance(int balance) {
        this.balance = balance;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLicenseNo() {
        return license_no;
    }

    public void setLicenseNo(int licenseNo) {
        this.license_no = licenseNo;
    }

    public String getLicenseOrg() {
        return license_org;
    }

    public void setLicenseOrg(String licenseOrg) {
        this.license_org = licenseOrg;
    }
}

package com.lainlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateTokenRequest {

    @JsonProperty("license_no")
    private int licenseNo;
    @JsonProperty("license_org")
    private String licenseOrg;
    private String email;
    private int balance;
    private boolean admin;

    // Конструктор по умолчанию для десериализации
    public CreateTokenRequest() {}

    public CreateTokenRequest(int licenseNo, String licenseOrg, String email, int balance, boolean admin) {
        this.licenseNo = licenseNo;
        this.licenseOrg = licenseOrg;
        this.email = email;
        this.balance = balance;
        this.admin = admin;
    }

    public int getLicenseNo() { return licenseNo; }
    public void setLicenseNo(int licenseNo) { this.licenseNo = licenseNo; }

    public String getLicenseOrg() { return licenseOrg; }
    public void setLicenseOrg(String licenseOrg) { this.licenseOrg = licenseOrg; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getBalance() { return Math.max(0, balance); }
    public void setBalance(int balance) { this.balance = balance; }

    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
}

package com.lainlab.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class LicenseStats {

    private int license_no;
    private String license_org;
    private String email;
    private int balance;
    private long ai_requests;
    private long questions_stored;

    public LicenseStats() {
    }

    public int getLicense_no() {
        return license_no;
    }

    public void setLicense_no(int license_no) {
        this.license_no = license_no;
    }

    public String getLicense_org() {
        return license_org;
    }

    public void setLicense_org(String license_org) {
        this.license_org = license_org;
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

    public long getAi_requests() {
        return ai_requests;
    }

    public void setAi_requests(long ai_requests) {
        this.ai_requests = ai_requests;
    }

    public long getQuestions_stored() {
        return questions_stored;
    }

    public void setQuestions_stored(long questions_stored) {
        this.questions_stored = questions_stored;
    }
}

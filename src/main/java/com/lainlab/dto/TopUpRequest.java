package com.lainlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable.Deserializable
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopUpRequest {
    private int amount;

    public int getAmount() {
        return Math.max(0, amount);
    }

    public void setAmount(int amount) {
        if(amount < 0) {
            this.amount = 0;
        } else {
            this.amount = amount;
        }
    }
}

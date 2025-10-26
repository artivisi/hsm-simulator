package com.artivisi.hsm.simulator.dto;

import lombok.Data;

@Data
public class PinEncryptRequest {
    private String pin;
    private String accountNumber;
    private String format; // ISO-0, ISO-1, ISO-3, ISO-4
    private String keyId;
}

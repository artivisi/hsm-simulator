package com.artivisi.hsm.simulator.dto;

import lombok.Data;

@Data
public class KeyExchangeRequest {
    private String sourceKeyId;
    private String targetKeyId;
    private String keyType; // ZMK, ZPK, ZAK (ZSK), TEK (ZSK)
    private String keyData; // encrypted key to exchange
}

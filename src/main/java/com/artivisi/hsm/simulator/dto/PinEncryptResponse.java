package com.artivisi.hsm.simulator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinEncryptResponse {
    private String encryptedPinBlock;
    private String format;
    private String pvv;
}

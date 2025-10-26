package com.artivisi.hsm.simulator.config;

import com.artivisi.hsm.simulator.entity.KeyShare;
import com.artivisi.hsm.simulator.repository.KeyShareRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Custom converter that allows Spring to automatically convert shareId (String) to KeyShare entity.
 * This enables automatic entity resolution in controller path variables.
 */
@Component
@RequiredArgsConstructor
public class StringToKeyShareConverter implements Converter<String, KeyShare> {

    private final KeyShareRepository keyShareRepository;

    @Override
    public KeyShare convert(String shareId) {
        return keyShareRepository.findByShareId(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found: " + shareId));
    }
}

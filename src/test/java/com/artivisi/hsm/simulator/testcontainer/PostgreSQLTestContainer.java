package com.artivisi.hsm.simulator.testcontainer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class PostgreSQLTestContainer {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgresql = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("hsm_simulator_test")
            .withUsername("hsm_user")
            .withPassword("xK9m2pQ8vR5nF7tA1sD3wE6zY")
            .withReuse(true);

    @Test
    void contextLoads() {
        // Test that the container starts and connection is established
        System.out.println("PostgreSQL container is running on port: " + postgresql.getMappedPort(5432));
        System.out.println("JDBC URL: " + postgresql.getJdbcUrl());
        System.out.println("Database is healthy: " + postgresql.isRunning());
    }
}
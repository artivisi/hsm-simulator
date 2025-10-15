package com.artivisi.hsm.simulator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.artivisi.hsm.simulator.repository")
public class HsmSimulatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(HsmSimulatorApplication.class, args);
	}

}

package io.github.evertonsoethe.etlimporter;

import org.springframework.boot.SpringApplication;

public class TestEtlImporterApplication {

	public static void main(String[] args) {
		SpringApplication.from(EtlImporterApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

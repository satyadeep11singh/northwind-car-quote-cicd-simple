package com.northwindmutual.quotegen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Northwind Mutual Car Quote Generator - Spring Boot Application.
 *
 * <p>Project 4 of the BootAzure portfolio: a CI/CD pipeline (Jenkins, Maven,
 * SonarQube, Trivy, ACR, AKS) built around this application.
 *
 * <p>Domain model and structural patterns (entity hierarchy, controller
 * shape, repository conventions, Actuator wiring) are adapted from the
 * Spring PetClinic reference application
 * (https://github.com/spring-projects/spring-petclinic), Apache License 2.0.
 */
@SpringBootApplication
public class QuoteGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuoteGeneratorApplication.class, args);
	}

}

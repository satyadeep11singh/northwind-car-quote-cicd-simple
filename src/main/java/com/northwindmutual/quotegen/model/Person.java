package com.northwindmutual.quotegen.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Simple JavaBean domain object representing a person (first name, last name).
 *
 * <p>Pattern adapted from the Spring PetClinic reference application
 * (https://github.com/spring-projects/spring-petclinic), Apache License 2.0.
 */
@MappedSuperclass
public class Person extends BaseEntity {

	@Column(length = 30)
	@Size(max = 30)
	@NotBlank
	private String firstName;

	@Column(length = 30)
	@Size(max = 30)
	@NotBlank
	private String lastName;

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

}

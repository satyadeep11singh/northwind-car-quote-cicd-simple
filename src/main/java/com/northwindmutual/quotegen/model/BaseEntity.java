package com.northwindmutual.quotegen.model;

import java.io.Serializable;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Simple JavaBean domain object with an id property. Used as a base class for
 * entities needing this property.
 *
 * <p>Pattern adapted from the Spring PetClinic reference application
 * (https://github.com/spring-projects/spring-petclinic), Apache License 2.0.
 */
@MappedSuperclass
public class BaseEntity implements Serializable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public boolean isNew() {
		return this.id == null;
	}

}

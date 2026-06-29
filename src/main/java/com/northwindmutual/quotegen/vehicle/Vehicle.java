package com.northwindmutual.quotegen.vehicle;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * A vehicle to be insured, owned by a {@link Driver}.
 * Plays the same structural role that {@code Pet} plays in Spring PetClinic.
 *
 * <p>This side of the relationship owns the foreign key ({@code driver_id}),
 * making the {@code Driver}/{@code Vehicle} association bidirectional rather
 * than the unidirectional {@code @OneToMany + @JoinColumn} pattern Spring
 * PetClinic itself uses for {@code Owner}/{@code Pet}. That pattern is known
 * to insert child rows with a temporarily null foreign key before a
 * follow-up UPDATE sets it (see Vlad Mihalcea's and Thorben Janssen's
 * writeups on @OneToMany association mapping) - harmless against PetClinic's
 * nullable {@code owner_id} column, but incompatible with our deliberately
 * {@code NOT NULL driver_id} constraint. Making the relationship
 * bidirectional lets Hibernate set the foreign key directly in the INSERT.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

	private static final int MIN_MODEL_YEAR = 1980;

	private static final int MAX_MODEL_YEAR = 2027;

	@ManyToOne
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	@Column(name = "model_year")
	@NotNull
	@Min(MIN_MODEL_YEAR)
	@Max(MAX_MODEL_YEAR)
	private Integer modelYear;

	@Column
	@NotBlank
	private String make;

	@Column
	@NotBlank
	private String model;

	@Column(name = "usage_type")
	@NotNull
	@Enumerated(EnumType.STRING)
	private VehicleUsageType usageType;

	@Column(name = "annual_mileage")
	@NotNull
	@Positive
	private Integer annualMileage;

	public Driver getDriver() {
		return this.driver;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public Integer getModelYear() {
		return this.modelYear;
	}

	public void setModelYear(Integer modelYear) {
		this.modelYear = modelYear;
	}

	public String getMake() {
		return this.make;
	}

	public void setMake(String make) {
		this.make = make;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public VehicleUsageType getUsageType() {
		return this.usageType;
	}

	public void setUsageType(VehicleUsageType usageType) {
		this.usageType = usageType;
	}

	public Integer getAnnualMileage() {
		return this.annualMileage;
	}

	public void setAnnualMileage(Integer annualMileage) {
		this.annualMileage = annualMileage;
	}

	/**
	 * Convenience display label, e.g. "2021 Honda Civic".
	 * @return a human-readable description of this vehicle
	 */
	public String getDisplayName() {
		return this.modelYear + " " + this.make + " " + this.model;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}

}

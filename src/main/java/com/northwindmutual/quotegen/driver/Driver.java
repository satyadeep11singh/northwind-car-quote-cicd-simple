package com.northwindmutual.quotegen.driver;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.core.style.ToStringCreator;
import org.springframework.format.annotation.DateTimeFormat;

import com.northwindmutual.quotegen.model.Person;
import com.northwindmutual.quotegen.vehicle.Vehicle;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A driver requesting a car insurance quote. Plays the same structural role
 * that {@code Owner} plays in Spring PetClinic: a {@link Person} that owns a
 * collection of dependent entities (here, {@link Vehicle}s) and is the entry
 * point for the quote workflow.
 */
@Entity
@Table(name = "drivers")
public class Driver extends Person {

	@Column
	@NotBlank
	private String address;

	@Column
	@NotBlank
	private String city;

	@Column(name = "postal_code")
	@NotBlank
	@Pattern(regexp = "^[A-Za-z]\\d[A-Za-z][ -]?\\d[A-Za-z]\\d$", message = "{postalCode.invalid}")
	private String postalCode;

	@Column
	@NotBlank
	@Pattern(regexp = "\\d{10}", message = "{telephone.invalid}")
	private String telephone;

	@Column(name = "date_of_birth")
	@NotNull
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate dateOfBirth;

	@Column(name = "license_years_held")
	@NotNull
	@PositiveOrZero
	private Integer licenseYearsHeld;

	@OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@OrderBy("make")
	private final List<Vehicle> vehicles = new ArrayList<>();

	public String getAddress() {
		return this.address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostalCode() {
		return this.postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getTelephone() {
		return this.telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public LocalDate getDateOfBirth() {
		return this.dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Integer getLicenseYearsHeld() {
		return this.licenseYearsHeld;
	}

	public void setLicenseYearsHeld(Integer licenseYearsHeld) {
		this.licenseYearsHeld = licenseYearsHeld;
	}

	public List<Vehicle> getVehicles() {
		return this.vehicles;
	}

	public void addVehicle(Vehicle vehicle) {
		if (vehicle.isNew()) {
			vehicle.setDriver(this);
			getVehicles().add(vehicle);
		}
	}

	/**
	 * Return the Vehicle with the given id, or null if none found for this Driver.
	 * @param id to test
	 * @return the Vehicle with the given id, or null if no such Vehicle exists
	 */
	public Vehicle getVehicle(Integer id) {
		for (Vehicle vehicle : getVehicles()) {
			if (!vehicle.isNew()) {
				Integer compId = vehicle.getId();
				if (Objects.equals(compId, id)) {
					return vehicle;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("id", this.getId())
			.append("new", this.isNew())
			.append("lastName", this.getLastName())
			.append("firstName", this.getFirstName())
			.append("address", this.address)
			.append("city", this.city)
			.append("postalCode", this.postalCode)
			.append("telephone", this.telephone)
			.toString();
	}

}

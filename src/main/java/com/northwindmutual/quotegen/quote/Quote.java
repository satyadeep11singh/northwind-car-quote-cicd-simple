package com.northwindmutual.quotegen.quote;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.model.BaseEntity;
import com.northwindmutual.quotegen.vehicle.Vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * A generated insurance quote: the result of running a {@link Driver}'s and
 * {@link Vehicle}'s details, plus chosen coverage, through
 * {@link QuoteCalculationService}.
 *
 * <p>Stores not just the final premium but each individual rating factor
 * that contributed to it, so the quote result page can show a transparent
 * breakdown rather than a single opaque number.
 */
@Entity
@Table(name = "quotes")
public class Quote extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "driver_id", nullable = false)
	@NotNull
	private Driver driver;

	@ManyToOne
	@JoinColumn(name = "vehicle_id", nullable = false)
	@NotNull
	private Vehicle vehicle;

	@Column(name = "liability_limit")
	@NotNull
	@Enumerated(EnumType.STRING)
	private LiabilityLimit liabilityLimit;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private Deductible deductible;

	@Column(name = "base_rate", precision = 10, scale = 2)
	private BigDecimal baseRate;

	@Column(name = "age_factor", precision = 6, scale = 3)
	private BigDecimal ageFactor;

	@Column(name = "vehicle_age_factor", precision = 6, scale = 3)
	private BigDecimal vehicleAgeFactor;

	@Column(name = "usage_factor", precision = 6, scale = 3)
	private BigDecimal usageFactor;

	@Column(name = "coverage_factor", precision = 6, scale = 3)
	private BigDecimal coverageFactor;

	@Column(name = "annual_premium", precision = 10, scale = 2)
	private BigDecimal annualPremium;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public Driver getDriver() {
		return this.driver;
	}

	public void setDriver(Driver driver) {
		this.driver = driver;
	}

	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public LiabilityLimit getLiabilityLimit() {
		return this.liabilityLimit;
	}

	public void setLiabilityLimit(LiabilityLimit liabilityLimit) {
		this.liabilityLimit = liabilityLimit;
	}

	public Deductible getDeductible() {
		return this.deductible;
	}

	public void setDeductible(Deductible deductible) {
		this.deductible = deductible;
	}

	public BigDecimal getBaseRate() {
		return this.baseRate;
	}

	public void setBaseRate(BigDecimal baseRate) {
		this.baseRate = baseRate;
	}

	public BigDecimal getAgeFactor() {
		return this.ageFactor;
	}

	public void setAgeFactor(BigDecimal ageFactor) {
		this.ageFactor = ageFactor;
	}

	public BigDecimal getVehicleAgeFactor() {
		return this.vehicleAgeFactor;
	}

	public void setVehicleAgeFactor(BigDecimal vehicleAgeFactor) {
		this.vehicleAgeFactor = vehicleAgeFactor;
	}

	public BigDecimal getUsageFactor() {
		return this.usageFactor;
	}

	public void setUsageFactor(BigDecimal usageFactor) {
		this.usageFactor = usageFactor;
	}

	public BigDecimal getCoverageFactor() {
		return this.coverageFactor;
	}

	public void setCoverageFactor(BigDecimal coverageFactor) {
		this.coverageFactor = coverageFactor;
	}

	public BigDecimal getAnnualPremium() {
		return this.annualPremium;
	}

	public void setAnnualPremium(BigDecimal annualPremium) {
		this.annualPremium = annualPremium;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}

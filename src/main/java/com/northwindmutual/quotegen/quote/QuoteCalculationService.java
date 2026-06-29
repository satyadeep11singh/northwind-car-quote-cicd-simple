package com.northwindmutual.quotegen.quote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

import org.springframework.stereotype.Service;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.vehicle.Vehicle;
import com.northwindmutual.quotegen.vehicle.VehicleUsageType;

/**
 * Calculates a car insurance premium using a simple, transparent rating
 * formula:
 *
 * <pre>
 * annualPremium = baseRate
 *               x ageFactor
 *               x vehicleAgeFactor
 *               x usageFactor
 *               x liabilityLimit.ratingFactor
 *               x deductible.ratingFactor
 * </pre>
 *
 * <p>This is intentionally illustrative rather than actuarially accurate: the
 * goal is a calculation a reader can follow end-to-end from a {@link Driver}
 * and {@link Vehicle}'s details to a final number, with every intermediate
 * factor stored on the resulting {@link Quote} so the UI can show a full
 * breakdown rather than a single opaque figure.
 */
@Service
public class QuoteCalculationService {

	/** Flat starting premium, in dollars per year, before any rating factors are applied. */
	static final BigDecimal BASE_RATE = new BigDecimal("800.00");

	private static final int CURRENT_YEAR = LocalDate.now().getYear();

	/**
	 * Build a fully-priced {@link Quote} for the given driver, vehicle, and
	 * chosen coverage. The returned {@link Quote} is not yet persisted.
	 * @param driver the driver requesting the quote
	 * @param vehicle the vehicle to be insured
	 * @param liabilityLimit the chosen liability coverage tier
	 * @param deductible the chosen deductible tier
	 * @return a priced, not-yet-saved {@link Quote}
	 */
	public Quote calculate(Driver driver, Vehicle vehicle, LiabilityLimit liabilityLimit, Deductible deductible) {
		BigDecimal ageFactor = calculateAgeFactor(driver);
		BigDecimal vehicleAgeFactor = calculateVehicleAgeFactor(vehicle);
		BigDecimal usageFactor = calculateUsageFactor(vehicle.getUsageType());
		BigDecimal coverageFactor = calculateCoverageFactor(liabilityLimit, deductible);

		BigDecimal annualPremium = BASE_RATE.multiply(ageFactor)
			.multiply(vehicleAgeFactor)
			.multiply(usageFactor)
			.multiply(coverageFactor)
			.setScale(2, RoundingMode.HALF_UP);

		Quote quote = new Quote();
		quote.setDriver(driver);
		quote.setVehicle(vehicle);
		quote.setLiabilityLimit(liabilityLimit);
		quote.setDeductible(deductible);
		quote.setBaseRate(BASE_RATE);
		quote.setAgeFactor(ageFactor);
		quote.setVehicleAgeFactor(vehicleAgeFactor);
		quote.setUsageFactor(usageFactor);
		quote.setCoverageFactor(coverageFactor);
		quote.setAnnualPremium(annualPremium);
		return quote;
	}

	/**
	 * Younger and less experienced drivers carry a higher rating factor.
	 * Drivers under 25 with fewer than 3 years of licensed experience are
	 * rated highest; drivers 25+ with 3+ years of experience get the
	 * baseline factor of 1.0.
	 */
	BigDecimal calculateAgeFactor(Driver driver) {
		int age = Period.between(driver.getDateOfBirth(), LocalDate.now()).getYears();
		int yearsHeld = driver.getLicenseYearsHeld();

		if (age < 25 && yearsHeld < 3) {
			return new BigDecimal("1.75");
		}
		if (age < 25) {
			return new BigDecimal("1.40");
		}
		if (yearsHeld < 3) {
			return new BigDecimal("1.25");
		}
		return BigDecimal.ONE;
	}

	/**
	 * Newer vehicles are more expensive to repair or replace, so they carry
	 * a higher rating factor. The factor decreases as the vehicle ages,
	 * with a floor at 0.80 for vehicles 10+ years old.
	 */
	BigDecimal calculateVehicleAgeFactor(Vehicle vehicle) {
		int vehicleAge = CURRENT_YEAR - vehicle.getModelYear();

		if (vehicleAge <= 1) {
			return new BigDecimal("1.30");
		}
		if (vehicleAge <= 4) {
			return new BigDecimal("1.10");
		}
		if (vehicleAge <= 9) {
			return BigDecimal.ONE;
		}
		return new BigDecimal("0.80");
	}

	/**
	 * Business use carries the most road exposure, pleasure use the least.
	 */
	BigDecimal calculateUsageFactor(VehicleUsageType usageType) {
		return switch (usageType) {
			case BUSINESS -> new BigDecimal("1.35");
			case COMMUTE -> new BigDecimal("1.15");
			case PLEASURE -> BigDecimal.ONE;
		};
	}

	/**
	 * Combines the two coverage-tier rating factors (each already defined
	 * on the {@link LiabilityLimit} and {@link Deductible} enums) into a
	 * single coverage factor.
	 */
	BigDecimal calculateCoverageFactor(LiabilityLimit liabilityLimit, Deductible deductible) {
		return BigDecimal.valueOf(liabilityLimit.getRatingFactor()).multiply(BigDecimal.valueOf(deductible.getRatingFactor()));
	}

}

package com.northwindmutual.quotegen.quote;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.vehicle.Vehicle;
import com.northwindmutual.quotegen.vehicle.VehicleUsageType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link QuoteCalculationService}. These tests exercise the
 * rating formula directly, without a Spring context, since the calculation
 * itself has no framework dependencies.
 */
class QuoteCalculationServiceTests {

	private final QuoteCalculationService service = new QuoteCalculationService();

	private Driver driverAged(int age, int licenseYearsHeld) {
		Driver driver = new Driver();
		driver.setFirstName("Test");
		driver.setLastName("Driver");
		driver.setDateOfBirth(LocalDate.now().minusYears(age));
		driver.setLicenseYearsHeld(licenseYearsHeld);
		return driver;
	}

	private Vehicle vehicleOfAge(int vehicleAge, VehicleUsageType usageType) {
		Vehicle vehicle = new Vehicle();
		vehicle.setMake("Toyota");
		vehicle.setModel("Corolla");
		vehicle.setModelYear(LocalDate.now().getYear() - vehicleAge);
		vehicle.setUsageType(usageType);
		vehicle.setAnnualMileage(15000);
		return vehicle;
	}

	@Test
	void baselineDriverAndVehicleProducesBaseRateOnly() {
		// 30 years old, 10 years licensed, 7-year-old car, pleasure use,
		// cheapest coverage tier - every factor should be 1.0.
		Driver driver = driverAged(30, 10);
		Vehicle vehicle = vehicleOfAge(7, VehicleUsageType.PLEASURE);

		Quote quote = service.calculate(driver, vehicle, LiabilityLimit.ONE_MILLION, Deductible.ONE_THOUSAND);

		assertThat(quote.getAgeFactor()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(quote.getVehicleAgeFactor()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(quote.getUsageFactor()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(quote.getCoverageFactor()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(quote.getAnnualPremium()).isEqualByComparingTo(QuoteCalculationService.BASE_RATE);
	}

	@Test
	void youngInexperiencedDriverGetsHighestAgeFactor() {
		Driver driver = driverAged(20, 1);

		BigDecimal factor = service.calculateAgeFactor(driver);

		assertThat(factor).isEqualByComparingTo(new BigDecimal("1.75"));
	}

	@Test
	void youngExperiencedDriverGetsModerateAgeFactor() {
		// Started driving young enough to have 3+ years of experience before 25.
		Driver driver = driverAged(24, 5);

		BigDecimal factor = service.calculateAgeFactor(driver);

		assertThat(factor).isEqualByComparingTo(new BigDecimal("1.40"));
	}

	@Test
	void olderButNewlyLicensedDriverGetsInexperienceFactor() {
		Driver driver = driverAged(40, 1);

		BigDecimal factor = service.calculateAgeFactor(driver);

		assertThat(factor).isEqualByComparingTo(new BigDecimal("1.25"));
	}

	@Test
	void brandNewVehicleGetsHighestVehicleAgeFactor() {
		Vehicle vehicle = vehicleOfAge(0, VehicleUsageType.PLEASURE);

		BigDecimal factor = service.calculateVehicleAgeFactor(vehicle);

		assertThat(factor).isEqualByComparingTo(new BigDecimal("1.30"));
	}

	@Test
	void oldVehicleGetsDiscountFactor() {
		Vehicle vehicle = vehicleOfAge(12, VehicleUsageType.PLEASURE);

		BigDecimal factor = service.calculateVehicleAgeFactor(vehicle);

		assertThat(factor).isEqualByComparingTo(new BigDecimal("0.80"));
	}

	@Test
	void businessUseCarriesHighestUsageFactor() {
		assertThat(service.calculateUsageFactor(VehicleUsageType.BUSINESS)).isEqualByComparingTo(new BigDecimal("1.35"));
		assertThat(service.calculateUsageFactor(VehicleUsageType.COMMUTE)).isEqualByComparingTo(new BigDecimal("1.15"));
		assertThat(service.calculateUsageFactor(VehicleUsageType.PLEASURE)).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void higherLiabilityAndLowerDeductibleIncreaseCoverageFactor() {
		BigDecimal cheapest = service.calculateCoverageFactor(LiabilityLimit.ONE_MILLION, Deductible.ONE_THOUSAND);
		BigDecimal mostExpensive = service.calculateCoverageFactor(LiabilityLimit.TWO_MILLION, Deductible.FIVE_HUNDRED);

		assertThat(cheapest).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(mostExpensive).isGreaterThan(cheapest);
	}

	@Test
	void worstCaseRiskProfileProducesHighestPremium() {
		// 19 years old, 0 years licensed, brand-new car, business use, max coverage.
		Driver driver = driverAged(19, 0);
		Vehicle vehicle = vehicleOfAge(0, VehicleUsageType.BUSINESS);

		Quote quote = service.calculate(driver, vehicle, LiabilityLimit.TWO_MILLION, Deductible.FIVE_HUNDRED);

		// Sanity bound: premium should be well above the base rate given every
		// factor is at its riskiest setting.
		assertThat(quote.getAnnualPremium()).isGreaterThan(QuoteCalculationService.BASE_RATE.multiply(new BigDecimal("2")));
	}

	@Test
	void quoteRetainsTheDriverVehicleAndCoverageChoicesItWasCalculatedFrom() {
		Driver driver = driverAged(30, 10);
		Vehicle vehicle = vehicleOfAge(3, VehicleUsageType.COMMUTE);

		Quote quote = service.calculate(driver, vehicle, LiabilityLimit.TWO_MILLION, Deductible.FIVE_HUNDRED);

		assertThat(quote.getDriver()).isSameAs(driver);
		assertThat(quote.getVehicle()).isSameAs(vehicle);
		assertThat(quote.getLiabilityLimit()).isEqualTo(LiabilityLimit.TWO_MILLION);
		assertThat(quote.getDeductible()).isEqualTo(Deductible.FIVE_HUNDRED);
	}

}

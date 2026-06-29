package com.northwindmutual.quotegen.vehicle;

/**
 * How the vehicle is primarily used. Drives the usage rating factor in
 * {@link com.northwindmutual.quotegen.quote.QuoteCalculationService}.
 *
 * <p>Kept as a simple Java enum rather than a database-backed lookup table
 * (the approach Spring PetClinic takes for {@code PetType}) since the set of
 * usage types is small, fixed, and unlikely to need editing at runtime.
 */
public enum VehicleUsageType {

	COMMUTE("Commute to work/school"), PLEASURE("Pleasure / personal use only"), BUSINESS("Business use");

	private final String displayName;

	VehicleUsageType(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}

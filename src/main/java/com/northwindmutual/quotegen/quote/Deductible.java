package com.northwindmutual.quotegen.quote;

/**
 * Fixed collision/comprehensive deductible tiers offered by Northwind Mutual.
 * A higher deductible means the driver pays more out-of-pocket per claim, so
 * it earns a lower (cheaper) rating factor. Drives the coverage rating factor
 * in {@link QuoteCalculationService}.
 */
public enum Deductible {

	FIVE_HUNDRED("$500", 1.10), ONE_THOUSAND("$1,000", 1.00);

	private final String displayName;

	private final double ratingFactor;

	Deductible(String displayName, double ratingFactor) {
		this.displayName = displayName;
		this.ratingFactor = ratingFactor;
	}

	public String getDisplayName() {
		return displayName;
	}

	public double getRatingFactor() {
		return ratingFactor;
	}

}

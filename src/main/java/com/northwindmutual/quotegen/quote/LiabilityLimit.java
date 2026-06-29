package com.northwindmutual.quotegen.quote;

/**
 * Fixed liability coverage tiers offered by Northwind Mutual. Drives the
 * coverage rating factor in {@link QuoteCalculationService}.
 */
public enum LiabilityLimit {

	ONE_MILLION("$1,000,000", 1.00), TWO_MILLION("$2,000,000", 1.15);

	private final String displayName;

	private final double ratingFactor;

	LiabilityLimit(String displayName, double ratingFactor) {
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

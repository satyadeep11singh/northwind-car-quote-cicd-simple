package com.northwindmutual.quotegen.quote;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.driver.DriverRepository;
import com.northwindmutual.quotegen.vehicle.Vehicle;

import jakarta.persistence.EntityNotFoundException;

/**
 * Drives the quote-generation workflow: choose one of a driver's vehicles,
 * choose coverage, calculate a premium via {@link QuoteCalculationService},
 * and show the resulting breakdown. Also exposes the driver's quote history.
 *
 * <p>This controller has no direct equivalent in Spring PetClinic - it is
 * the one genuinely new piece of business logic this project adds on top of
 * the PetClinic-derived scaffolding.
 */
@Controller
@RequestMapping("/drivers/{driverId}")
class QuoteController {

	private final DriverRepository drivers;

	private final QuoteRepository quotes;

	private final QuoteCalculationService quoteCalculationService;

	public QuoteController(DriverRepository drivers, QuoteRepository quotes,
			QuoteCalculationService quoteCalculationService) {
		this.drivers = drivers;
		this.quotes = quotes;
		this.quoteCalculationService = quoteCalculationService;
	}

	@ModelAttribute("liabilityLimits")
	public LiabilityLimit[] populateLiabilityLimits() {
		return LiabilityLimit.values();
	}

	@ModelAttribute("deductibles")
	public Deductible[] populateDeductibles() {
		return Deductible.values();
	}

	@ModelAttribute("driver")
	public Driver findDriver(@PathVariable("driverId") int driverId) {
		Optional<Driver> optionalDriver = this.drivers.findById(driverId);
		return optionalDriver.orElseThrow(
				() -> new IllegalArgumentException("Driver not found with id: " + driverId + ". Please ensure the ID is correct"));
	}

	/**
	 * Shows the quote-request form, letting the driver pick which of their
	 * vehicles to insure and which coverage tiers to apply.
	 */
	@GetMapping("/quotes/new")
	public String initQuoteForm(Driver driver, ModelMap model) {
		if (driver.getVehicles().isEmpty()) {
			model.addAttribute("noVehiclesMessage",
					"This driver has no vehicles on file yet. Add a vehicle before requesting a quote.");
		}
		return "quotes/requestQuoteForm";
	}

	/**
	 * Calculates and persists a quote, then redirects to the result page.
	 */
	@PostMapping("/quotes/new")
	public String processQuoteForm(Driver driver, @RequestParam("vehicleId") Integer vehicleId,
			@RequestParam("liabilityLimit") LiabilityLimit liabilityLimit,
			@RequestParam("deductible") Deductible deductible, RedirectAttributes redirectAttributes) {

		Vehicle vehicle = driver.getVehicle(vehicleId);
		if (vehicle == null) {
			throw new EntityNotFoundException(
					"Vehicle not found with id: " + vehicleId + " for driver " + driver.getId());
		}

		Quote quote = this.quoteCalculationService.calculate(driver, vehicle, liabilityLimit, deductible);
		quote.setCreatedAt(LocalDateTime.now());
		this.quotes.save(quote);

		redirectAttributes.addFlashAttribute("message", "Quote generated");
		return "redirect:/drivers/{driverId}/quotes/" + quote.getId();
	}

	/**
	 * Shows a single quote's result, including the full rating breakdown.
	 */
	@GetMapping("/quotes/{quoteId}")
	public String showQuote(@PathVariable("quoteId") int quoteId, ModelMap model) {
		Quote quote = this.quotes.findById(quoteId)
			.orElseThrow(() -> new IllegalArgumentException("Quote not found with id: " + quoteId));
		model.addAttribute("quote", quote);
		return "quotes/quoteResult";
	}

	/**
	 * Lists every quote previously generated for this driver, most recent first.
	 */
	@GetMapping("/quotes")
	public String listQuotes(Driver driver, ModelMap model) {
		List<Quote> quoteHistory = this.quotes.findByDriverIdOrderByCreatedAtDesc(driver.getId());
		model.addAttribute("quoteHistory", quoteHistory);
		return "quotes/quoteHistory";
	}

}

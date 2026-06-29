package com.northwindmutual.quotegen.driver;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Handles search, creation, viewing, and editing of {@link Driver} records -
 * the entry point into the quote workflow.
 */
@Controller
class DriverController {

	private static final String VIEWS_DRIVER_CREATE_OR_UPDATE_FORM = "drivers/createOrUpdateDriverForm";

	private final DriverRepository drivers;

	public DriverController(DriverRepository drivers) {
		this.drivers = drivers;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	@ModelAttribute("driver")
	public Driver findDriver(@PathVariable(name = "driverId", required = false) Integer driverId) {
		return driverId == null ? new Driver()
				: this.drivers.findById(driverId)
					.orElseThrow(() -> new IllegalArgumentException("Driver not found with id: " + driverId
							+ ". Please ensure the ID is correct and the driver exists."));
	}

	@GetMapping("/drivers/new")
	public String initCreationForm() {
		return VIEWS_DRIVER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/drivers/new")
	public String processCreationForm(@Valid Driver driver, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error creating the driver profile.");
			return VIEWS_DRIVER_CREATE_OR_UPDATE_FORM;
		}

		this.drivers.save(driver);
		redirectAttributes.addFlashAttribute("message", "New driver profile created");
		return "redirect:/drivers/" + driver.getId();
	}

	@GetMapping("/drivers/find")
	public String initFindForm() {
		return "drivers/findDrivers";
	}

	@GetMapping("/drivers")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Driver driver, BindingResult result,
			Model model) {
		String lastName = driver.getLastName();
		if (lastName == null) {
			lastName = "";
		}

		Page<Driver> driverResults = findPaginatedForDriversLastName(page, lastName);
		if (driverResults.isEmpty()) {
			result.rejectValue("lastName", "notFound", "not found");
			return "drivers/findDrivers";
		}

		if (driverResults.getTotalElements() == 1) {
			driver = driverResults.iterator().next();
			return "redirect:/drivers/" + driver.getId();
		}

		return addPaginationModel(page, model, driverResults);
	}

	private String addPaginationModel(int page, Model model, Page<Driver> paginated) {
		List<Driver> listDrivers = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listDrivers", listDrivers);
		return "drivers/driversList";
	}

	private Page<Driver> findPaginatedForDriversLastName(int page, String lastname) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return drivers.findByLastNameStartingWith(lastname, pageable);
	}

	@GetMapping("/drivers/{driverId}/edit")
	public String initUpdateDriverForm() {
		return VIEWS_DRIVER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/drivers/{driverId}/edit")
	public String processUpdateDriverForm(@Valid Driver driver, BindingResult result,
			@PathVariable("driverId") int driverId, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error updating the driver profile.");
			return VIEWS_DRIVER_CREATE_OR_UPDATE_FORM;
		}

		if (!Objects.equals(driver.getId(), driverId)) {
			result.rejectValue("id", "mismatch", "The driver ID in the form does not match the URL.");
			redirectAttributes.addFlashAttribute("error", "Driver ID mismatch. Please try again.");
			return "redirect:/drivers/{driverId}/edit";
		}

		driver.setId(driverId);
		this.drivers.save(driver);
		redirectAttributes.addFlashAttribute("message", "Driver profile updated");
		return "redirect:/drivers/{driverId}";
	}

	/**
	 * Displays a driver's profile, including their vehicles and quote history.
	 * @param driverId the id of the driver to display
	 * @return a ModelAndView with the model attributes for the view
	 */
	@GetMapping("/drivers/{driverId}")
	public ModelAndView showDriver(@PathVariable("driverId") int driverId) {
		ModelAndView mav = new ModelAndView("drivers/driverDetails");
		Optional<Driver> optionalDriver = this.drivers.findById(driverId);
		Driver driver = optionalDriver.orElseThrow(
				() -> new IllegalArgumentException("Driver not found with id: " + driverId + ". Please ensure the ID is correct"));
		mav.addObject(driver);
		return mav;
	}

}

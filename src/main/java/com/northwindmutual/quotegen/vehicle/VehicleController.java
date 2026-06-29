package com.northwindmutual.quotegen.vehicle;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.northwindmutual.quotegen.driver.Driver;
import com.northwindmutual.quotegen.driver.DriverRepository;

import jakarta.validation.Valid;

/**
 * Handles creation and editing of {@link Vehicle} records, nested under a
 * specific {@link Driver}.
 */
@Controller
@RequestMapping("/drivers/{driverId}")
class VehicleController {

	private static final String VIEWS_VEHICLE_CREATE_OR_UPDATE_FORM = "vehicles/createOrUpdateVehicleForm";

	private final DriverRepository drivers;

	public VehicleController(DriverRepository drivers) {
		this.drivers = drivers;
	}

	@ModelAttribute("usageTypes")
	public VehicleUsageType[] populateUsageTypes() {
		return VehicleUsageType.values();
	}

	@ModelAttribute("driver")
	public Driver findDriver(@PathVariable("driverId") int driverId) {
		Optional<Driver> optionalDriver = this.drivers.findById(driverId);
		return optionalDriver.orElseThrow(
				() -> new IllegalArgumentException("Driver not found with id: " + driverId + ". Please ensure the ID is correct"));
	}

	@ModelAttribute("vehicle")
	public Vehicle findVehicle(@PathVariable("driverId") int driverId,
			@PathVariable(name = "vehicleId", required = false) Integer vehicleId) {
		if (vehicleId == null) {
			return new Vehicle();
		}

		Driver driver = findDriver(driverId);
		return driver.getVehicle(vehicleId);
	}

	@InitBinder("driver")
	public void initDriverBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id");
	}

	@InitBinder("vehicle")
	public void initVehicleBinder(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id", "*.id", "driver");
	}

	@GetMapping("/vehicles/new")
	public String initCreationForm(Driver driver, ModelMap model) {
		Vehicle vehicle = new Vehicle();
		driver.addVehicle(vehicle);
		return VIEWS_VEHICLE_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/vehicles/new")
	public String processCreationForm(Driver driver, @Valid Vehicle vehicle, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return VIEWS_VEHICLE_CREATE_OR_UPDATE_FORM;
		}

		driver.addVehicle(vehicle);
		this.drivers.save(driver);
		redirectAttributes.addFlashAttribute("message", "New vehicle has been added");
		return "redirect:/drivers/{driverId}";
	}

	@GetMapping("/vehicles/{vehicleId}/edit")
	public String initUpdateForm() {
		return VIEWS_VEHICLE_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/vehicles/{vehicleId}/edit")
	public String processUpdateForm(Driver driver, @Valid Vehicle vehicle, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return VIEWS_VEHICLE_CREATE_OR_UPDATE_FORM;
		}

		updateVehicleDetails(driver, vehicle);
		redirectAttributes.addFlashAttribute("message", "Vehicle details have been updated");
		return "redirect:/drivers/{driverId}";
	}

	private void updateVehicleDetails(Driver driver, Vehicle vehicle) {
		Vehicle existingVehicle = driver.getVehicle(vehicle.getId());
		if (existingVehicle != null) {
			existingVehicle.setMake(vehicle.getMake());
			existingVehicle.setModel(vehicle.getModel());
			existingVehicle.setModelYear(vehicle.getModelYear());
			existingVehicle.setUsageType(vehicle.getUsageType());
			existingVehicle.setAnnualMileage(vehicle.getAnnualMileage());
		}
		else {
			driver.addVehicle(vehicle);
		}
		this.drivers.save(driver);
	}

}

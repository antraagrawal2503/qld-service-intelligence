package com.antra.qldserviceintelligence.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentPressureDto;
import com.antra.qldserviceintelligence.service.EmergencyDepartmentPressureService;

@RestController
public class EmergencyDepartmentPressureController {

	private final EmergencyDepartmentPressureService service;

	public EmergencyDepartmentPressureController(EmergencyDepartmentPressureService service) {
		this.service = service;
	}

	@GetMapping("/api/v1/emergency-departments/pressure")
	public List<EmergencyDepartmentPressureDto> getPressure() throws IOException {
		return service.getPressure();
	}

	@GetMapping("/api/v1/emergency-departments/pressure/highest")
	public List<EmergencyDepartmentPressureDto> getHighestPressure() throws IOException {
		return service.getHighestPressure();
	}
}

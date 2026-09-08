package com.antra.qldserviceintelligence.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;
import com.antra.qldserviceintelligence.service.EmergencyDepartmentService;

@RestController
public class EmergencyDepartmentController {

	private final EmergencyDepartmentService emergencyDepartmentService;

	public EmergencyDepartmentController(EmergencyDepartmentService emergencyDepartmentService) {
		this.emergencyDepartmentService = emergencyDepartmentService;
	}

	@GetMapping("/api/v1/emergency-departments")
	public List<EmergencyDepartmentDto> getEmergencyDepartments() throws IOException {
		return emergencyDepartmentService.getEmergencyDepartments();
	}
}

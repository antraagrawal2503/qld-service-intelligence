package com.antra.qldserviceintelligence.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

	@GetMapping("/api/v1/status")
	public Map<String, String> getStatus() {
		return Map.of(
				"service", "QLD Service Intelligence",
				"status", "operational");
	}
}
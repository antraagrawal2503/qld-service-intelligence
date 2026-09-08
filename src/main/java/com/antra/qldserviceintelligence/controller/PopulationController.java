package com.antra.qldserviceintelligence.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.antra.qldserviceintelligence.model.PopulationDto;
import com.antra.qldserviceintelligence.service.PopulationService;

@RestController
public class PopulationController {

	private final PopulationService populationService;

	public PopulationController(PopulationService populationService) {
		this.populationService = populationService;
	}

	@GetMapping("/api/v1/population")
	public List<PopulationDto> getPopulation() throws IOException {
		return populationService.getPopulation();
	}
}
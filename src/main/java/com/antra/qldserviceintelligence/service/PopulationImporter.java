package com.antra.qldserviceintelligence.service;

import java.io.IOException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.antra.qldserviceintelligence.model.PopulationRecord;
import com.antra.qldserviceintelligence.repository.PopulationRepository;

@Component
public class PopulationImporter implements ApplicationRunner {

	private final PopulationService populationService;
	private final PopulationRepository repository;

	public PopulationImporter(PopulationService populationService, PopulationRepository repository) {
		this.populationService = populationService;
		this.repository = repository;
	}

	@Override
	@Transactional(rollbackFor = IOException.class)
	public void run(ApplicationArguments args) throws IOException {
		// The parser strips surrounding whitespace; the remaining source LGA name is the key.
		for (var row : populationService.readPopulationForImport()) {
			PopulationRecord record = repository.findByLga(row.lga())
					.orElseGet(() -> new PopulationRecord(row.lga(), row.population2020(), row.population2025()));
			record.updatePopulation(row.population2020(), row.population2025());
			repository.save(record);
		}
	}
}

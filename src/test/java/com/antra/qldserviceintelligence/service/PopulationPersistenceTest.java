package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import com.antra.qldserviceintelligence.model.PopulationRecord;
import com.antra.qldserviceintelligence.repository.PopulationRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PopulationPersistenceTest {

	@Autowired PopulationRepository repository;
	@Autowired PopulationImporter importer;
	@Autowired PopulationService service;
	@Autowired EntityManager entityManager;
	@Autowired WebApplicationContext context;
	@Autowired ObjectMapper objectMapper;
	@TempDir Path directory;

	@Test
	void persistsAllFieldsAndFindsByLga() {
		PopulationRecord saved = repository.saveAndFlush(new PopulationRecord("Test LGA", 1200, 1301));
		entityManager.clear();

		PopulationRecord loaded = repository.findByLga("Test LGA").orElseThrow();
		assertThat(loaded.getId()).isNotNull().isEqualTo(saved.getId());
		assertThat(loaded.getLga()).isEqualTo("Test LGA");
		assertThat(loaded.getPopulation2020()).isEqualTo(1200);
		assertThat(loaded.getPopulation2025()).isEqualTo(1301);
		assertThat(repository.findByLga("Unknown LGA")).isEmpty();
	}

	@Test
	void databaseRejectsDuplicateLga() {
		repository.saveAndFlush(new PopulationRecord("Duplicate LGA", 100, 110));
		assertThatThrownBy(() -> repository.saveAndFlush(new PopulationRecord("Duplicate LGA", 200, 220)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void startupImportsDatasetAndRepeatedImportsPreserveIdsAndValues() throws Exception {
		assertThat(repository.count()).isEqualTo(78);
		Map<String, Long> ids = repository.findAll().stream()
				.collect(Collectors.toMap(PopulationRecord::getLga, PopulationRecord::getId));
		importer.run(new DefaultApplicationArguments());
		importer.run(new DefaultApplicationArguments());
		repository.flush();
		entityManager.clear();

		assertThat(repository.count()).isEqualTo(78);
		for (var row : service.getPopulationGrowth()) {
			PopulationRecord saved = repository.findByLga(row.lga()).orElseThrow();
			assertThat(saved.getId()).isEqualTo(ids.get(row.lga()));
			assertThat(saved.getPopulation2020()).isEqualTo(row.population2020());
			assertThat(saved.getPopulation2025()).isEqualTo(row.population2025());
		}
		assertThat(repository.findByLga("Queensland")).isEmpty();
	}

	@Test
	void importUpdatesExistingLgaAndHandlesDuplicateCsvRows() throws Exception {
		Path csv = directory.resolve("population.csv");
		Files.writeString(csv, "LGA,2020,2025p\n  Import LGA  ,100,110\nImport LGA,100,120\n");
		PopulationImporter fixtureImporter = new PopulationImporter(new PopulationService(csv), repository);
		long before = repository.count();
		fixtureImporter.run(new DefaultApplicationArguments());
		Long id = repository.findByLga("Import LGA").orElseThrow().getId();
		Files.writeString(csv, "LGA,2020,2025p\nImport LGA,105,130\n");
		fixtureImporter.run(new DefaultApplicationArguments());
		repository.flush();
		entityManager.clear();

		assertThat(repository.count()).isEqualTo(before + 1);
		PopulationRecord saved = repository.findByLga("Import LGA").orElseThrow();
		assertThat(saved.getId()).isEqualTo(id);
		assertThat(saved.getPopulation2020()).isEqualTo(105);
		assertThat(saved.getPopulation2025()).isEqualTo(130);
	}

	@Test
	void allPopulationEndpointsPreserveResponsesAfterStartupImport() throws Exception {
		var mvc = MockMvcBuilders.webAppContextSetup(context).build();
		Map<String, Object> responses = Map.of(
				"", service.getPopulation(),
				"/growth", service.getPopulationGrowth(),
				"/growth/fastest", service.getFastestPopulationGrowth(),
				"/growth/declining", service.getDecliningPopulationGrowth());
		for (var response : responses.entrySet()) {
			mvc.perform(get("/api/v1/population" + response.getKey()))
					.andExpect(status().isOk())
					.andExpect(content().json(objectMapper.writeValueAsString(response.getValue())));
		}
	}
}

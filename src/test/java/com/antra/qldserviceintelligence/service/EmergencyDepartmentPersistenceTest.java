package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
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

import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentRecord;
import com.antra.qldserviceintelligence.repository.EmergencyDepartmentRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EmergencyDepartmentPersistenceTest {

	@Autowired EmergencyDepartmentRepository repository;
	@Autowired EmergencyDepartmentImporter importer;
	@Autowired EmergencyDepartmentService service;
	@Autowired EntityManager entityManager;
	@Autowired WebApplicationContext context;
	@Autowired ObjectMapper objectMapper;
	@TempDir Path directory;

	@Test
	void persistsEveryFieldAndLooksUpByFacilityAndQuarter() throws Exception {
		var row = fixtureService().getEmergencyDepartments().getFirst();
		var saved = repository.saveAndFlush(new EmergencyDepartmentRecord(row));
		entityManager.clear();
		var loaded = repository.findByFacilityCodeAndQuarter("001", "Sep-25").orElseThrow();
		assertThat(loaded.getId()).isNotNull().isEqualTo(saved.getId());
		assertMatches(loaded, row);
		assertThat(repository.findByFacilityCodeAndQuarter("001", "Dec-25")).isEmpty();
		assertThat(repository.findByFacilityCodeAndQuarter("missing", "Sep-25")).isEmpty();
	}

	@Test
	void importsOnlyAllRowsAndPreservesNullableNumbers() throws Exception {
		long before = repository.count();
		var fixture = fixtureService();
		new EmergencyDepartmentImporter(fixture, repository).run(new DefaultApplicationArguments());
		repository.flush();
		entityManager.clear();
		assertThat(repository.count()).isEqualTo(before + 3);
		for (var row : fixture.getEmergencyDepartments()) {
			assertMatches(repository.findByFacilityCodeAndQuarter(row.facilityCode(), row.quarter()).orElseThrow(), row);
		}
	}

	@Test
	void startupImports104RealFacilitiesAndRepeatsWithoutDuplicates() throws Exception {
		assertThat(repository.count()).isEqualTo(104);
		var ids = repository.findAll().stream().collect(Collectors.toMap(
				row -> row.getFacilityCode() + ":" + row.getQuarter(), EmergencyDepartmentRecord::getId));
		importer.run(new DefaultApplicationArguments());
		importer.run(new DefaultApplicationArguments());
		repository.flush();
		entityManager.clear();
		assertThat(repository.count()).isEqualTo(104);
		assertThat(repository.findAll()).noneMatch(row ->
				row.getFacilityCode().equals("99999") || row.getFacilityName().equals("Queensland"));
		for (var row : service.getEmergencyDepartments()) {
			var saved = repository.findByFacilityCodeAndQuarter(row.facilityCode(), row.quarter()).orElseThrow();
			assertThat(saved.getId()).isEqualTo(ids.get(row.facilityCode() + ":" + row.quarter()));
			assertMatches(saved, row);
		}
	}

	@Test
	void repeatImportUpdatesExistingRecordIncludingNameAndNullValues() throws Exception {
		Path csv = directory.resolve("ed.csv");
		String original = Files.readString(Path.of("src/test/resources/emergency-departments.csv"));
		Files.writeString(csv, original);
		var fixtureImporter = new EmergencyDepartmentImporter(new EmergencyDepartmentService(csv), repository);
		fixtureImporter.run(new DefaultApplicationArguments());
		var id = repository.findByFacilityCodeAndQuarter("001", "Sep-25").orElseThrow().getId();
		long count = repository.count();
		Files.writeString(csv, original.replace("Royal Brisbane & Women's", "Renamed facility")
				.replace("\"13,130\",-2.5", "-,1.25"));
		fixtureImporter.run(new DefaultApplicationArguments());
		repository.flush();
		entityManager.clear();
		var updated = repository.findByFacilityCodeAndQuarter("001", "Sep-25").orElseThrow();
		assertThat(repository.count()).isEqualTo(count);
		assertThat(updated.getId()).isEqualTo(id);
		assertThat(updated.getFacilityName()).isEqualTo("Renamed facility");
		assertThat(updated.getNumberOfAttendances()).isNull();
		assertThat(updated.getAttendanceVariationPercent()).isEqualByComparingTo("1.25");
	}

	@Test
	void uniquenessUsesCodeAndQuarterRatherThanFacilityName() {
		repository.saveAndFlush(new EmergencyDepartmentRecord(emptyRow("test-1", "Sep-25")));
		repository.saveAndFlush(new EmergencyDepartmentRecord(emptyRow("test-1", "Dec-25")));
		repository.saveAndFlush(new EmergencyDepartmentRecord(emptyRow("test-2", "Sep-25")));
		assertThatThrownBy(() -> repository.saveAndFlush(
				new EmergencyDepartmentRecord(emptyRow("test-1", "Sep-25"))))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void endpointRemainsCsvBackedWithUnchangedResponse() throws Exception {
		repository.deleteAllInBatch();
		var mvc = MockMvcBuilders.webAppContextSetup(context).build();
		mvc.perform(get("/api/v1/emergency-departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(104))
				.andExpect(content().json(objectMapper.writeValueAsString(service.getEmergencyDepartments())));
	}

	private EmergencyDepartmentService fixtureService() {
		return new EmergencyDepartmentService(Path.of("src/test/resources/emergency-departments.csv"));
	}

	private EmergencyDepartmentDto emptyRow(String code, String quarter) {
		return new EmergencyDepartmentDto(code, "Same name", quarter,
				null, null, null, null, null, null, null, null, null, null, null);
	}

	private void assertMatches(EmergencyDepartmentRecord saved, EmergencyDepartmentDto row) {
		assertThat(saved).usingRecursiveComparison().ignoringFields("id")
				.withComparatorForType(BigDecimal::compareTo, BigDecimal.class).isEqualTo(row);
	}
}

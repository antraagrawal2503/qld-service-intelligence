package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;

class EmergencyDepartmentServiceTest {

	@Test
	void selectsExactAllRowsAndParsesEveryMetricWhilePreservingNames() throws Exception {
		List<EmergencyDepartmentDto> rows = fixtureService().getEmergencyDepartments();

		assertThat(rows).hasSize(3);
		assertThat(rows.getFirst()).isEqualTo(new EmergencyDepartmentDto(
				"001", "Royal Brisbane & Women's", "Sep-25", 13130,
				new BigDecimal("-2.5"), new BigDecimal("65.5"), 21,
				new BigDecimal("3.9"), new BigDecimal("34.1"), new BigDecimal("24.2"),
				new BigDecimal("42"), 498, new BigDecimal("3.8"), 506));
	}

	@Test
	void blankAndUnavailableNumericValuesBecomeNull() throws Exception {
		List<EmergencyDepartmentDto> rows = fixtureService().getEmergencyDepartments();

		assertThat(rows.get(1)).isEqualTo(new EmergencyDepartmentDto(
				"002", "Facility  Two", "Sep-25", null, null, null, null, null, null, null, null, null, null, null));
		assertThat(rows.get(2)).isEqualTo(new EmergencyDepartmentDto(
				"003", "Unavailable", "Sep-25", null, null, null, null, null, null, null, null, null, null, null));
	}

	@Test
	void readsAllOverallRecordsFromSourceDataset() throws Exception {
		List<EmergencyDepartmentDto> rows = new EmergencyDepartmentService().getEmergencyDepartments();

		assertThat(rows).hasSize(104);
		assertThat(rows).extracting(EmergencyDepartmentDto::facilityCode)
				.doesNotHaveDuplicates().doesNotContain("99999");
		assertThat(rows).extracting(EmergencyDepartmentDto::facilityName)
				.doesNotContain("Queensland").contains("Queensland Children's");
		assertThat(rows.getFirst().facilityName()).isEqualTo("Mater Adult");
		assertThat(rows.getFirst().numberOfAttendances()).isEqualTo(13130);
	}

	private EmergencyDepartmentService fixtureService() {
		return new EmergencyDepartmentService(Path.of("src/test/resources/emergency-departments.csv"));
	}
}

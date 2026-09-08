package com.antra.qldserviceintelligence.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.antra.qldserviceintelligence.service.EmergencyDepartmentService;

class EmergencyDepartmentControllerTest {

	@Test
	void returnsOnlyRealFacilitiesAndKeepsQueenslandChildrens() throws Exception {
		var mvc = MockMvcBuilders.standaloneSetup(
				new EmergencyDepartmentController(new EmergencyDepartmentService())).build();

		mvc.perform(get("/api/v1/emergency-departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(104))
				.andExpect(jsonPath("$[*].facilityCode",
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("99999"))))
				.andExpect(jsonPath("$[*].facilityName",
						org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("Queensland"))))
				.andExpect(jsonPath("$[*].facilityName",
						org.hamcrest.Matchers.hasItem("Queensland Children's")));
	}

	@Test
	void statusEndpointRemainsAvailableAlongsideEmergencyDepartments() throws Exception {
		var mvc = MockMvcBuilders.standaloneSetup(new StatusController(),
				new EmergencyDepartmentController(new EmergencyDepartmentService())).build();

		mvc.perform(get("/api/v1/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.service").value("QLD Service Intelligence"))
				.andExpect(jsonPath("$.status").value("operational"));
	}

	@Test
	void exposesOverallDepartmentRecords() throws Exception {
		var service = new EmergencyDepartmentService(Path.of("src/test/resources/emergency-departments.csv"));
		var mvc = MockMvcBuilders.standaloneSetup(new EmergencyDepartmentController(service)).build();

		mvc.perform(get("/api/v1/emergency-departments"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].facilityCode").value("001"))
				.andExpect(jsonPath("$[0].facilityName").value("Royal Brisbane & Women's"))
				.andExpect(jsonPath("$[0].quarter").value("Sep-25"))
				.andExpect(jsonPath("$[0].numberOfAttendances").value(13130))
				.andExpect(jsonPath("$[0].attendanceVariationPercent").value(-2.5))
				.andExpect(jsonPath("$[1].numberOfAttendances").value(org.hamcrest.Matchers.nullValue()));
	}
}

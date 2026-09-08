package com.antra.qldserviceintelligence.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.antra.qldserviceintelligence.service.PopulationService;

class PopulationControllerTest {

	@TempDir
	Path directory;

	@Test
	void exposesGrowthAndPreservesExistingPopulationResponse() throws Exception {
		Path csv = directory.resolve("population.csv");
		Files.writeString(csv, "LGA,2020,2025p\nCentral Highlands (R),1200,1301\nQueensland,1200,1301\n");
		MockMvc mvc = MockMvcBuilders.standaloneSetup(
				new PopulationController(new PopulationService(csv))).build();

		mvc.perform(get("/api/v1/population"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						[{"lga":"Central Highlands (R)","population2025":1301}]
						"""))
				.andExpect(jsonPath("$[0].population2020").doesNotExist())
				.andExpect(jsonPath("$[0].growthPercent").doesNotExist());

		mvc.perform(get("/api/v1/population/growth"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						[{"lga":"Central Highlands (R)","population2020":1200,"population2025":1301,
						"populationChange":101,"growthPercent":8.42}]
						"""));
	}
}

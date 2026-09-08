package com.antra.qldserviceintelligence.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.antra.qldserviceintelligence.service.PopulationService;

class PopulationControllerTest {

	@TempDir
	Path directory;

	@Test
	void fastestReturnsTenHighestGrowthPercentagesInDescendingOrder() throws Exception {
		StringBuilder csv = new StringBuilder("LGA,2020,2025p\n");
		for (int growth : new int[] { 3, 12, 1, 8, 5, 11, 2, 10, 6, 4, 9, 7 }) {
			csv.append("Area ").append(growth).append(",100,").append(100 + growth).append('\n');
		}
		csv.append("Undefined,0,100\nDeclining,100,90\nQueensland,100,200\n");
		ResultActions result = mvcFor(csv.toString()).perform(get("/api/v1/population/growth/fastest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(10));

		for (int index = 0; index < 10; index++) {
			int growth = 12 - index;
			result.andExpect(jsonPath("$[" + index + "].lga").value("Area " + growth))
					.andExpect(jsonPath("$[" + index + "].growthPercent").value((double) growth));
		}
	}

	@Test
	void fastestReturnsFewerThanTenWhenFewerAreAvailable() throws Exception {
		mvcFor("LGA,2020,2025p\nSmall (S),100,105\nUndefined,0,10\n")
				.perform(get("/api/v1/population/growth/fastest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].lga").value("Small (S)"));
	}

	@Test
	void decliningReturnsOnlyNegativeGrowthInAscendingOrder() throws Exception {
		mvcFor("""
				LGA,2020,2025p
				Mild (S),100,99
				Growing (S),100,110
				Severe (S),100,80
				Unchanged (S),100,100
				Moderate (S),100,95
				Undefined (S),0,100
				Queensland,100,50
				""")
				.perform(get("/api/v1/population/growth/declining"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].growthPercent").value(-20.0))
				.andExpect(jsonPath("$[1].growthPercent").value(-5.0))
				.andExpect(jsonPath("$[2].growthPercent").value(-1.0));
	}

	@Test
	void rankingsReturnEmptyListsWhenNoPercentagesQualify() throws Exception {
		MockMvc mvc = mvcFor("LGA,2020,2025p\nUndefined,0,100\n");
		for (String ranking : new String[] { "fastest", "declining" }) {
			mvc.perform(get("/api/v1/population/growth/" + ranking))
					.andExpect(status().isOk())
					.andExpect(content().json("[]"));
		}
	}

	private MockMvc mvcFor(String contents) throws Exception {
		Path csv = directory.resolve("ranking.csv");
		Files.writeString(csv, contents);
		return MockMvcBuilders.standaloneSetup(new PopulationController(new PopulationService(csv))).build();
	}

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

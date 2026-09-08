package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.antra.qldserviceintelligence.model.PopulationDto;
import com.antra.qldserviceintelligence.model.PopulationGrowthDto;

class PopulationServiceTest {

	@Test
	void readsPopulationFromMetadataAndSplitHeaderRows() throws Exception {
		Path csv = Files.createTempFile("qld-population", ".csv");
		Files.writeString(csv, """
				Population estimates,,,,
				,,,,
				LGA,Estimated resident population at 30 June,,
				,2001,2024r,2025p
				Aurukun (S),1,1,\"1,165\"
				Queensland,1,1,\"5,669,764\"
				Invalid LGA,1,1,not-a-number
				,,,,
				""");

		List<PopulationDto> result = new PopulationService(csv).getPopulation();

		assertThat(result).containsExactly(new PopulationDto("Aurukun (S)", 1165));
	}

	@Test
	void readsCombinedHeaderRow() throws Exception {
		Path csv = Files.createTempFile("qld-population", ".csv");
		Files.writeString(csv, """
				LGA,2001,2025p
				Boulia (S),566,489
				""");

		assertThat(new PopulationService(csv).getPopulation())
				.containsExactly(new PopulationDto("Boulia (S)", 489));
	}

	@Test
	void preservesInternalSpacesInLgaNames() throws Exception {
		Path csv = Files.createTempFile("qld-population", ".csv");
		Files.writeString(csv, """
				LGA,2001,2025p
				Central Highlands (R),1,29481
				Cloncurry (S),1,3907
				North Burnett (R),1,10562
				""");

		assertThat(new PopulationService(csv).getPopulation())
				.extracting(PopulationDto::lga)
				.containsExactly("Central Highlands (R)", "Cloncurry (S)", "North Burnett (R)");
	}

	@Test
	void calculatesGrowthAndPreservesSourceNamesWhileExcludingInvalidRows() throws Exception {
		Path csv = Files.createTempFile("qld-population-growth", ".csv");
		Files.writeString(csv, """
				Population estimates,,
				LGA,Estimated resident population at 30 June,
				,2020,2025p
				  Central  Highlands (R)  ,"1,200","1,301"
				Blackall-Tambo (R),300,280
				No change (S),100,100
				Queensland,1600,1681
				Metadata,,
				Invalid baseline,not-a-number,100
				Invalid current,100,not-a-number
				Missing baseline,,100
				Short row
				,100,200
				""");

		assertThat(new PopulationService(csv).getPopulationGrowth()).containsExactly(
				new PopulationGrowthDto("Central  Highlands (R)", 1200, 1301, 101, new BigDecimal("8.42")),
				new PopulationGrowthDto("Blackall-Tambo (R)", 300, 280, -20, new BigDecimal("-6.67")),
				new PopulationGrowthDto("No change (S)", 100, 100, 0, new BigDecimal("0.00")));
	}

	@Test
	void readsGrowthFromCombinedHeaderAndHandlesZeroBaseline() throws Exception {
		Path csv = Files.createTempFile("qld-population-growth", ".csv");
		Files.writeString(csv, """
				LGA,2025p,2020
				Example (S),10,0
				""");

		assertThat(new PopulationService(csv).getPopulationGrowth()).containsExactly(
				new PopulationGrowthDto("Example (S)", 0, 10, 10, null));
	}

	@Test
	void readsGrowthForEveryLgaInTheSourceDataset() throws Exception {
		PopulationService service = new PopulationService();

		assertThat(service.getPopulationGrowth()).hasSize(78)
				.extracting(PopulationGrowthDto::lga)
				.containsExactlyElementsOf(service.getPopulation().stream().map(PopulationDto::lga).toList())
				.doesNotContain("Queensland");
	}
}

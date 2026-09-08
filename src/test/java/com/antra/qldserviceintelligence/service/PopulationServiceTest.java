package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.antra.qldserviceintelligence.model.PopulationDto;

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
}
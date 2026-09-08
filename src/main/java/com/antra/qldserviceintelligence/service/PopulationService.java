package com.antra.qldserviceintelligence.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import com.antra.qldserviceintelligence.model.PopulationDto;
import com.antra.qldserviceintelligence.model.PopulationGrowthDto;

@Service
public class PopulationService {

	private static final Path DEFAULT_DATA_PATH = Path.of("data/raw/qld_lga_population_2001_2025.csv");

	private final Path dataPath;

	public PopulationService() {
		this(DEFAULT_DATA_PATH);
	}

	public PopulationService(Path dataPath) {
		this.dataPath = dataPath;
	}

	public List<PopulationDto> getPopulation() throws IOException {
		return readPopulation(false).stream()
				.map(row -> new PopulationDto(row.lga(), row.population2025()))
				.toList();
	}

	public List<PopulationGrowthDto> getPopulationGrowth() throws IOException {
		return readPopulation(true).stream().map(row -> {
			int change = row.population2025() - row.population2020();
			// A percentage is undefined when the baseline is zero.
			BigDecimal growthPercent = row.population2020() == 0 ? null
					: BigDecimal.valueOf(change).multiply(BigDecimal.valueOf(100))
							.divide(BigDecimal.valueOf(row.population2020()), 2, RoundingMode.HALF_UP);
			return new PopulationGrowthDto(row.lga(), row.population2020(), row.population2025(),
					change, growthPercent);
		}).toList();
	}

	private record PopulationRow(String lga, int population2020, int population2025) {
	}

	private List<PopulationRow> readPopulation(boolean include2020) throws IOException {
		try (CSVParser parser = CSVParser.parse(dataPath, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
			List<CSVRecord> records = parser.getRecords();
			int lgaHeaderIndex = findLgaHeaderIndex(records);
			int populationColumnIndex = findPopulationColumnIndex(records, lgaHeaderIndex, "2025p");
			int baselineColumnIndex = include2020 ? findPopulationColumnIndex(records, lgaHeaderIndex, "2020") : -1;

			List<PopulationRow> population = new ArrayList<>();
			for (int recordIndex = lgaHeaderIndex + 1; recordIndex < records.size(); recordIndex++) {
				CSVRecord record = records.get(recordIndex);
				if (record.size() <= Math.max(populationColumnIndex, baselineColumnIndex)) {
					continue;
				}

				String lga = parseLgaName(record);
				if (lga.isBlank() || lga.equalsIgnoreCase("Queensland")) {
					continue;
				}

				String populationValue = record.get(populationColumnIndex).trim();
				if (populationValue.isBlank()) {
					continue;
				}

				try {
					int population2025 = Integer.parseInt(populationValue.replace(",", ""));
					int population2020 = include2020
							? Integer.parseInt(record.get(baselineColumnIndex).trim().replace(",", "")) : 0;
					if (include2020 && (population2020 < 0 || population2025 < 0)) {
						continue;
					}
					population.add(new PopulationRow(lga, population2020, population2025));
				} catch (NumberFormatException ignored) {
					// Metadata and malformed rows are not population records.
				}
			}
			return population;
		}
	}

	private String parseLgaName(CSVRecord record) {
		return record.get(0).strip();
	}

	private int findLgaHeaderIndex(List<CSVRecord> records) {
		for (int index = 0; index < records.size(); index++) {
			if (records.get(index).size() > 0 && records.get(index).get(0).trim().equalsIgnoreCase("LGA")) {
				return index;
			}
		}
		throw new IllegalArgumentException("CSV does not contain an LGA header");
	}

	private int findPopulationColumnIndex(List<CSVRecord> records, int lgaHeaderIndex, String year) {
		for (int recordIndex = lgaHeaderIndex; recordIndex < records.size(); recordIndex++) {
			CSVRecord record = records.get(recordIndex);
			for (int columnIndex = 0; columnIndex < record.size(); columnIndex++) {
				if (record.get(columnIndex).trim().equalsIgnoreCase(year)) {
					return columnIndex;
				}
			}
		}
		throw new IllegalArgumentException("CSV does not contain a " + year + " column");
	}
}

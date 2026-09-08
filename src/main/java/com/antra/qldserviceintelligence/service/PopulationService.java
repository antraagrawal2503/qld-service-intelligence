package com.antra.qldserviceintelligence.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import com.antra.qldserviceintelligence.model.PopulationDto;

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
		try (CSVParser parser = CSVParser.parse(dataPath, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
			List<CSVRecord> records = parser.getRecords();
			int lgaHeaderIndex = findLgaHeaderIndex(records);
			int populationColumnIndex = findPopulationColumnIndex(records, lgaHeaderIndex);

			List<PopulationDto> population = new ArrayList<>();
			for (int recordIndex = lgaHeaderIndex + 1; recordIndex < records.size(); recordIndex++) {
				CSVRecord record = records.get(recordIndex);
				if (record.size() <= populationColumnIndex) {
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
					population.add(new PopulationDto(lga, Integer.parseInt(populationValue.replace(",", ""))));
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

	private int findPopulationColumnIndex(List<CSVRecord> records, int lgaHeaderIndex) {
		for (int recordIndex = lgaHeaderIndex; recordIndex < records.size(); recordIndex++) {
			CSVRecord record = records.get(recordIndex);
			for (int columnIndex = 0; columnIndex < record.size(); columnIndex++) {
				if (record.get(columnIndex).trim().equalsIgnoreCase("2025p")) {
					return columnIndex;
				}
			}
		}
		throw new IllegalArgumentException("CSV does not contain a 2025p column");
	}
}
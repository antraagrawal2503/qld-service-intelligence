package com.antra.qldserviceintelligence.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;

@Service
public class EmergencyDepartmentService {

	private static final Path DEFAULT_DATA_PATH = Path.of("data/raw/qld_emergency_department_sep2025.csv");
	private final Path dataPath;

	public EmergencyDepartmentService() {
		this(DEFAULT_DATA_PATH);
	}

	public EmergencyDepartmentService(Path dataPath) {
		this.dataPath = dataPath;
	}

	public List<EmergencyDepartmentDto> getEmergencyDepartments() throws IOException {
		CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get();
		try (CSVParser parser = CSVParser.parse(dataPath, StandardCharsets.UTF_8, format)) {
			List<EmergencyDepartmentDto> departments = new ArrayList<>();
			for (CSVRecord record : parser) {
				if (!"ALL".equals(record.get("Triage_Category"))) {
					continue;
				}
				// Code 99999 identifies the statewide aggregate, not an individual facility.
				if ("99999".equals(record.get("Facility/HHS Code").trim())) {
					continue;
				}
				departments.add(new EmergencyDepartmentDto(
						record.get("Facility/HHS Code"),
						record.get("Facility/HHS Desc"),
						record.get("Last Month in QTR"),
						parseInteger(record.get("Number of Attendances")),
						parseDecimal(record.get("Variation in the number of attendances (%)")),
						parseDecimal(record.get("Patients Seen within clinically recommended times (%)")),
						parseInteger(record.get("Median Waiting time to treatment (minutes)")),
						parseDecimal(record.get("Patients who did not wait for treatment (%)")),
						parseDecimal(record.get("Patients admitted from the Emergency Department (%)")),
						parseDecimal(record.get("Admissions to hospital within 4 hours (%)")),
						parseDecimal(record.get("Patients whose ED stay was within 4 hours (%)")),
						parseInteger(record.get("Number of patients who left after treatment commenced")),
						parseDecimal(record.get("Percentage of patients who left after treatment commenced (%)")),
						parseInteger(record.get("Number of patients who did not wait for treatment"))));
			}
			return departments;
		}
	}

	private BigDecimal parseDecimal(String value) {
		try {
			return new BigDecimal(value.trim().replace(",", ""));
		} catch (NumberFormatException ignored) {
			// Blank, unavailable ("-"), and malformed values are unknown, not zero.
			return null;
		}
	}

	private Integer parseInteger(String value) {
		BigDecimal number = parseDecimal(value);
		try {
			return number == null ? null : number.intValueExact();
		} catch (ArithmeticException ignored) {
			return null;
		}
	}
}

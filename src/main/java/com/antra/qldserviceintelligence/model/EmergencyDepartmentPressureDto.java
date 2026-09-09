package com.antra.qldserviceintelligence.model;

import java.math.BigDecimal;
import java.util.List;

/** Exploratory comparative heuristic; not a clinically validated measure or predictive model. */
public record EmergencyDepartmentPressureDto(
		String facilityCode,
		String facilityName,
		String quarter,
		BigDecimal pressureScore,
		PressureLevel pressureLevel,
		List<String> drivers,
		List<String> unavailableMetrics) {

	public enum PressureLevel { LOW, MEDIUM, HIGH, UNKNOWN }
}

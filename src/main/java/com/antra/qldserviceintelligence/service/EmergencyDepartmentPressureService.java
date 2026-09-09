package com.antra.qldserviceintelligence.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentPressureDto;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentPressureDto.PressureLevel;

/** Relative to the current source cohort, not a clinical assessment or prediction. */
@Service
public class EmergencyDepartmentPressureService {

	private static final MathContext PRECISION = MathContext.DECIMAL128;
	// Declaration order breaks ties in driver contributions and orders unavailable metrics.
	private static final List<Metric> METRICS = List.of(
			new Metric("medianWaitingTimeMinutes", new BigDecimal("0.30"), true,
					row -> row.medianWaitingTimeMinutes() == null ? null : BigDecimal.valueOf(row.medianWaitingTimeMinutes()),
					"Long median waiting time"),
			new Metric("patientsSeenWithinRecommendedTimePercent", new BigDecimal("0.30"), false,
					EmergencyDepartmentDto::patientsSeenWithinRecommendedTimePercent,
					"Low percentage seen within recommended time"),
			new Metric("patientsDidNotWaitPercent", new BigDecimal("0.20"), true,
					EmergencyDepartmentDto::patientsDidNotWaitPercent,
					"High percentage of patients who did not wait"),
			new Metric("edStayWithin4HoursPercent", new BigDecimal("0.20"), false,
					EmergencyDepartmentDto::edStayWithin4HoursPercent,
					"Low percentage of ED stays completed within 4 hours"));

	private final EmergencyDepartmentService source;

	public EmergencyDepartmentPressureService(EmergencyDepartmentService source) {
		this.source = source;
	}

	public List<EmergencyDepartmentPressureDto> getPressure() throws IOException {
		var rows = source.getEmergencyDepartments();
		var bounds = METRICS.stream().map(metric -> boundsFor(metric, rows)).toList();
		return rows.stream().map(row -> score(row, bounds)).toList();
	}

	public List<EmergencyDepartmentPressureDto> getHighestPressure() throws IOException {
		return getPressure().stream().filter(row -> row.pressureScore() != null)
				.sorted(Comparator.comparing(EmergencyDepartmentPressureDto::pressureScore).reversed()
						.thenComparing(EmergencyDepartmentPressureDto::facilityCode)
						.thenComparing(EmergencyDepartmentPressureDto::quarter))
				.limit(10).toList();
	}

	private Bounds boundsFor(Metric metric, List<EmergencyDepartmentDto> rows) {
		BigDecimal min = null;
		BigDecimal max = null;
		for (var row : rows) {
			BigDecimal value = metric.value().apply(row);
			if (value != null) {
				min = min == null ? value : min.min(value);
				max = max == null ? value : max.max(value);
			}
		}
		return new Bounds(min, max);
	}

	private EmergencyDepartmentPressureDto score(EmergencyDepartmentDto row, List<Bounds> bounds) {
		BigDecimal weightedSum = BigDecimal.ZERO;
		BigDecimal availableWeight = BigDecimal.ZERO;
		List<String> unavailable = new ArrayList<>();
		List<Contribution> contributions = new ArrayList<>();
		for (int index = 0; index < METRICS.size(); index++) {
			Metric metric = METRICS.get(index);
			BigDecimal value = metric.value().apply(row);
			if (value == null) {
				unavailable.add(metric.name());
				continue;
			}
			Bounds range = bounds.get(index);
			BigDecimal span = range.max().subtract(range.min());
			// A constant metric provides no comparative pressure, but is still available.
			BigDecimal normalized = span.signum() == 0 ? BigDecimal.ZERO
					: (metric.higherIsWorse() ? value.subtract(range.min()) : range.max().subtract(value))
							.multiply(BigDecimal.valueOf(100)).divide(span, PRECISION);
			BigDecimal contribution = normalized.multiply(metric.weight());
			weightedSum = weightedSum.add(contribution);
			availableWeight = availableWeight.add(metric.weight());
			if (contribution.signum() > 0) {
				contributions.add(new Contribution(metric.explanation(), contribution));
			}
		}
		BigDecimal score = availableWeight.signum() == 0 ? null
				: weightedSum.divide(availableWeight, 2, RoundingMode.HALF_UP);
		// Stable sorting retains metric declaration order for equal contributions.
		List<String> drivers = contributions.stream()
				.sorted(Comparator.comparing(Contribution::value).reversed()).limit(2)
				.map(Contribution::explanation).toList();
		return new EmergencyDepartmentPressureDto(row.facilityCode(), row.facilityName(), row.quarter(),
				score, level(score), drivers, List.copyOf(unavailable));
	}

	private PressureLevel level(BigDecimal score) {
		if (score == null) return PressureLevel.UNKNOWN;
		if (score.compareTo(BigDecimal.valueOf(40)) < 0) return PressureLevel.LOW;
		if (score.compareTo(BigDecimal.valueOf(70)) < 0) return PressureLevel.MEDIUM;
		return PressureLevel.HIGH;
	}

	private record Metric(String name, BigDecimal weight, boolean higherIsWorse,
			Function<EmergencyDepartmentDto, BigDecimal> value, String explanation) { }
	private record Bounds(BigDecimal min, BigDecimal max) { }
	private record Contribution(String explanation, BigDecimal value) { }
}

package com.antra.qldserviceintelligence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.antra.qldserviceintelligence.controller.EmergencyDepartmentController;
import com.antra.qldserviceintelligence.controller.EmergencyDepartmentPressureController;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentDto;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentPressureDto;
import com.antra.qldserviceintelligence.model.EmergencyDepartmentPressureDto.PressureLevel;

class EmergencyDepartmentPressureServiceTest {

	@Test
	void normalizesHigherIsWorseWaitingTimeUsingObservedRange() throws Exception {
		var scores = analytics(List.of(row("a", 10, null, null, null),
				row("b", 20, null, null, null), row("c", 50, null, null, null))).getPressure();
		assertScores(scores, "0.00", "25.00", "100.00");
	}

	@Test
	void normalizesHigherIsWorseDidNotWaitUsingObservedRange() throws Exception {
		var scores = analytics(List.of(row("a", null, null, "2", null),
				row("b", null, null, "5", null), row("c", null, null, "14", null))).getPressure();
		assertScores(scores, "0.00", "25.00", "100.00");
	}

	@Test
	void normalizesLowerIsWorseSeenWithinRecommendedTime() throws Exception {
		var scores = analytics(List.of(row("a", null, "20", null, null),
				row("b", null, "80", null, null), row("c", null, "100", null, null))).getPressure();
		assertScores(scores, "100.00", "25.00", "0.00");
	}

	@Test
	void normalizesLowerIsWorseEdStay() throws Exception {
		var scores = analytics(List.of(row("a", null, null, null, "20"),
				row("b", null, null, null, "80"), row("c", null, null, null, "100"))).getPressure();
		assertScores(scores, "100.00", "25.00", "0.00");
	}

	@Test
	void appliesAllFourWeights() throws Exception {
		var scores = analytics(List.of(best(), worst(), row("target", 80, "40", "20", "90"))).getPressure();
		// 80 * .30 + 60 * .30 + 20 * .20 + 10 * .20 = 48.
		assertThat(scores.get(2).pressureScore()).isEqualTo(new BigDecimal("48.00"));
	}

	@ParameterizedTest
	@CsvSource({"0,LOW", "39.99,LOW", "40,MEDIUM", "69.99,MEDIUM", "70,HIGH", "100,HIGH",
			"39.994,LOW", "39.995,MEDIUM", "69.995,HIGH"})
	void classifiesRoundedScoreBoundaries(String value, PressureLevel expected) throws Exception {
		var scores = analytics(List.of(best(), worst(), row("target", null, null, value, null))).getPressure();
		assertThat(scores.get(2).pressureLevel()).isEqualTo(expected);
		assertThat(scores.get(2).pressureScore()).isEqualTo(
				new BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP));
	}

	@Test
	void renormalizesAvailableWeightsAndReportsMissingMetrics() throws Exception {
		var score = analytics(List.of(best(), worst(), row("target", 100, null, "0", null)))
				.getPressure().get(2);
		assertThat(score.pressureScore()).isEqualTo(new BigDecimal("60.00"));
		assertThat(score.unavailableMetrics()).containsExactly(
				"patientsSeenWithinRecommendedTimePercent", "edStayWithin4HoursPercent");
		assertThat(score.drivers()).containsExactly("Long median waiting time");
	}

	@Test
	void unknownFacilityHasNullScoreNoDriversAndAllMetricsUnavailable() throws Exception {
		var score = analytics(List.of(row("unknown", null, null, null, null))).getPressure().getFirst();
		assertThat(score.pressureScore()).isNull();
		assertThat(score.pressureLevel()).isEqualTo(PressureLevel.UNKNOWN);
		assertThat(score.drivers()).isEmpty();
		assertThat(score.unavailableMetrics()).containsExactly("medianWaitingTimeMinutes",
				"patientsSeenWithinRecommendedTimePercent", "patientsDidNotWaitPercent", "edStayWithin4HoursPercent");
	}

	@Test
	void ignoresMissingValuesWhenDeterminingBounds() throws Exception {
		var scores = analytics(List.of(row("missing", null, null, null, null),
				row("a", 20, null, null, null), row("b", 40, null, null, null))).getPressure();
		assertThat(scores.getFirst().pressureScore()).isNull();
		assertScores(scores.subList(1, 3), "0.00", "100.00");
	}

	@Test
	void constantMetricsContributeZeroAndRemainAvailable() throws Exception {
		var scores = analytics(List.of(row("a", 20, "50", "5", "50"),
				row("b", 20, "50", "5", "50"))).getPressure();
		assertScores(scores, "0.00", "0.00");
		assertThat(scores).allSatisfy(score -> {
			assertThat(score.drivers()).isEmpty();
			assertThat(score.unavailableMetrics()).isEmpty();
			assertThat(score.pressureLevel()).isEqualTo(PressureLevel.LOW);
		});
	}

	@Test
	void constantMetricStillCountsInAvailableWeight() throws Exception {
		var scores = analytics(List.of(row("a", 20, null, "0", null),
				row("b", 20, null, "10", null))).getPressure();
		assertScores(scores, "0.00", "40.00");
	}

	@Test
	void driversRankWeightedContributionsAndBreakTiesInMetricOrder() throws Exception {
		var service = analytics(List.of(best(), worst(), row("target", 50, "100", "100", "0")));
		var scores = service.getPressure();
		assertThat(scores.get(1).drivers()).containsExactly("Long median waiting time",
				"Low percentage seen within recommended time");
		assertThat(scores.get(2).drivers()).containsExactly("High percentage of patients who did not wait",
				"Low percentage of ED stays completed within 4 hours");
		assertThat(service.getPressure()).isEqualTo(scores);
		assertThat(scores.getFirst().drivers()).isEmpty();
	}

	@Test
	void emptySourceReturnsEmptyAnalyticsAndRankings() throws Exception {
		var service = analytics(List.of());
		assertThat(service.getPressure()).isEmpty();
		assertThat(service.getHighestPressure()).isEmpty();
	}

	@Test
	void highestEndpointReturnsTenSortedScoresAndExcludesUnknown() throws Exception {
		List<EmergencyDepartmentDto> rows = new ArrayList<>();
		rows.add(row("unknown", null, null, null, null));
		for (int i = 0; i <= 12; i++) rows.add(row("facility-" + i, i * 10, null, null, null));
		var service = analytics(rows);
		var highest = service.getHighestPressure();
		assertThat(highest).hasSize(10);
		assertThat(highest).extracting(EmergencyDepartmentPressureDto::pressureScore)
				.isSortedAccordingTo(java.util.Comparator.reverseOrder());
		var mvc = MockMvcBuilders.standaloneSetup(new EmergencyDepartmentPressureController(service)).build();
		var result = mvc.perform(get("/api/v1/emergency-departments/pressure/highest"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
		for (int index = 0; index < 10; index++) {
			result.andExpect(jsonPath("$[" + index + "].facilityCode").value("facility-" + (12 - index)))
					.andExpect(jsonPath("$[" + index + "].pressureScore").value(highest.get(index).pressureScore().doubleValue()));
		}
	}

	@Test
	void highestEndpointHandlesFewerThanTenAndTiesDeterministically() throws Exception {
		var service = analytics(List.of(row("b", 10, null, null, null),
				row("a", 10, null, null, null), row("unknown", null, null, null, null)));
		var mvc = MockMvcBuilders.standaloneSetup(new EmergencyDepartmentPressureController(service)).build();
		mvc.perform(get("/api/v1/emergency-departments/pressure/highest"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].facilityCode").value("a"))
				.andExpect(jsonPath("$[1].facilityCode").value("b"));
	}

	@Test
	void allFacilityEndpointIncludesUnknownAndExplainabilityFields() throws Exception {
		var mvc = MockMvcBuilders.standaloneSetup(new EmergencyDepartmentPressureController(
				analytics(List.of(best(), worst(), row("unknown", null, null, null, null))))).build();
		mvc.perform(get("/api/v1/emergency-departments/pressure"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[1].facilityCode").value("worst"))
				.andExpect(jsonPath("$[1].facilityName").value("Facility worst"))
				.andExpect(jsonPath("$[1].quarter").value("Sep-25"))
				.andExpect(jsonPath("$[1].pressureScore").value(100.0))
				.andExpect(jsonPath("$[1].pressureLevel").value("HIGH"))
				.andExpect(jsonPath("$[1].drivers.length()").value(2))
				.andExpect(jsonPath("$[1].unavailableMetrics").isEmpty())
				.andExpect(jsonPath("$[2].pressureScore").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$[2].pressureLevel").value("UNKNOWN"))
				.andExpect(jsonPath("$[2].unavailableMetrics.length()").value(4));
	}

	@Test
	void realDatasetReturns104FacilitiesAndExistingEndpointRemainsUnchanged() throws Exception {
		var source = new EmergencyDepartmentService();
		var service = new EmergencyDepartmentPressureService(source);
		var scores = service.getPressure();
		assertThat(scores).hasSize(104).noneMatch(row -> row.facilityCode().equals("99999")
				|| row.facilityName().equals("Queensland"));
		assertThat(scores).allSatisfy(score -> {
			if (score.pressureScore() != null) assertThat(score.pressureScore()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
			assertThat(score.drivers().size()).isLessThanOrEqualTo(2);
		});
		var mvc = MockMvcBuilders.standaloneSetup(new EmergencyDepartmentPressureController(service),
				new EmergencyDepartmentController(source)).build();
		mvc.perform(get("/api/v1/emergency-departments/pressure"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(104));
		mvc.perform(get("/api/v1/emergency-departments"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(104))
				.andExpect(content().json(new tools.jackson.databind.ObjectMapper().writeValueAsString(source.getEmergencyDepartments())))
				.andExpect(jsonPath("$[0].pressureScore").doesNotExist());
	}

	private EmergencyDepartmentPressureService analytics(List<EmergencyDepartmentDto> rows) {
		return new EmergencyDepartmentPressureService(new EmergencyDepartmentService() {
			@Override public List<EmergencyDepartmentDto> getEmergencyDepartments() { return rows; }
		});
	}

	private EmergencyDepartmentDto best() { return row("best", 0, "100", "0", "100"); }
	private EmergencyDepartmentDto worst() { return row("worst", 100, "0", "100", "0"); }

	private EmergencyDepartmentDto row(String code, Integer wait, String seen, String didNotWait, String stay) {
		return new EmergencyDepartmentDto(code, "Facility " + code, "Sep-25", null, null, decimal(seen),
				wait, decimal(didNotWait), null, null, decimal(stay), null, null, null);
	}

	private BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }

	private void assertScores(List<EmergencyDepartmentPressureDto> rows, String... values) {
		assertThat(rows).extracting(EmergencyDepartmentPressureDto::pressureScore)
				.containsExactly(java.util.Arrays.stream(values).map(BigDecimal::new).toArray(BigDecimal[]::new));
	}
}

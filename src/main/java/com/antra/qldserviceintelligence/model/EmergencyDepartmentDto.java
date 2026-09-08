package com.antra.qldserviceintelligence.model;

import java.math.BigDecimal;

public record EmergencyDepartmentDto(
		String facilityCode,
		String facilityName,
		String quarter,
		Integer numberOfAttendances,
		BigDecimal attendanceVariationPercent,
		BigDecimal patientsSeenWithinRecommendedTimePercent,
		Integer medianWaitingTimeMinutes,
		BigDecimal patientsDidNotWaitPercent,
		BigDecimal patientsAdmittedFromEdPercent,
		BigDecimal admissionsWithin4HoursPercent,
		BigDecimal edStayWithin4HoursPercent,
		Integer patientsLeftAfterTreatmentCommenced,
		BigDecimal patientsLeftAfterTreatmentCommencedPercent,
		Integer patientsDidNotWait) {
}

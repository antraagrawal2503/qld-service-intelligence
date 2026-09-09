package com.antra.qldserviceintelligence.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "emergency_department_records", uniqueConstraints =
		@UniqueConstraint(name = "uk_ed_facility_quarter", columnNames = { "facility_code", "quarter" }))
public class EmergencyDepartmentRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "facility_code", nullable = false)
	private String facilityCode;

	@Column(nullable = false)
	private String facilityName;

	@Column(nullable = false)
	private String quarter;

	private Integer numberOfAttendances;

	@Column(precision = 19, scale = 6)
	private BigDecimal attendanceVariationPercent;

	@Column(precision = 19, scale = 6)
	private BigDecimal patientsSeenWithinRecommendedTimePercent;

	private Integer medianWaitingTimeMinutes;

	@Column(precision = 19, scale = 6)
	private BigDecimal patientsDidNotWaitPercent;

	@Column(precision = 19, scale = 6)
	private BigDecimal patientsAdmittedFromEdPercent;

	@Column(precision = 19, scale = 6)
	private BigDecimal admissionsWithin4HoursPercent;

	@Column(precision = 19, scale = 6)
	private BigDecimal edStayWithin4HoursPercent;

	private Integer patientsLeftAfterTreatmentCommenced;

	@Column(precision = 19, scale = 6)
	private BigDecimal patientsLeftAfterTreatmentCommencedPercent;

	private Integer patientsDidNotWait;

	protected EmergencyDepartmentRecord() {
	}

	public EmergencyDepartmentRecord(EmergencyDepartmentDto row) {
		this.facilityCode = row.facilityCode().strip();
		this.quarter = row.quarter().strip();
		updateFrom(row);
	}

	public void updateFrom(EmergencyDepartmentDto row) {
		this.facilityName = row.facilityName();
		this.numberOfAttendances = row.numberOfAttendances();
		this.attendanceVariationPercent = row.attendanceVariationPercent();
		this.patientsSeenWithinRecommendedTimePercent = row.patientsSeenWithinRecommendedTimePercent();
		this.medianWaitingTimeMinutes = row.medianWaitingTimeMinutes();
		this.patientsDidNotWaitPercent = row.patientsDidNotWaitPercent();
		this.patientsAdmittedFromEdPercent = row.patientsAdmittedFromEdPercent();
		this.admissionsWithin4HoursPercent = row.admissionsWithin4HoursPercent();
		this.edStayWithin4HoursPercent = row.edStayWithin4HoursPercent();
		this.patientsLeftAfterTreatmentCommenced = row.patientsLeftAfterTreatmentCommenced();
		this.patientsLeftAfterTreatmentCommencedPercent = row.patientsLeftAfterTreatmentCommencedPercent();
		this.patientsDidNotWait = row.patientsDidNotWait();
	}

	public Long getId() {
		return id;
	}

	public String getFacilityCode() {
		return facilityCode;
	}

	public String getFacilityName() {
		return facilityName;
	}

	public String getQuarter() {
		return quarter;
	}

	public Integer getNumberOfAttendances() {
		return numberOfAttendances;
	}

	public BigDecimal getAttendanceVariationPercent() {
		return attendanceVariationPercent;
	}

	public BigDecimal getPatientsSeenWithinRecommendedTimePercent() {
		return patientsSeenWithinRecommendedTimePercent;
	}

	public Integer getMedianWaitingTimeMinutes() {
		return medianWaitingTimeMinutes;
	}

	public BigDecimal getPatientsDidNotWaitPercent() {
		return patientsDidNotWaitPercent;
	}

	public BigDecimal getPatientsAdmittedFromEdPercent() {
		return patientsAdmittedFromEdPercent;
	}

	public BigDecimal getAdmissionsWithin4HoursPercent() {
		return admissionsWithin4HoursPercent;
	}

	public BigDecimal getEdStayWithin4HoursPercent() {
		return edStayWithin4HoursPercent;
	}

	public Integer getPatientsLeftAfterTreatmentCommenced() {
		return patientsLeftAfterTreatmentCommenced;
	}

	public BigDecimal getPatientsLeftAfterTreatmentCommencedPercent() {
		return patientsLeftAfterTreatmentCommencedPercent;
	}

	public Integer getPatientsDidNotWait() {
		return patientsDidNotWait;
	}
}

package com.antra.qldserviceintelligence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "population_records", uniqueConstraints =
		@UniqueConstraint(name = "uk_population_records_lga", columnNames = "lga"))
public class PopulationRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String lga;

	@Column(nullable = false)
	private int population2020;

	@Column(nullable = false)
	private int population2025;

	protected PopulationRecord() {
	}

	public PopulationRecord(String lga, int population2020, int population2025) {
		this.lga = lga.strip();
		updatePopulation(population2020, population2025);
	}

	public void updatePopulation(int population2020, int population2025) {
		this.population2020 = population2020;
		this.population2025 = population2025;
	}

	public Long getId() { return id; }
	public String getLga() { return lga; }
	public int getPopulation2020() { return population2020; }
	public int getPopulation2025() { return population2025; }
}

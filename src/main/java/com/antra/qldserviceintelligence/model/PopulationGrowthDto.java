package com.antra.qldserviceintelligence.model;

import java.math.BigDecimal;

public record PopulationGrowthDto(String lga, int population2020, int population2025,
		int populationChange, BigDecimal growthPercent) {
}

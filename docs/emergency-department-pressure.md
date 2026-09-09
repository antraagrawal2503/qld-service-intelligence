# Emergency Department pressure analytics

This score is an exploratory comparative heuristic, **not a clinically validated measure or predictive model**. LOW, MEDIUM and HIGH describe relative scores within the source cohort; they do not establish clinical safety, care quality, or an individual patient's likely experience.

- `GET /api/v1/emergency-departments/pressure` returns all facilities in source order.
- `GET /api/v1/emergency-departments/pressure/highest` returns at most 10 facilities with non-null scores, sorted by score descending. Equal scores are ordered by facility code, then quarter, ascending.

Both endpoints use the existing CSV-backed ED service. Its current cohort consists of 104 real facility records from Sep-25, using only `ALL` triage rows and excluding the statewide aggregate. Existing APIs and persistence are unchanged. No population data or hospital-to-LGA mapping is used.

| Metric | Higher pressure | Weight |
| --- | --- | --- |
| `medianWaitingTimeMinutes` | Higher value | 0.30 |
| `patientsSeenWithinRecommendedTimePercent` | Lower value | 0.30 |
| `patientsDidNotWaitPercent` | Higher value | 0.20 |
| `edStayWithin4HoursPercent` | Lower value | 0.20 |

Each metric's minimum and maximum are calculated over non-null observations across the full source cohort, before selecting the highest 10. Higher-is-worse components use `100 * (value - min) / (max - min)`; lower-is-worse components use `100 * (max - value) / (max - min)`.

A constant metric (`max == min`) contributes zero comparative pressure and retains its available weight. Missing observations are omitted, never imputed as zero. The score is the sum of available weighted components divided by the sum of available weights. If all four observations are missing, the score is null and the level is UNKNOWN.

Scores are rounded to two decimal places using HALF_UP. Levels are assigned to the rounded score: LOW below 40, MEDIUM from 40 to below 70, HIGH from 70 through 100. This keeps the reported score and level consistent at rounding boundaries.

The response includes facility code, facility name, quarter, score, level, `drivers`, and `unavailableMetrics`. Unavailable metrics use the field names above. Drivers are fixed human-readable descriptions of at most the two strongest positive weighted contributions, ranked before rounding. Weight renormalization does not change their order within a facility. Equal contributions use the table's metric order; zero contributions produce no driver. No LLM generates explanations.

These scores depend on cohort extremes and metric availability. Changes to the cohort can change a facility's score without changes to its own measurements. Facilities with different available metrics have different effective weights. The current source is a single quarter; these endpoints do not implement cross-quarter trend comparisons.

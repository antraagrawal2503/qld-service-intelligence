"use strict";

(() => {
  const integer = new Intl.NumberFormat("en-AU", { maximumFractionDigits: 0 });
  const decimal = new Intl.NumberFormat("en-AU", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const signedInteger = new Intl.NumberFormat("en-AU", { maximumFractionDigits: 0, signDisplay: "exceptZero" });
  const signedDecimal = new Intl.NumberFormat("en-AU", { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: "exceptZero" });
  const isNumber = value => typeof value === "number" && Number.isFinite(value);
  const format = (value, formatter = integer) => isNumber(value) ? formatter.format(value) : "Unavailable";
  const byId = id => document.getElementById(id);

  // API strings are always inserted as text, never parsed as HTML.
  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  // Share successful responses for this page, including source metrics used by the KPI and inspector.
  // Failed requests are evicted so each retry can recover independently.
  const requests = new Map();
  function fetchRows(path) {
    if (!requests.has(path)) {
      requests.set(path, requestRows(path).catch(error => {
        requests.delete(path);
        throw error;
      }));
    }
    return requests.get(path);
  }

  async function requestRows(path) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    try {
      const response = await fetch(path, { signal: controller.signal, headers: { Accept: "application/json" } });
      if (!response.ok) throw new Error("Request failed");
      const rows = await response.json();
      if (!Array.isArray(rows)) throw new Error("Unexpected response");
      return rows;
    } finally {
      clearTimeout(timeout);
    }
  }

  function showError(target, message, retry) {
    const box = element("div", "error", message);
    const button = element("button", "retry", "Try again");
    button.type = "button";
    button.addEventListener("click", retry);
    box.append(button);
    target.replaceChildren(box);
  }

  async function loadKpi(id, path, count) {
    const target = byId(id);
    target.setAttribute("aria-busy", "true");
    target.replaceChildren(element("span", "loading", "Loading…"));
    try {
      target.textContent = integer.format(count(await fetchRows(path)));
    } catch {
      showError(target, "Data unavailable", () => loadKpi(id, path, count));
    } finally {
      target.setAttribute("aria-busy", "false");
    }
  }

  function bar(width, level = "", className = "") {
    const track = element("div", `bar ${className}`);
    track.setAttribute("aria-hidden", "true"); // The numeric value is exposed next to the visual.
    const fill = element("span", `bar-fill ${level}`);
    fill.style.width = `${Math.max(0, Math.min(100, width))}%`;
    track.append(fill);
    return track;
  }

  function rowHeading(name, className) {
    const heading = element("div", "row-heading");
    const rank = element("span", "rank");
    rank.setAttribute("aria-hidden", "true"); // The ordered list already supplies rank semantics.
    const details = element("div");
    details.append(element("h3", className, name || "Unnamed record"));
    heading.append(rank, details);
    return { heading, details };
  }

  function pressureRow(row) {
    const item = element("li", "ranking-row");
    const { heading, details } = rowHeading(row.facilityName, "facility-name");
    details.append(element("p", "quarter", row.quarter || "Quarter unavailable"));
    const score = element("div", "score", format(row.pressureScore, decimal));
    if (isNumber(row.pressureScore)) score.append(element("small", "", " / 100"));
    const level = ["HIGH", "MEDIUM", "LOW"].includes(row.pressureLevel) ? row.pressureLevel : "UNKNOWN";
    score.append(element("span", `badge ${level.toLowerCase()}`, level));
    heading.append(score);
    const overview = element("div", "pressure-overview");
    const toggle = element("button", "facility-toggle", row.facilityName || "Unnamed facility");
    toggle.type = "button";
    toggle.setAttribute("aria-label", `Inspect ${row.facilityName || "facility"}, ${row.quarter || "quarter unavailable"}`);
    toggle.setAttribute("aria-expanded", "false");
    const inspector = element("section", "facility-inspector");
    inspector.id = `facility-inspector-${++inspectorId}`;
    inspector.hidden = true;
    inspector.setAttribute("aria-label", `Details for ${row.facilityName || "facility"}`);
    toggle.setAttribute("aria-controls", inspector.id);
    details.children[0].replaceChildren(toggle);
    toggle.addEventListener("click", () => {
      const open = inspector.hidden;
      inspector.hidden = !open;
      toggle.setAttribute("aria-expanded", String(open));
      if (open && !inspector.childElementCount) renderInspector(inspector, row);
    });
    overview.append(heading);
    if (isNumber(row.pressureScore)) overview.append(bar(row.pressureScore, level.toLowerCase()));
    const drivers = element("ul", "drivers");
    for (const driver of row.drivers || []) drivers.append(element("li", "", driver));
    if (!drivers.childElementCount) drivers.append(element("li", "", "No positive pressure drivers available"));
    overview.append(drivers);
    if (row.unavailableMetrics?.length) {
      overview.append(element("p", "missing-note", `${row.unavailableMetrics.length} of 4 metrics unavailable; available weights renormalized.`));
    }
    overview.append(element("span", "inspect-hint", "Inspect metrics ↗"));
    item.append(overview, inspector);
    return item;
  }

  function populationRow(row, maxGrowth) {
    const item = element("li", "ranking-row");
    const { heading } = rowHeading(row.lga, "area-name");
    heading.append(element("span", "growth-score", isNumber(row.growthPercent) ? `${signedDecimal.format(row.growthPercent)}%` : "Unavailable"));
    const stats = element("dl", "population-stats");
    for (const [label, value, formatter] of [
      ["2020", row.population2020, integer],
      ["2025", row.population2025, integer],
      ["Change", row.populationChange, signedInteger]
    ]) {
      const cell = element("div");
      cell.append(element("dt", "", label), element("dd", "", format(value, formatter)));
      stats.append(cell);
    }
    item.append(heading, stats);
    if (isNumber(row.growthPercent)) {
      // Visual scaling only: growth values and ranking come from the backend.
      item.append(bar(maxGrowth > 0 ? Math.abs(row.growthPercent) / maxGrowth * 100 : 0, "", "growth-bar"));
    }
    return item;
  }

  let inspectorId = 0;
  const sourceMetrics = [
    ["medianWaitingTimeMinutes", "Median waiting time", " minutes"],
    ["patientsSeenWithinRecommendedTimePercent", "Patients seen within recommended time", "%"],
    ["patientsDidNotWaitPercent", "Patients who did not wait", "%"],
    ["edStayWithin4HoursPercent", "ED stay within 4 hours", "%"]
  ];

  function renderInspector(inspector, row) {
    inspector.append(element("h4", "", row.facilityName), element("p", "quarter", `Reporting quarter: ${row.quarter || "Unavailable"}`));
    inspector.append(element("h4", "", "Pressure index"));
    const score = element("p", "inspector-score", isNumber(row.pressureScore) ? `${decimal.format(row.pressureScore)} / 100` : "Unavailable");
    const level = ["HIGH", "MEDIUM", "LOW"].includes(row.pressureLevel) ? row.pressureLevel : "UNKNOWN";
    score.append(element("span", `badge ${level.toLowerCase()}`, level));
    inspector.append(score, element("p", "", "This comparative score summarises the published metrics shown below."));
    const drivers = element("ul", "inspector-drivers");
    for (const driver of row.drivers || []) drivers.append(element("li", "", driver));
    inspector.append(drivers.childElementCount ? drivers : element("p", "", "No positive pressure drivers available."));
    if (row.unavailableMetrics?.length) {
      const names = row.unavailableMetrics.map(key => sourceMetrics.find(metric => metric[0] === key)?.[1] || key);
      inspector.append(element("p", "", `Unavailable metrics: ${names.join("; ")}.`));
    }
    inspector.append(element("h4", "source-heading", "Published source metrics"));
    const source = element("div", "source-metrics");
    source.setAttribute("role", "status");
    source.setAttribute("aria-live", "polite");
    inspector.append(source);
    loadSourceMetrics(source, row);
  }

  async function loadSourceMetrics(target, pressure) {
    target.setAttribute("aria-busy", "true");
    target.textContent = "Loading published metrics…";
    try {
      const rows = await fetchRows("/api/v1/emergency-departments");
      // Match the persisted source key, never facility name or inferred geography.
      const row = rows.find(row => row.facilityCode?.trim() === pressure.facilityCode?.trim()
        && row.quarter?.trim() === pressure.quarter?.trim());
      if (!row) {
        target.textContent = "No published source record matches this facility and reporting quarter.";
        return;
      }
      const metrics = element("dl", "inspector-metrics");
      for (const [key, label, unit] of sourceMetrics) {
        const cell = element("div");
        const value = isNumber(row[key]) ? `${format(row[key], unit === "%" ? decimal : integer)}${unit}` : "Unavailable";
        cell.append(element("dt", "", label), element("dd", "", value));
        metrics.append(cell);
      }
      target.replaceChildren(metrics);
    } catch {
      showError(target, "Published source metrics could not be loaded.", () => loadSourceMetrics(target, pressure));
    } finally {
      target.setAttribute("aria-busy", "false");
    }
  }

  const explorerRows = { pressure: null, population: null };
  function descending(a, b, field) {
    if (!isNumber(a[field])) return isNumber(b[field]) ? 1 : 0;
    if (!isNumber(b[field])) return -1;
    return b[field] - a[field];
  }
  const compareText = (a, b) => a < b ? -1 : a > b ? 1 : 0;

  function renderExplorer(kind) {
    if (!explorerRows[kind]) return;
    const pressure = kind === "pressure";
    const search = byId(pressure ? "facility-search" : "area-search").value.trim().toLocaleLowerCase("en-AU");
    const level = pressure ? byId("pressure-level").value : "ALL";
    const top = byId(`${kind}-view`).value === "top";
    const rows = explorerRows[kind].filter(row => {
      const name = pressure ? row.facilityName : row.lga;
      return (name || "").toLocaleLowerCase("en-AU").includes(search)
        && (!pressure || level === "ALL" || row.pressureLevel === level);
    }).sort((a, b) => pressure
      ? descending(a, b, "pressureScore") || compareText(a.facilityCode, b.facilityCode) || compareText(a.quarter, b.quarter)
      : descending(a, b, "growthPercent")); // Stable ties preserve the backend's source order.
    // Match the original top-10 endpoints when no filters are active. All views and
    // explicit searches/filters retain unknown scores, after the available scores.
    const eligible = top && !search && level === "ALL"
      ? rows.filter(row => isNumber(row[pressure ? "pressureScore" : "growthPercent"])) : rows;
    const visible = top ? eligible.slice(0, 10) : eligible;
    byId(`${kind}-view-label`).textContent = top ? "Top 10" : pressure ? "All facilities" : "All areas";
    const fragment = document.createDocumentFragment();
    const maxGrowth = pressure ? 0 : Math.max(0, ...visible.map(row => isNumber(row.growthPercent) ? Math.abs(row.growthPercent) : 0));
    for (const row of visible) fragment.append(pressure ? pressureRow(row) : populationRow(row, maxGrowth));
    byId(`${kind}-list`).replaceChildren(fragment);
    const noun = pressure ? "facilities" : "areas";
    byId(`${kind}-state`).textContent = visible.length
      ? `Showing ${visible.length} of ${eligible.length} matching ${noun}.`
      : explorerRows[kind].length ? `No ${noun} match this selection. Try another search, filter or view.`
        : `No ${noun} are available in this dataset.`;
  }

  async function loadExplorer(kind, path) {
    const state = byId(`${kind}-state`);
    const list = byId(`${kind}-list`);
    byId(`${kind}-controls`).disabled = true;
    list.setAttribute("aria-busy", "true");
    state.textContent = kind === "pressure" ? "Loading facility pressure…" : "Loading population growth…";
    try {
      explorerRows[kind] = await fetchRows(path);
      byId(`${kind}-controls`).disabled = false;
      renderExplorer(kind);
    } catch {
      showError(state, "This section could not be loaded. Please try again.", () => loadExplorer(kind, path));
    } finally {
      list.setAttribute("aria-busy", "false");
    }
  }

  for (const [id, event, kind] of [
    ["facility-search", "input", "pressure"], ["pressure-level", "change", "pressure"],
    ["pressure-view", "change", "pressure"], ["area-search", "input", "population"],
    ["population-view", "change", "population"]
  ]) byId(id).addEventListener(event, () => renderExplorer(kind));

  // Each section loads independently; filters operate on API data without recalculating analytics.
  loadKpi("ed-count", "/api/v1/emergency-departments", rows => rows.length);
  loadKpi("population-count", "/api/v1/population", rows => rows.length);
  loadKpi("high-count", "/api/v1/emergency-departments/pressure", rows => rows.filter(row => row.pressureLevel === "HIGH").length);
  loadExplorer("pressure", "/api/v1/emergency-departments/pressure");
  loadExplorer("population", "/api/v1/population/growth");
})();

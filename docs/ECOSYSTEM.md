# Fil Ecosystem

How Fil relates to the Bios suite (Bios, W2F, Smokeless, Virgil, SoulRadio).
Fil is a **companion** in the Bios sense: it owns specialized capture
surfaces (gait analysis via phone accelerometer, planned keystroke and
cognitive micro-tests) and a domain-specific drift engine for MS that Bios
cannot reasonably own. When fully wired, it will push computed neurological
scores back to Bios's metric bus for cross-correlation with cardiovascular
and sleep signals.

For the **suite-wide rule** see Bios's
[`docs/ECOSYSTEM_BOUNDARIES.md`](../../Bios/docs/ECOSYSTEM_BOUNDARIES.md).

For the **visual map** of all data flows across the six apps and which
edges ship vs. remain planned, see
[miam-knowledge-base/docs/ecosystem-map.md](https://github.com/mi4m/miam-knowledge-base/blob/main/docs/ecosystem-map.md).

---

## Status (2026-05)

- **Built:** gait pipeline (stride time, variability, asymmetry, cadence,
  step count, walking segments), per-axis + composite drift z-score engine,
  on-device fall detection. All stored locally.
- **Planned:** keystroke analysis (AccessibilityService), SDMT/tapping/
  contrast micro-tests, MS-specific composite score.
- **Bios writes:** four keys reserved (`gait_asymmetry`, `cognitive_speed`,
  `motor_score`, `relapse_risk`) — **none wired today**.

Per Bios's `ECOSYSTEM_BOUNDARIES.md`: Fil's gait + cognitive signals are
exactly the kind of cross-system input the generic anomaly engine needs to
distinguish MS relapse from infection from cardiovascular drift. When Fil
wires the writes, the unlock is real.

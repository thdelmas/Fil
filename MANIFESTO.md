# Fil

## Le fil de vos jours. Toujours.

Every second, your nervous system carries thousands of signals through pathways insulated by myelin — the sheath that keeps the current flowing. Multiple sclerosis strips that insulation away. Signals slow, scatter, drop. The body still speaks, but the words blur.

Fil exists to keep the thread visible.

---

## The Problem

MS relapses don't announce themselves. They build quietly — a slight drag in one foot, a half-second slower reaction, a word that won't come. By the time you notice, the inflammatory cascade has been running for days. The neurologist sees you every six months. Between visits, you're on your own.

Meanwhile, the phone in your pocket measures gait, the watch on your wrist measures heart rate, and your fingers on the screen measure dexterity — every single day, without you thinking about it. These signals contain the early warning. Nobody is reading them.

## What We Do

Fil reads the thread.

We **listen** to the passive signals your phone and wearable already generate — walking rhythm, typing speed, finger precision, heart rate variability, sleep architecture, fatigue patterns.

We **ask** for 30 seconds of your morning — a quick symbol-matching game, a tapping exercise — to measure cognitive processing speed and fine motor control with clinical-grade precision.

We **watch** for the drift — the moment your personal pattern starts shifting away from your baseline. Not population averages. Your normal. Your thread.

We **speak up** when the thread frays — days before you'd notice, weeks before your next appointment. Clear, calm, factual. Never alarming. Never diagnosing. Just: "Your gait symmetry and typing speed have shifted this week. Here's what the data shows."

## Our Principles

### 1. The thread is yours

Your neurological data is the most intimate data that exists. It stays on your device. Nothing leaves without your explicit, informed decision. No cloud processing. No "anonymous" aggregation without consent. Fil works entirely offline.

### 2. Detect early, speak clearly

Every feature answers one question: does this help catch a relapse sooner? We measure what the literature validates, present it without jargon, and always distinguish data from diagnosis. Fil is an instrument, not a doctor.

### 3. Respect the person

Fil is built for real people living real lives with MS. Not for researchers, not for clinicians, not for insurers. The interface respects autonomy, never catastrophizes, and never reduces a person to their disease. A bad data day is just a data day.

### 4. 30 seconds, not 30 minutes

Active tests must be brief enough to become habit. A morning routine, not a chore. If it takes longer than brushing your teeth, it won't survive the first month.

### 5. Compose, don't isolate

Fil is a Bios companion app. It reads from Bios's sensor pipeline (10 adapters, encrypted storage, computed baselines) and feeds neurological signals back. Bios watches the body; Fil watches the nerve. They compose — neither replaces the other.

## Architecture

### Passive Signals (always-on, zero effort)

| Signal | Source | What it catches |
|--------|--------|----------------|
| Gait asymmetry | Phone accelerometer | Spasticity, foot drop, cerebellar ataxia |
| Stride cadence | Phone accelerometer | Walking speed changes preceding relapse |
| Typing speed | AccessibilityService | Fine motor slowing, coordination loss |
| Keystroke errors | AccessibilityService | Tremor, imprecision |
| Postural sway | Phone accelerometer (standing) | Balance impairment |
| HRV | Bios (watch/PPG) | Autonomic dysfunction during relapse |
| Sleep architecture | Bios (Health Connect) | Fragmentation, fatigue signature |
| Activity level | Bios (steps, active minutes) | Fatigue-driven activity decline |
| Screen time pattern | UsageStats | Cognitive fatigue, engagement drop |

### Active Micro-Tests (30 seconds/morning)

| Test | Measures | Clinical equivalent |
|------|----------|-------------------|
| Symbol-Digit | Cognitive processing speed | SDMT (gold standard for MS cognition) |
| Finger Tapping | Fine motor dexterity | 9-Hole Peg Test |
| Contrast Letters | Visual acuity changes | Low-contrast letter acuity (optic neuritis) |

### Detection Engine

- **Personal baseline**: 14-day rolling window, same as W2F/Bios
- **Multivariate drift**: LSDD or EGADS on [gait, typing, cognition, dexterity, HRV, sleep] feature vector
- **Relapse risk score**: 0-100 composite, relative to personal normal
- **Uhthoff's correlation**: ambient temperature + symptom onset tracking

### Data Flow

```
Bios (sensor backbone: HRV, sleep, steps, activity)
  |
  v
Fil (gait analysis, typing, micro-tests, drift detection)
  |
  v
Bios (receives: gait_asymmetry, cognitive_speed, motor_score, relapse_risk)
```

## Tech Stack

- **Android**: Kotlin + Jetpack Compose
- **Data source**: Bios ContentProvider (primary), Health Connect (fallback)
- **On-device ML**: LiteRT for gait classification, EGADS for drift detection
- **Companion signals**: Pushes to Bios via `content://com.bios.app.health/companion/`
- **Package**: `com.fil.app`

## What Fil Is Not

Fil is not a diagnostic tool. It does not replace neurological examination. It does not predict disability progression. It does not tell you what to do.

Fil is a thread — thin, continuous, yours — that connects the days between appointments and makes the invisible visible. When you sit across from your neurologist, you bring data, not just memory. When a relapse begins, you see the shift in the numbers before you feel it in your legs.

Le fil. Toujours.

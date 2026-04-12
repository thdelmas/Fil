# Octopus Investigation 001: Improve Patient Life

**Date:** 2026-04-12
**Goal:** Discover open-source projects and contributors relevant to Fil's mission of MS relapse early detection and improving patient life.
**Origins crawled:** Sage-Bionetworks, muschellij2, mad-lab-fau, the-momentum, stanfordnmbl, cyrilzakka, Welltory, onnela-lab, BIDMCDigitalPsychiatry, RADAR-base, ResearchKit

---

## Tentacle A: MS-Specific Digital Monitoring (RADAR-CNS)

**The single most relevant ecosystem found.**

RADAR-base is an open-source platform (Apache Kafka-based) for remote patient monitoring. Their **RADAR-CNS** project ran a 24-month cohort study monitoring MS, depression, and epilepsy patients using continuous wearable/smartphone data. Published in JMIR 2025: "Longitudinal Digital Phenotyping of Multiple Sclerosis Severity Using Passively Sensed Behaviors."

| Repo | What | Why it matters |
|------|------|----------------|
| [RADAR-base/radar-prmt-android](https://github.com/RADAR-base/radar-prmt-android) | Android passive remote monitoring app (Kotlin) | Architecture reference for Fil's passive data collection |
| [RADAR-base/radar-commons-android](https://github.com/RADAR-base/radar-commons-android) | Base Android library + plugin system | Reusable plugin architecture for sensor modules |
| [RADAR-base/radar-android-phone](https://github.com/RADAR-base/radar-android-phone) | Phone sensor plugin (accelerometer, GPS, screen) | Direct overlap with Fil's passive signals |
| [RADAR-base/RADAR-Android-Audio](https://github.com/RADAR-base/RADAR-Android-Audio) | Voice capture plugin | Voice changes are an MS biomarker |
| [RADAR-base/RADAR-Schemas](https://github.com/RADAR-base/RADAR-Schemas) | Apache Avro data schemas | Interoperability reference |
| [RADAR-base/RADAR-Questionnaire](https://github.com/RADAR-base/RADAR-Questionnaire) | Active questionnaire app (TypeScript) | Patient-reported outcomes design |

**People to watch:** RADAR-base org contributors, especially the RADAR-CNS MS workstream team.

---

## Tentacle B: Gait Analysis & Biomechanics

| Repo | What | Why it matters |
|------|------|----------------|
| [mad-lab-fau/gaitmap](https://github.com/mad-lab-fau/gaitmap) | Premier open-source IMU gait analysis library | Core algorithms: stride detection, gait events, asymmetry |
| [mad-lab-fau/gaitmap-datasets](https://github.com/mad-lab-fau/gaitmap-datasets) | Open gait datasets | Validation data for Fil's gait module |
| [mobilise-d/mobgap](https://github.com/mobilise-d/mobgap) | Mobilise-D real-world gait pipeline | Production-grade: GSD, cadence, stride length from lower-back IMU |
| [stanfordnmbl/mobile-gaitlab](https://github.com/stanfordnmbl/mobile-gaitlab) | Predict gait params from video (89 stars) | Alternative gait assessment approach |
| [stanfordnmbl/GaitDynamics](https://github.com/stanfordnmbl/GaitDynamics) | Gait dynamics analysis (85 stars) | Gait pattern modeling |
| [stanfordnmbl/imu-fog-detection](https://github.com/stanfordnmbl/imu-fog-detection) | IMU freezing-of-gait detection | Relevant to MS spasticity/foot drop detection |
| [muschellij2/walking](https://github.com/muschellij2/walking) | Segment walking from accelerometry | Bridges Onnela Lab's forest to R; gait extraction |
| [muschellij2/stepcountpy](https://github.com/muschellij2/stepcountpy) | Step counting from accelerometer | UK Biobank-compatible, reference implementation |
| [kaurrachneet6/Vision-Based-Gait-Analysis-Framework-for-Predicting-Multiple-Sclerosis](https://github.com/kaurrachneet6/Vision-Based-Gait-Analysis-Framework-for-Predicting-Multiple-Sclerosis) | Deep learning gait analysis for MS | Directly MS-targeted gait prediction |

**Key people:** MAD Lab team at FAU Erlangen-Nurnberg, Mobilise-D consortium (EU-funded, regulatory-grade).

---

## Tentacle C: HRV & Physiological Signals

| Repo | What | Why it matters |
|------|------|----------------|
| [mad-lab-fau/BioPsyKit](https://github.com/mad-lab-fau/BioPsyKit) | ECG/HRV processing pipeline | R-peak detection, HRV features, ECG-derived respiration |
| [Welltory/measure-stress-hrv-android](https://github.com/Welltory/measure-stress-hrv-android) | Android HRV stress measurement SDK | Drop-in HRV analysis for Android |
| [Welltory/hrv-covid19](https://github.com/Welltory/hrv-covid19) | HRV + COVID wearables research (58 stars) | Methodology for HRV-disease correlation studies |
| [Welltory/welltory-ppg-dataset](https://github.com/Welltory/welltory-ppg-dataset) | Open PPG dataset (20 stars) | Validation data for PPG-based HRV |
| [rroblak/openhrv-android](https://github.com/rroblak/openhrv-android) | Open-source Android HRV tracker | Reference for HRV tracking UI/UX |
| [JanBancerewicz/PPGbetter](https://github.com/JanBancerewicz/PPGbetter) | Android PPG + HRV via camera | Camera-based HRV as fallback sensor |

---

## Tentacle D: Digital Phenotyping & Passive Sensing Platforms

| Repo | What | Why it matters |
|------|------|----------------|
| [onnela-lab/beiwe-android](https://github.com/onnela-lab/beiwe-android) | Beiwe Android app (Kotlin, 27 stars) | Open-source digital phenotyping, passive sensor collection |
| [onnela-lab/forest](https://github.com/onnela-lab/forest) | Forest phenotyping analysis library (37 stars) | Feature extraction from smartphone sensor data |
| [onnela-lab/als-wearables](https://github.com/onnela-lab/als-wearables) | ALS wearable monitoring | Neurodegenerative disease monitoring pattern (closest to MS) |
| [onnela-lab/als-beiwe-passive-data](https://github.com/onnela-lab/als-beiwe-passive-data) | ALS passive data analysis | Passive sensing for neurodegeneration |
| [BIDMCDigitalPsychiatry/LAMP-core-android](https://github.com/BIDMCDigitalPsychiatry/LAMP-core-android) | LAMP Android scaffolding (Kotlin) | Digital phenotyping app framework |
| [BIDMCDigitalPsychiatry/LAMP-cortex](https://github.com/BIDMCDigitalPsychiatry/LAMP-cortex) | LAMP data analysis engine | Cortex processes passive sensing data into features |

---

## Tentacle E: Anomaly Detection & Drift (Detection Engine)

| Repo | What | Why it matters |
|------|------|----------------|
| [yahoo/egads](https://github.com/yahoo/egads) | Extensible Generic Anomaly Detection System (Java) | Decoupled TMM/ADM architecture; automatic thresholding; runs on Android |
| [SeldonIO/alibi-detect](https://github.com/SeldonIO/alibi-detect) | LSDD drift detectors (Python, TF/PyTorch) | Online/streaming distribution shift detection |
| [rob-med/awesome-TS-anomaly-detection](https://github.com/rob-med/awesome-TS-anomaly-detection) | Curated list of TS anomaly detection | Survey of all approaches |
| [HsiangYangChu/LIBCDD](https://github.com/HsiangYangChu/LIBCDD) | LSDD Change Detection Test (Python) | Simple API for change point detection |
| [onnela-lab/online-anomaly](https://github.com/onnela-lab/online-anomaly) | Online anomaly detection | Streaming anomaly detection for phenotyping data |

**Architecture note:** EGADS (Java, Android-compatible) for point anomalies + LSDD for distributional shift = two-layer detection matching Fil's "multivariate drift" design.

---

## Tentacle F: Wearable Data Infrastructure & Android Sensor Access

| Repo | What | Why it matters |
|------|------|----------------|
| [the-momentum/open-wearables](https://github.com/the-momentum/open-wearables) | Unified wearable data API (self-hosted, MIT) | Privacy-first, 200+ devices, AI-ready |
| [the-momentum/open_wearables_android_sdk](https://github.com/the-momentum/open_wearables_android_sdk) | Android SDK | Drop-in wearable data ingestion |
| [mad-lab-fau/SensorLib](https://github.com/mad-lab-fau/SensorLib) | Android BLE sensor abstraction | Unified access to Empatica, Shimmer, NilsPod, Muse |
| [cyrilzakka/Halo](https://github.com/cyrilzakka/Halo) | Open-source health tracking (38 stars) | Privacy-first wearable reference |
| [ETH-PBL/H-Watch](https://github.com/ETH-PBL/H-Watch) | Open-source healthcare smartwatch | Fully open hardware + software |

---

## Tentacle G: Cognitive & Motor Testing

| Repo | What | Why it matters |
|------|------|----------------|
| [Sage-Bionetworks/mPower](https://github.com/Sage-Bionetworks/mPower) | ResearchKit Parkinson's app | Tapping, gait, voice, balance tasks — closest app to Fil |
| [Sage-Bionetworks/mhealthx](https://github.com/Sage-Bionetworks/mhealthx) | Mobile health feature extraction | pyGait, tapping features, voice features |
| [Sage-Bionetworks/BridgeAndroidSDK](https://github.com/Sage-Bionetworks/BridgeAndroidSDK) | Android research study SDK | Android infrastructure for active tests |
| [Sage-Bionetworks/SageResearch-Android](https://github.com/Sage-Bionetworks/SageResearch-Android) | Android research app framework | Open-source framework for research apps |
| [bielekovaLab/Bielekova-Lab-Code](https://github.com/bielekovaLab/Bielekova-Lab-Code) | Smartphone SDMT analysis code | Validated digital SDMT for MS patients |
| [adobrasinovic/edss](https://github.com/adobrasinovic/edss) | EDSS disability scale (16 stars) | Kurtzke scale implementation for MS disability quantification |

---

## Tentacle H: MS Imaging & Clinical Data

| Repo | What | Why it matters |
|------|------|----------------|
| [muschellij2/open_ms_data](https://github.com/muschellij2/open_ms_data) | Open MS MRI data with lesion segmentations | Ground truth validation data |
| [sergivalverde/nicMSlesions](https://github.com/sergivalverde/nicMSlesions) | MS lesion segmentation CNN (51 stars) | Automated lesion detection |
| [OpenKBC/multiple_sclerosis_proj](https://github.com/OpenKBC/multiple_sclerosis_proj) | ML/AI platform for MS | Data analysis + prediction |
| [muschellij2/oasis](https://github.com/muschellij2/oasis) | Automated MS lesion segmentation (OASIS) | Clinical validation pipeline |
| [muschellij2/nsrr](https://github.com/muschellij2/nsrr) | National Sleep Research Resource interface | Sleep data access for MS fatigue research |

---

## Key People

| Person | Affiliation | Why relevant | GitHub |
|--------|-------------|-------------|--------|
| John Muschelli | Johns Hopkins | Uniquely bridges MS imaging + wearable actigraphy | [muschellij2](https://github.com/muschellij2) |
| MAD Lab team | FAU Erlangen | Leading open gait/sensor research | [mad-lab-fau](https://github.com/mad-lab-fau) |
| RADAR-CNS team | King's College London | Ran 24-month MS digital monitoring study | [RADAR-base](https://github.com/RADAR-base) |
| Mobilise-D consortium | EU-funded | Regulatory-grade gait digital biomarkers | [mobilise-d](https://github.com/mobilise-d) |
| Onnela Lab | Harvard | Digital phenotyping pioneers, ALS wearables | [onnela-lab](https://github.com/onnela-lab) |
| Bielekova Lab | NIH/Johns Hopkins | Smartphone SDMT validation for MS | [bielekovaLab](https://github.com/bielekovaLab) |

---

## Unexplored Leads (Queue for future octopus searches)

- `Healios AG` — developed the dreaMS app (Basel, Switzerland), not open-source but worth monitoring
- `neurobooth/neurobooth-os` — digital phenotyping data acquisition
- `M-SenseResearchGroup` — wearable activity identification
- `OWEAR.org` — open wearable algorithms database
- `ResearchKit` contributors building active motor/cognitive tasks
- `vishnuravi` — followed by cyrilzakka, builds health-related iOS apps
- `bielekovaLab` members — MS-specific computational neurology
- RADAR-base individual contributors on the MS workstream

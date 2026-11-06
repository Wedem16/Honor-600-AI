# Lumina 600 Pro Camera & Studio Enhance

An original, one-handed flagship camera and computational photography suite designed for the HONOR 600 hardware profile, combining real-time `CameraCharacteristics` hardware introspection, pro-grade manual controls with docked live-monitoring overlays, and non-blocking post-capture OpenAI image enhancement.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX choices incorporate your confirmed preferences and define how hardware capabilities and cloud AI processing are handled safely on-device.

- **Confirmed Decision 1 — Unified Milestone Delivery (M1–M6)**: All six milestones (Core Camera & Capabilities, One-Handed UI Shell, Multi-Profile Video Engine, OpenAI Post-Capture Enhancement, Pro Manual & Live Monitoring Suite, and Presets/Thermal/Diagnostics) will be delivered together in a single, cohesive, compilable build.
- **Confirmed Decision 2 — OpenAI `gpt-image-1` & Configurable Endpoint**: The AI Enhance pipeline defaults to `gpt-image-1` via the multipart `/v1/images/edits` endpoint, with full settings support to switch models (`gpt-image-1`, `dall-e-2`) or configure a custom proxy backend URL alongside an encrypted on-device API key.
- **Confirmed Decision 3 — Docked Top-Corner Pro Monitoring HUD**: In Pro Photo and Pro Video modes, real-time luminance/color analysis runs on a non-blocking `ImageAnalysis` pipeline, displaying a compact live RGB/Luma Histogram in the top corner and allowing one active full-frame viewfinder overlay at a time (Zebra Stripes, Focus Peaking, or False Color).
- **Hardware Truthfulness & Capability Gating**: No resolution, frame rate, HDR profile, stabilization mode, or manual sensor control is ever faked. Every control dynamically queries Android's `CameraManager` and `CameraCharacteristics` at runtime; unsupported capabilities on the active lens are visibly disabled with a clear hardware diagnostic badge explaining why.

---

## 1. Overview & Core Concept

- **What It Does**: Lumina 600 provides a fast, ergonomic camera experience inspired by modern one-handed flagship camera interfaces while remaining 100% original in code, layout, and visual assets. It pairs hardware-accurate Photo, Video, Night, Portrait, Pro Photo, and Pro Video modes with an optional, asynchronous OpenAI image-editing workflow that preserves every original photo while generating an AI-enhanced companion image.
- **Target Audience / Persona**: Mobile photographers and videographers on the HONOR 600 (and compatible Android devices) who want honest hardware control, pro monitoring tools (Histograms, Zebras, Focus Peaking, False Color), reusable shooting presets, and customizable AI post-processing without shutter lag.
- **Key Value**:
  - **Zero-Lag Capture First**: Originals are always written to `MediaStore` with complete EXIF metadata before any AI network operation begins.
  - **100% Hardware Honesty**: Instant runtime inspection of every physical and logical camera lens, reporting exact supported resolutions, FPS ranges, 10-bit/HDR profiles, OIS/EIS support, and manual sensor ranges.
  - **User-Controlled AI Enhancement**: Never forces a hardcoded style prompt—users define their own prompt (empty by default with one-tap clear/reset), choose automatic or manual gallery enhancement, and toggle seamlessly between Original and AI versions.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First Launch & Permission Onboarding**:
   - Presents a clean, high-contrast permission sheet requesting Camera and Microphone access (with graceful fallback to photo-only or hardware diagnostics if declined).
   - When the user enables AI Enhance for the first time, displays a mandatory **One-Time Privacy & Upload Consent Notice** explaining that selected photos are transmitted to OpenAI (or their configured proxy endpoint) for processing.
2. **One-Handed Capture & Gesture Navigation**:
   - **Horizontal Swipe on Viewfinder**: Cycles smoothly across modes (`NIGHT` • `PORTRAIT` • `PHOTO` • `VIDEO` • `PRO PHOTO` • `PRO VIDEO`) with haptic and screen-reader announcements.
   - **Vertical Swipe on Viewfinder**: Flips between rear and front lenses with a spring-animated blur transition.
   - **Shutter Interactions**: Single tap captures a photo (or starts/stops video); long-pressing the shutter triggers high-speed burst capture or quick-video recording; non-blocking background AI jobs display a compact pill indicator near the gallery thumbnail with a cancel button.
   - **Four-Dot Quick Settings & Full Inspector**: Tapping the four-dot button opens a bottom-weighted quick-settings tray (aspect ratio, timer, flash, grid, level, exposure compensation, stabilization, resolution/FPS) and a link to the Hardware Compatibility Matrix & AI Settings sheet.
3. **Pro Photo & Pro Video Workflow**:
   - Bottom control deck reveals tactile scrubbers for **ISO**, **Shutter Speed**, **White Balance (Kelvin + presets)**, **Manual Focus Distance**, and **Exposure Compensation**, each bounded strictly by `SENSOR_INFO_SENSITIVITY_RANGE`, `SENSOR_INFO_EXPOSURE_TIME_RANGE`, and `LENS_INFO_MINIMUM_FOCUS_DISTANCE`.
   - Top-right docked HUD displays a real-time **Luminance/RGB Histogram** and quick pill toggles for **Zebra Stripes** (configurable clipping threshold), **Focus Peaking** (edge-detection highlight), or **False Color** (IRE exposure map).
4. **Built-In Dual-Version Gallery & AI Compare**:
   - Displays captured media directly from local storage/MediaStore with an **"AI"** badge on paired captures.
   - Includes a fast **Original / AI** segmented toggle, side-by-side metadata inspection, a manual **"Enhance with AI"** button (for captures taken while auto-enhance was off), and a full prompt/status log.
5. **Custom Presets & Hardware Diagnostics**:
   - Users can save, load, rename, and delete shooting presets (e.g., *"Low-Light Astro"*, *"4K60 Action"*). Loading a preset validates every setting against the currently active lens and warns if any parameter was clamped or skipped.
   - Dedicated **HONOR 600 Hardware & Compatibility Report** screen lists both real-time detected device capabilities and documented hardware limitations.

### Visual Identity & Theme

- **Aesthetic Direction**: **Precision Optical Dark (`Obsidian Viewfinder`)** — an original, tactile, bottom-weighted dark UI engineered for OLED glare reduction, outdoor legibility, and thumb-zone ergonomics.
- **Color Palette & Mood**:
  - `--background` / Viewfinder Canvas: Deep Optical Black (`#08090C`)
  - `--surface` / Floating Control Deck: Translucent Graphite (`#14171F` at 82% alpha)
  - `--surface-variant` / Pill Controls & Docked HUD: Matte Slate (`#1E222D`)
  - `--primary` / Active Mode & Focus Lock Accent: Amber Gold (`#FFB800`)
  - `--secondary` / AI Enhance & Computational Badge: Cyan Optic (`#00E5FF`)
  - `--error` / Recording Tally & Clipping Alert: Cinema Crimson (`#FF3B30`)
  - `--on-surface` / Primary Labels: Crisp Ivory (`#F5F7FA`) with high-contrast 7:1+ legibility
- **Typography & Hierarchy**:
  - **Display & Mode Headers**: `Space Grotesk` (bundled locally) in uppercase tracking for mode carousels (`PHOTO`, `PRO VIDEO`) and diagnostic headers.
  - **Technical Readouts & Telemetry**: `JetBrains Mono` (bundled locally) with tabular numerals for shutter speeds (`1/250s`), ISO (`ISO 400`), Kelvin (`5600K`), timecodes (`00:14:22`), and histogram IRE scales so numbers never jitter during live updates.
  - **Body & Settings Copy**: Clean geometric sans (`Inter` / system sans) scaled in `sp` for full large-text accessibility.
- **Component Styling & Layout**:
  - **Thumb-Zone Ergonomics**: All primary interactive controls (lens pills `0.6x / 1x / 2x / 5x`, mode ribbon, shutter ring, gallery badge, flip button, and four-dot quick settings) reside in the lower 38% of the screen, respecting `WindowInsets.navigationBars`.
  - **Top Utility Bar**: Reserved for passive telemetry, docked Pro Histogram HUD, thermal status badge, and quick status icons, padded with `WindowInsets.safeDrawing`.
  - **Accessibility**: Minimum `48dp x 48dp` touch targets on all buttons, explicit `contentDescription` and `testTag` attributes on every interactive control, and live accessibility announcements (`LiveRegionMode.Polite`) for countdown timers, capture completion, recording state, and AI processing progress.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Hybrid CameraX + Camera2 Interop Architecture**
  - *Chosen Approach*: Use Jetpack CameraX (`Preview`, `ImageCapture`, `VideoCapture`, `ImageAnalysis`) bridged with `Camera2Interop` and direct `CameraManager` / `CameraCharacteristics` queries.
  - *Why*: CameraX ensures rock-solid lifecycle binding, surface management, and zero memory leaks across foldables and orientation changes, while `Camera2Interop` and `CameraCharacteristics` provide raw access to hardware capability ranges (`SCALER_STREAM_CONFIGURATION_MAP`, `CONTROL_AE_LOCK`, `SENSOR_SENSITIVITY`, `SENSOR_EXPOSURE_TIME`, `CONTROL_AWB_MODE`, OIS/EIS flags, and dynamic range profiles).
  - *Alternatives Considered*: Pure low-level Camera2 state machines were passed over for preview/video session plumbing because they are prone to device-specific surface race conditions when switching rapidly between photo, video, and image analysis streams.
- **Decision 2: API Key Security — On-Device Encrypted Storage vs. Proxy Backend**
  - *Chosen Approach*: Provide an in-app **Encrypted Credential Store** (backed by Android Keystore AES-256-GCM encryption + optional `BuildConfig` secret injection) AND a configurable **Base Endpoint URL** so the user can either enter an OpenAI key directly or route requests through their own lightweight proxy server. The UI clearly explains the security trade-off between both modes.
  - *Why*: Storing a key in Android Keystore-backed encrypted preferences is convenient for personal devices and testing, whereas a proxy backend keeps the OpenAI secret off client hardware entirely and allows server-side rate limiting.
- **Decision 3: Non-Blocking Post-Capture AI Pipeline**
  - *Chosen Approach*: Capturing a photo immediately writes the original JPEG + EXIF to `MediaStore` (`Pictures/Lumina600`) and unlocks the shutter for the next shot. If AI Enhance is enabled (Auto or Manual), a background coroutine job encodes the image, calls `/v1/images/edits` with the user's custom prompt and selected quality/size, and saves the returned image as a separate `IMG_..._AI.jpg` file linked to the original.
  - *Why*: Guarantees zero shutter lag and ensures the user never loses a photo due to network drops, timeouts, or API rate limits.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Jetpack Compose + M3)                       │
├───────────────────────┬───────────────────────────┬───────────────────────────┤
│   Viewfinder Canvas   │   Docked Pro HUD (Top)    │  Bottom Ergonomic Deck    │
│ • CameraPreviewSurface│ • Compact RGB/Luma Hist   │ • Zoom Lens Pills         │
│ • Grid & Horizon Level│ • Single Active Overlay:  │ • Pro Scrubbers (ISO/S/WB)│
│ • Focus/Exposure Ring │   Zebra / Peaking / False │ • Mode Swipe Ribbon       │
│ • Gesture Detector    │ • Thermal & AI Status Pill│ • Shutter / Flip / Gallery│
└───────────┬───────────┴─────────────┬─────────────┴─────────────┬─────────────┘
            │                         │                           │
            ▼                         ▼                           ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & STATE LAYER (CameraViewModel)                 │
│ • StateFlow<CameraUiState> (Active Lens, Mode, Pro Params, Thermal, Presets) │
│ • StateFlow<HardwareCapabilityReport> (Supported Sizes, FPS, HDR, Manual)     │
│ • StateFlow<AiEnhanceQueueState> (Active Jobs, Consent, Prompt, Error/Retry)  │
└───────────┬─────────────────────────┬───────────────────────────┬─────────────┘
            │                         │                           │
            ▼                         ▼                           ▼
┌───────────────────────┐ ┌─────────────────────────┐ ┌─────────────────────────┐
│  HARDWARE & CAMERA2   │ │  OPENAI ENHANCE ENGINE  │ │ STORAGE & PERSISTENCE   │
│ • CameraCapabilities  │ │ • OkHttp/Retrofit Client│ │ • MediaStore + Exif     │
│   Inspector           │ │ • Multipart /images/edit│ │   Preservation Manager  │
│ • CameraX + Camera2   │ │ • gpt-image-1 / dall-e-2│ │ • Keystore AES-256-GCM  │
│   Capture Controller  │ │ • Cancellable Coroutine │ │   Credential Vault      │
│ • FrameAnalyzer (Luma │ │   Queue + Rate-Limit &  │ │ • Preset & Settings     │
│   Zebra/Peaking/False)│ │   Offline Error Handler │ │   Repository (JSON/Prefs│
└───────────────────────┘ └─────────────────────────┘ └─────────────────────────┘
```

### Data Model & State

- **`CameraLensCapability`**: Captures per-lens hardware truth (`cameraId`, `lensFacing`, `focalLengths`, `hyperFocalDistance`, `hardwareLevel`, `supportedPhotoSizes`, `supportedVideoProfiles` with 720p/1080p/4K and 24/30/60 FPS flags, `supportsHdr10`, `supportsOis`, `supportsEis`, `isoRange`, `exposureTimeRangeNs`, `minimumFocusDistance`, `availableAwbModes`, `exposureCompensationRange`, `exposureCompensationStep`, `maxRegionsAf`, `maxRegionsAe`, and `unsupportedFeatures: List<UnsupportedFeatureReason>`).
- **`ProControlState`**: Holds live manual parameters (`iso: Int?`, `shutterSpeedNs: Long?`, `wbMode: Int`, `wbKelvin: Int`, `focusDistanceDiopters: Float?`, `exposureCompIndex: Int`, `aeLocked: Boolean`, `afLocked: Boolean`, `activeOverlay: ProOverlayType`, `zebraThresholdPercent: Int`, `showHistogram: Boolean`).
- **`CapturedMediaPair`**: Represents a captured photo or video in the gallery (`id`, `originalUri`, `enhancedUri: Uri?`, `captureTimestamp`, `lensId`, `resolution`, `exifSummary`, `aiStatus: AiJobStatus`, `promptUsed: String?`, `errorMessage: String?`).
- **`CameraPreset`**: User-saved configuration (`id`, `name`, `mode`, `aspectRatio`, `resolution`, `fps`, `hdrEnabled`, `stabilizationEnabled`, `proControls`, `customFolder`).
- **`AiEnhanceSettings`**: User-controlled configuration (`enabled`, `autoEnhanceAfterCapture`, `userPrompt` defaulting to `""`, `model` defaulting to `"gpt-image-1"`, `imageSize`, `imageQuality`, `baseEndpointUrl`, `hasAcceptedConsent`).

### Interactive Component & State Mapping

- **Mode & Lens Switching**: Swiping horizontally or tapping a mode pill updates `currentMode` in `CameraViewModel`, re-validates available resolutions/FPS for the active lens, and rebinds the CameraX use cases (`ImageCapture` vs. `VideoCapture` + `ImageAnalysis`) without leaking surfaces.
- **Pro Sliders & Docked HUD**: Adjusting ISO or Shutter Speed automatically switches `CONTROL_AE_MODE` to `OFF` via `Camera2Interop.CaptureRequestOptions` (if supported by the lens; otherwise displays the hardware limitation reason). Enabling Histogram or one of the single-active overlays (`ZEBRA`, `FOCUS_PEAKING`, `FALSE_COLOR`) attaches the real-time YUV_420_888 `ImageAnalysis` processor and renders the overlay on a hardware-accelerated Compose `Canvas`.
- **AI Enhance Lifecycle**:
  1. User taps Shutter -> Original JPEG saved to `MediaStore/{customFolder}` with full EXIF (`ExifInterface`) -> Thumbnail updates immediately.
  2. If `aiEnhanceSettings.enabled && autoEnhanceAfterCapture` (or user taps **"Enhance"** in Gallery):
     - Checks `hasAcceptedConsent`; if false, displays the one-time OpenAI upload consent dialog.
     - Checks API key/endpoint and network connectivity; if missing or offline, surfaces a non-blocking banner with retry/settings actions.
     - Launches a cancellable coroutine job sending the image + `userPrompt` to `/v1/images/edits`, decodes the response (base64 or URL), copies core EXIF tags from the original, saves `IMG_..._AI.jpg`, and updates the `CapturedMediaPair` so the Gallery displays the **"AI"** badge and **Original / AI** toggle.
- **Thermal & Lifecycle Management**: Registers `PowerManager.OnThermalStatusChangedListener` (on API 29+ with safe fallback on API 24–28). When thermal status reaches `THERMAL_STATUS_MODERATE` or higher, displays a thermal badge, reduces analysis frame rate, and disables 4K60/heavy overlays if critical. Properly releases camera streams and sensor listeners (`Sensor.TYPE_GRAVITY` / `TYPE_ACCELEROMETER` for the horizon level) when lifecycle moves to `ON_STOP`.
- **Deliverable Documentation & Checklist Modal**: Includes an in-app **Manual Test Checklist & HONOR 600 Hardware Report** dialog (plus complete build/test notes) detailing every verified runtime check and listing features that standard Android Camera2 APIs on HONOR 600 cannot expose without vendor-private HAL keys.

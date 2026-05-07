# 🏗️ Project ClearRead Code Refactoring & Feature Implementation Plan

**Target Files:**
*   `app/src/main/java/com/clearread/ui/reader/PdfReaderScreen.kt` (Primary focus)
*   `app/src/main/java/com/clearread/ui/reader/PDFInteractionHelper.kt` (Secondary focus)

**Goal:** To transition the PDF reader from a functional but brittle prototype into a highly stable, maintainable, and feature-rich production component by addressing architectural flaws in gesture handling and improving resource robustness throughout the codebase.

## 🎯 Phase 0: Prerequisites & Setup
*(Before any major changes)*
1.  **Dependency Review:** Confirm current dependency versions are optimal for Compose/Android lifecycle (e.g., checking `lifecycle-viewmodel-compose` against project BOM). *(Self-correction step - No code change required unless an explicit issue is found.)*

## 🐛 Phase 1: Critical Bug Fixes & Stability Overhaul (High Priority)
**Target File:** `PdfReaderScreen.kt`

### 🐞 1. Gesture Handler Overhaul (The Core Fix)
*   **Problem:** The current monolithic `pointerInput` block conflates scrolling, zooming, and panning into one unstable mechanism. This is the leading cause of unpredictable UX.
*   **Solution:** Replace the custom gesture detection logic with a more robust, dedicated pattern. We will wrap the main content area in a composable that abstracts away the raw pointer handling.
    *   Introduce an intermediate state/logic layer to cleanly separate: `isPanning`, `isZooming`, and `isScrolling`.
    *   When panning/zooming is active, consumption of scroll events must be 100%. When only scrolling is intended, the gesture system must gracefully yield control back to the underlying `LazyColumn` (or equivalent scrollable view).

### 📐 2. Viewport Size Reliability
*   **Problem:** Relying solely on initial `onSizeChanged` can lead to stale or incorrect dimensions during rapid recompositions.
*   **Solution:** Utilize `Modifier.onGloballyPositioned` combined with `remember` logic that recalculates the viewport size whenever a significant layout parameter changes, ensuring `vpH`/`vpW` are always accurate relative to the current composition state.

### 🔢 3. Page Tracking Dead Zone Fix
*   **Problem:** The fixed `15px` dead zone is not scalable across different zoom levels or screen densities, leading to jitter or missed page changes.
*   **Solution:** Refactor the logic to calculate the "dead zone" as a *relative percentage* of the visible item's height (e.g., 5% buffer top/bottom) rather than a fixed pixel value. This ensures stability across different screen DPIs and zoom levels.

## ♻️ Phase 2: Refactoring & Robustness Improvements (Medium Priority)
**Target File:** `PDFInteractionHelper.kt`
**Target Files:** Global State Management in `PdfReaderScreen.kt`

### 🛠️ 1. Resource Management Cleanup (PDFInteractionHelper)
*   **Improvement:** Standardize all file/resource opening and closing (`ParcelFileDescriptor`, `InputStream`) using a single, private Kotlin extension function or helper within the class to ensure comprehensive resource cleanup and prevent potential leaks across all methods.

### ✨ 2. Code Idiom & Cleanliness (PDFInteractionHelper)
*   **Improvement:** Refactor complex `try-catch-finally` blocks into more modern Kotlin scope functions (`run`, `use`) where appropriate, greatly improving the readability without sacrificing safety.

### 🚀 3. State Consolidation (PdfReaderScreen.kt/ViewModel)
*   **Problem:** Multiple `LaunchedEffect`s are reacting independently to page changes, search triggers, etc., leading to state race conditions.
*   **Solution:** Consolidate all primary side effects and scrolling logic into a single flow collection mechanism within the ViewModel or composable scope. This makes the dependency graph explicit and easier to debug.

## 🔮 Phase 3: Feature Brainstorming & Enhancements (Low/Medium Priority)
*(To be implemented after stability is guaranteed)*

### 1. Text View Toggle (Accessibility)
*   **Feature:** Add a UI toggle that allows the user to switch from viewing raw, rendered PDF images to viewing the text content extracted and styled for reading.
    *   *Implementation Detail:* This will primarily utilize the `textContent` provided by the reader state, overlaying it on the page image where possible.

### 2. Proactive Prefetching (Performance)
*   **Feature:** Implement background prefetching of bitmaps for Page N+1 and N+2 whenever the user is stable on a page.
    *   *Implementation Detail:* This involves utilizing `CoroutineScope` timers/delay checks based on scroll speed to proactively trigger bitmap loading without blocking the UI thread, guaranteeing instant scrolling transitions.

***

**Next Steps:** Please analyze this plan. If you approve of the scope and the proposed architectural fixes (Phase 1 is critical), I will proceed with writing code changes for the first phase, starting with `PdfReaderScreen.kt`.
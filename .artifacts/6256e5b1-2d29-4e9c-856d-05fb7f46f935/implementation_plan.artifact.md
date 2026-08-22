# Replace `getIdentifier` with direct resource ID references

The goal is to eliminate usages of `Resources.getIdentifier()` in the project to improve performance, enable build optimizations, and ensure compile-time verification.

## Proposed Changes

### [Activity]

#### [MODIFY] [MainActivity.kt](file:///D:/MyProjects/Kotlin/Little Player/Little Player/app/src/main/java/com/flatcode/littleplayer/activity/MainActivity.kt)
- Replace dynamic resource lookup for `mc_bg` with `R.attr.mc_bg`.

### [Utils]

#### [MODIFY] [Extensions.kt](file:///D:/MyProjects/Kotlin/Little Player/Little Player/app/src/main/java/com/flatcode/littleplayer/utils/Extensions.kt)
- Refactor `getLibraryColor` to accept `@AttrRes attrId: Int` instead of a `String`.
- Update the internal logic to use `attrId` directly.
- Add an overload for `getLibraryColor(String)` that maps known attributes to their IDs and calls the `Int` version, marking it as deprecated.

### [Global Update]
- Update all call sites of `getLibraryColor(String)` to use `R.attr.*` or `com.google.android.material.R.attr.*`.

## Verification Plan

### Automated Tests
- Run existing unit tests (if any) to ensure no regressions in color retrieval logic.

### Manual Verification
- Verify that the theme colors (mc_track, mc_tick, etc.) are still correctly applied in the UI.
- Verify that the fallback colors (purple_500, purple_700) are used when attributes are missing.

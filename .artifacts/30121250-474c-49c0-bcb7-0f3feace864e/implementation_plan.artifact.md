# Implementation Plan - Add "Bulb" Category

The user wants to add a "Bulb" category to the "Integrate Device" dialog. Currently, the category "Lighting" exists but is hidden due to the category row not being scrollable, and it uses the name "Lighting" instead of "Bulb".

## Proposed Changes

### [Component Name] UI - Device Control

#### [MODIFY] [AddDeviceDialog.kt](file:///Users/pumudu/AndroidStudioProjects/Smarthome/app/src/main/java/com/example/smarthome/ui/devicecontrol/AddDeviceDialog.kt)
- Rename the display label for `DeviceType.LIGHT` from "Lighting" to "Bulb".
- Move `DeviceType.LIGHT` ("Bulb") to the second position in the list (after Outlet) for better visibility.
- Make the `DeviceTypeSelector` row horizontally scrollable using `.horizontalScroll(rememberScrollState())` to ensure all categories are accessible.

#### [MODIFY] [DeviceDetailSheet.kt](file:///Users/pumudu/AndroidStudioProjects/Smarthome/app/src/main/java/com/example/smarthome/ui/devicecontrol/DeviceDetailSheet.kt)
- Update `TypeBadge` to display "Bulb" instead of "Light" for `DeviceType.LIGHT` to maintain consistency across the app.

## Verification Plan

### Manual Verification
- Open the "Integrate Device" dialog (Add Device).
- Verify that "Bulb" is now visible and selectable.
- Verify that the category row can be scrolled horizontally.
- Add a "Bulb" device and verify that its detail sheet displays "Bulb" as the type.

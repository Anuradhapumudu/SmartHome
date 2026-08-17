# Walkthrough - Added "Bulb" Category

I have updated the "Integrate Device" dialog to include a "Bulb" category and improved its usability by making the category list scrollable.

## Changes Made

### UI Enhancements

#### [AddDeviceDialog.kt](file:///Users/pumudu/AndroidStudioProjects/Smarthome/app/src/main/java/com/example/smarthome/ui/devicecontrol/AddDeviceDialog.kt)
- Renamed the "Lighting" category to **"Bulb"**.
- Reordered the categories to place **"Bulb"** in the second position (immediately after Outlet) for easier access.
- Added **horizontal scrolling** to the category selection row, ensuring all device types are accessible on all screen sizes.

#### [DeviceDetailSheet.kt](file:///Users/pumudu/AndroidStudioProjects/Smarthome/app/src/main/java/com/example/smarthome/ui/devicecontrol/DeviceDetailSheet.kt)
- Updated the `TypeBadge` to display **"Bulb"** for light-type devices, maintaining consistency with the new category name.

## Verification Results

### Automated Tests
- Ran `gradle assembleDebug` to ensure the project still compiles correctly.
- Result: **Build successful.**

### Manual Verification Required
- Open the "Add Device" dialog and verify that **"Bulb"** is now visible and the category row can be scrolled.
- Add a device with the "Bulb" type and confirm it shows "Bulb" in its detail sheet.

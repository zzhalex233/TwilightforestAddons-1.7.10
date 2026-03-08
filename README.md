# Twilight Forest Addons

A Forge 1.12.2 addon project for The Twilight Forest.

## Overview

This workspace is the porting baseline for `twilightforestaddons` on Minecraft 1.12.2.
It is initialized with the target package, mod metadata, proxy structure, and a clean Gradle setup.

## Current Baseline

- Mod ID: `twilightforestaddons`
- Mod Name: `Twilight Forest Addons`
- Root package: `com.zzhalex233.twilightforestaddons`
- Minecraft: `1.12.2`
- Loader target: Forge / Cleanroom 1.12.2 template
- Twilight Forest is declared as a required runtime dependency in the main mod class

## Project Structure

- Main mod entry: `src/main/java/com/zzhalex233/twilightforestaddons/TwilightForestAddons.java`
- Proxies: `src/main/java/com/zzhalex233/twilightforestaddons/proxy/`
- Metadata: `src/main/resources/mcmod.info`
- Dependency setup: `gradle/scripts/dependencies.gradle`

## Next Porting Steps

1. Add the exact Twilight Forest 1.12.2 dependency in `gradle/scripts/dependencies.gradle`.
2. Port item, GUI, map, network, and teleport systems module by module.
3. Re-validate client and dedicated server startup after each subsystem lands.

## Development

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat build
```

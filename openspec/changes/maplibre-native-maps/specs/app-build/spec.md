# Spec Delta

## MODIFIED Requirements

### Requirement: Minified release preserves behavior

The system SHALL produce a release artifact with minification enabled that still works: data sync (Ktor client, kotlinx-serialization), map rendering (MapLibre Native), photo handling (CameraX/Coil), and persistence (Room).

#### Scenario: Minified build preserves runtime features
- **WHEN** a release artifact with minification enabled is installed and exercised
- **THEN** sync, maps, photos and persistence SHALL function as in the debug build

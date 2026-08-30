## Purpose
A app segue o modo escuro/claro do sistema em todas as camadas visuais (tema Compose, janela e barras de sistema), sem exceções que degradem a experiência, e esse comportamento é verificado por testes.

## ADDED Requirements

### Requirement: Follow the system theme

The app SHALL render its UI according to the device's system theme: dark surfaces and light-on-dark text when the system uses dark mode, and light surfaces with dark-on-light text when the system uses light mode.

#### Scenario: System in dark mode
- **WHEN** the device's system theme is dark
- **THEN** the app SHALL use the dark color scheme in every screen (client list, client form, client detail, photo viewer controls, dialogs)
- **AND** the window background SHALL be dark

#### Scenario: System in light mode
- **WHEN** the device's system theme is light
- **THEN** the app SHALL use the light color scheme in every screen
- **AND** the window background SHALL be light

#### Scenario: Theme switch at runtime
- **WHEN** the user changes the system theme while the app is open or in the background
- **THEN** the app SHALL update its UI to the new theme without requiring a restart

### Requirement: Theme-consistent launch window

When the app starts, the window background SHALL match the system theme from the first frame, so launching in dark mode never flashes a light window.

#### Scenario: Launch in dark mode
- **WHEN** the app is launched while the system uses dark mode
- **THEN** the activity window SHALL appear dark from the start

### Requirement: Adaptive system bar icons

The status bar and navigation bar icon appearance SHALL adapt to the system theme: dark icons on a light background and light icons on a dark background.

#### Scenario: Dark mode system bars
- **WHEN** the system theme is dark
- **THEN** the status bar and navigation bar icons SHALL be light

#### Scenario: Light mode system bars
- **WHEN** the system theme is light
- **THEN** the status bar and navigation bar icons SHALL be dark

### Requirement: Intentional visual exceptions

Screens that are dark by design SHALL stay dark regardless of the theme, and map tiles SHALL keep the map provider's rendering.

#### Scenario: Photo viewer
- **WHEN** the photo viewer is shown in either system theme
- **THEN** its background SHALL remain black with light controls

#### Scenario: Map tiles
- **WHEN** the map is shown in either system theme
- **THEN** the tile rendering SHALL be provided by the map library and SHALL NOT be forced to a theme color

### Requirement: Theme behavior is verified by tests

The theme behavior SHALL be covered by automated tests that assert the dark/light color schemes are selected according to the system theme.

#### Scenario: Dark scheme selection
- **WHEN** a test configures dark mode
- **THEN** the theme logic SHALL select the dark color scheme

#### Scenario: Light scheme selection
- **WHEN** a test configures light mode
- **THEN** the theme logic SHALL select the light color scheme

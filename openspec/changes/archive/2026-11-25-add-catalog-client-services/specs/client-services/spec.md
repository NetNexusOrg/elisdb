# Spec Delta

## Purpose

Records which catalog devices and treatments are installed at each client, including the installed quantity for devices, and presents that information in the client screens.

## ADDED Requirements

### Requirement: Client service association

The system SHALL associate catalog items with a client. A device item SHALL carry an installed quantity between 1 and 9999; a treatment item SHALL carry no quantity. The same catalog item SHALL NOT be associated twice with the same client, and a client SHALL NOT have more than 200 associations.

#### Scenario: Associate a device with a quantity

- **WHEN** the technician adds a device model to a client and enters a quantity between 1 and 9999
- **THEN** the association SHALL be stored with that quantity

#### Scenario: Associate a treatment

- **WHEN** the technician adds a treatment to a client
- **THEN** the association SHALL be stored without a quantity

#### Scenario: Reject an invalid device quantity

- **WHEN** a device quantity is zero, negative, above 9999 or not a whole number
- **THEN** the system SHALL reject the value
- **AND** SHALL inform the technician that the quantity is invalid

#### Scenario: Reject a treatment quantity

- **WHEN** a treatment association carries a quantity
- **THEN** the system SHALL reject the association

#### Scenario: No duplicate association

- **WHEN** the technician tries to add an item already associated with the client
- **THEN** the system SHALL prevent the duplicate
- **AND** the existing association SHALL remain unchanged

#### Scenario: Association limit

- **WHEN** a client already has 200 associations and another item is added
- **THEN** the system SHALL reject the addition
- **AND** SHALL inform the technician of the limit

### Requirement: Client services in the create/edit form

The client create/edit form SHALL include an "Aparelhos e tratamentos" section after the tags and before the location. The section SHALL offer the four categories and, for each, the active catalog items not yet associated with the client. Choosing an item SHALL append a fixed row; device rows SHALL have a numeric quantity field and treatment rows SHALL not.

#### Scenario: Section position

- **WHEN** the client form is shown
- **THEN** the "Aparelhos e tratamentos" section SHALL appear after the tags
- **AND** before the location

#### Scenario: Choose a category

- **WHEN** the technician taps one of the four category buttons
- **THEN** the form SHALL show the active items of that category that are not already associated
- **AND** when no item is available it SHALL show an empty-category message

#### Scenario: Add a device row

- **WHEN** the technician chooses a device model
- **THEN** a row SHALL be appended with the model name and a numeric quantity field

#### Scenario: Add a treatment row

- **WHEN** the technician chooses a treatment
- **THEN** a row SHALL be appended with the treatment name and no quantity field

#### Scenario: Remove a row

- **WHEN** the technician taps the remove action of an association row
- **THEN** the association SHALL be removed from the form
- **AND** the item SHALL become available again in its category list

#### Scenario: Create a client with services

- **WHEN** the technician saves a new client with associations
- **THEN** the client and its associations SHALL be stored together
- **AND** the user SHALL return to the client list

#### Scenario: Edit a client's services

- **WHEN** the technician adds, removes or changes the quantity of associations and saves
- **THEN** the stored associations SHALL reflect the change
- **AND** the user SHALL return to the client detail

### Requirement: Client services on the detail screen

The client detail screen SHALL show a read-only "Aparelhos e Tratamentos" section after the notes and before the photos. It SHALL group associations by category in the fixed order Roedores, Insetos Rastejantes, Insetos Voadores, Tratamentos, and SHALL omit empty groups. Each group SHALL show its total, and each device row SHALL show its installed quantity.

#### Scenario: Section position

- **WHEN** a client detail screen is shown
- **THEN** the "Aparelhos e Tratamentos" section SHALL appear after the notes
- **AND** before the photo gallery

#### Scenario: Show category totals

- **WHEN** a client has devices in a category
- **THEN** the category header SHALL show the sum of their quantities
- **AND** the category's rows SHALL show each device name and quantity

#### Scenario: Treatments show presence only

- **WHEN** a client has treatments
- **THEN** the Tratamentos group SHALL show the number of treatments
- **AND** each treatment row SHALL show no quantity

#### Scenario: Empty groups are omitted

- **WHEN** a client has no associations in a category
- **THEN** that category SHALL NOT be shown

#### Scenario: No associations

- **WHEN** a client has no associations
- **THEN** the section SHALL show "Sem aparelhos ou tratamentos."

#### Scenario: Inactive item installed

- **WHEN** a client has an item that is now inactive
- **THEN** the item SHALL still be shown and counted
- **AND** it SHALL NOT carry an inactive label

#### Scenario: Item not available locally

- **WHEN** a client association refers to a catalog item not stored on the device
- **THEN** that association SHALL NOT be shown
- **AND** SHALL NOT be counted in the totals

### Requirement: Atomic persistence of client and services

Saving a client and its associations SHALL be atomic: either both the client and the complete association set are persisted, or neither is. Replacing associations SHALL remove missing ones and add or update the supplied ones in the same transaction. A client whose associations changed SHALL be marked pending and SHALL NOT have its associations overwritten by a pull until the change is acknowledged.

#### Scenario: Save commits both parts

- **WHEN** a client and its associations are saved successfully
- **THEN** the client fields and the complete association set SHALL be readable together

#### Scenario: Save failure leaves previous data

- **WHEN** saving the client or the associations fails
- **THEN** the previous client fields and associations SHALL remain unchanged

#### Scenario: Pending client protects associations

- **WHEN** a client has local association changes not yet acknowledged by the server
- **THEN** a pull SHALL NOT replace that client's associations
- **AND** the pending change SHALL be pushed before the client is reconciled

### Requirement: Client services synchronization

Client associations SHALL be pushed and pulled together with their client. A push SHALL replace the server's association set atomically with the client when associations are supplied, SHALL preserve the server's associations when the field is absent, and SHALL clear them when an empty set is supplied.

#### Scenario: Push associations with the client

- **WHEN** a client is pushed with associations
- **THEN** the server SHALL store the client and the complete association set atomically

#### Scenario: Absent associations are preserved

- **WHEN** an older app pushes a client without an associations field
- **THEN** the server SHALL preserve the associations it already has for that client

#### Scenario: Empty associations clear the set

- **WHEN** a client is pushed with an empty association set
- **THEN** the server SHALL remove all of that client's associations
- **AND** keep the client itself

#### Scenario: Pull replaces associations

- **WHEN** a pull returns a client with associations
- **THEN** the app SHALL replace that client's local associations with the returned set in the same transaction as the client

#### Scenario: Pull without associations preserves local ones

- **WHEN** a pull returns a client without an associations field
- **THEN** the app SHALL preserve the client's local associations

#### Scenario: Server validation rejects a bad association

- **WHEN** a push contains an unknown item, an invalid quantity, a treatment with quantity or a duplicate item
- **THEN** the server SHALL reject the whole client record
- **AND** SHALL NOT partially store the associations

### Requirement: Client service changes in the client history

The system SHALL record changes to a client's associations in that client's history, showing items added, removed and quantity changes, with the item names captured at the time of the change. Entries recorded before this feature SHALL remain readable.

#### Scenario: Added and removed items are shown

- **WHEN** a save adds or removes associations
- **THEN** the client history entry SHALL list the added and removed item names

#### Scenario: Quantity change is shown

- **WHEN** a save changes a device quantity
- **THEN** the client history entry SHALL show the previous and new quantities

#### Scenario: Old entries remain readable

- **WHEN** the client history contains an entry recorded before service changes were supported
- **THEN** the entry SHALL keep its existing content
- **AND** SHALL NOT show an empty service section

### Requirement: Service compatibility with older clients and servers

An older app SHALL ignore association data it does not understand. A new app SHALL keep operating when the server does not support associations, and SHALL NOT lose local associations when that capability is unavailable.

#### Scenario: Older app receives associations

- **WHEN** an older app pulls a client that carries associations
- **THEN** it SHALL ignore the associations
- **AND** SHALL continue to store and synchronize the client

#### Scenario: New app against an older server

- **WHEN** the server does not accept or return associations
- **THEN** the app SHALL keep local associations
- **AND** SHALL continue synchronizing the client's fields

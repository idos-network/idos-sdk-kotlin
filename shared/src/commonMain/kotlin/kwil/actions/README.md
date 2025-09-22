# Kwil Actions

This module provides a type-safe way to interact with Kwil actions.

## Structure

- `generated/` - Contains all generated code
  - `GeneratedAction.kt` - Base interface for all actions
  - `main/` - Actions in the 'main' namespace
    - `ActionName.kt` - Per-action models and descriptors
- `ActionExtensions.kt` - Handwritten extensions for better DX

## Usage Example

### Basic Usage

```kotlin
// Use the generic call function
val grants = client.callAction(
    GetAccessGrantsForCredential,
    GetAccessGrantsForCredentialInput("cred-123")
)

// With error handling
val result = client.getAccessGrantsForCredential("cred-123")
result.onSuccess { grants ->
    // Handle success
}.onFailure { error ->
    // Handle error
}
```

### Adding a New Action

1. Create a new file in the appropriate namespace directory (e.g., `generated/main/`)
2. Define input/output models with `@Serializable`
3. Create an object implementing `GeneratedAction<I, O>`
4. Add extension functions in `ActionExtensions.kt`

## Generated vs Handwritten Code

- **Generated Code** (in `generated/`):
  - Pure data models (input/output)
  - Action descriptors with parameter metadata
  - No business logic
  - Can be regenerated from schema

- **Handwritten Code** (in root `actions/`):
  - Extension functions for better DX
  - Error handling wrappers
  - Cross-action utilities
  - Custom serialization if needed


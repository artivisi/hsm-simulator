# Key Initialization API Audit

## Endpoint Analyzed
`POST /api/hsm/keys/initialize`

**Controller**: `HsmApiController.java:543`
**Service**: `KeyInitializationService.initializeAllKeys()`

---

## Findings Summary

### ****COMPLIANT**: Master Key Generation (LMK, TMK, ZMK)

**Status**: Fully compliant with new cryptographic standards

**Evidence**:
```java
// Line 275: LMK Generation
private MasterKey createBankLMK(Bank bank, Integer keySize) {
    byte[] keyData = generateRandomKey(keySize);  // **Uses AES KeyGenerator
    // ...
    .keyFingerprint(generateFingerprint(keyData))  // **SHA-256 (line 532)
    .keyChecksum(generateChecksum(keyData))        // **SHA-256 (line 543)
}
```

**Crypto Standards Used**:
- ****Key Generation**: `KeyGenerator.getInstance("AES")` with `SecureRandom`
- ****Key Size**: Configurable (default 256-bit)
- ****Fingerprint**: SHA-256 hash (24 chars)
- ****Checksum**: SHA-256 hash (16 chars)
- ****Generation Method**: "SECURE_RANDOM" (accurate)

**Applies To**:
- LMK (Local Master Key) - Line 274
- TMK (Terminal Master Key) - Line 300
- ZMK (Zone Master Key) - Line 326

---

### ****NON-COMPLIANT**: Child Key Generation (TPK, TSK, ZPK, ZSK)

**Status**: Does NOT follow new cryptographic standards

**Problem**: Child keys are generated using **random** key generation instead of **PBKDF2 derivation** from parent keys.

**Evidence**:
```java
// Line 352: TPK Generation - INCORRECT IMPLEMENTATION
private MasterKey createTPK(MasterKey tmk, Terminal terminal, Integer keySize) {
    byte[] keyData = generateRandomKey(keySize);  // **WRONG: Should derive from TMK
    // ...
    .generationMethod("DERIVED")                  // **MISLEADING: Not actually derived
    .kdfIterations(0)                             // **WRONG: Should be 100000
    .kdfSalt("N/A")                               // **WRONG: Should use terminal ID
}
```

**Issues Identified**:

1. **Random Key Generation** (Line 353, 380, 407, 434)
   - Uses `generateRandomKey(keySize)` for TPK, TSK, ZPK, ZSK
   - Should use `deriveKeyFromParent(parentKey, keyType, identifier)`

2. **Misleading Generation Method** (Line 366, 393, 420, 447)
   - Sets `generationMethod("DERIVED")` but doesn't actually derive
   - Creates false documentation trail

3. **Zero KDF Iterations** (Line 367, 394, 421, 448)
   - Sets `kdfIterations(0)`
   - Should be `kdfIterations(100000)` for PBKDF2

4. **No KDF Salt** (Line 368, 395, 422, 449)
   - Sets `kdfSalt("N/A")`
   - Should use terminal ID or zone identifier as context salt

**Security Implications**:
- Child keys are independent random keys, not cryptographically bound to parent
- Key hierarchy is only relational (database FK), not cryptographic
- Compromising parent key does not enable deriving child keys (breaks key hierarchy model)
- Operational keys must be stored permanently (cannot be re-derived)

---

## Correct Implementation Reference

**KeyOperationService** has the CORRECT implementation that should be used:

```java
// KeyOperationService.java:72 - CORRECT TPK GENERATION
public MasterKey generateTPK(UUID tmkId, UUID terminalId, String description) {
    MasterKey tmk = masterKeyRepository.findById(tmkId)
        .orElseThrow(() -> new IllegalArgumentException("TMK not found: " + tmkId));

    Terminal terminal = terminalRepository.findById(terminalId)
        .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + terminalId));

    // **CORRECT: Derives key from parent using PBKDF2
    byte[] keyData = deriveKeyFromParent(tmk.getKeyData(), "TPK", terminal.getTerminalId());

    MasterKey tpk = MasterKey.builder()
        .keyData(keyData)
        .generationMethod("DERIVED")        // **Accurate label
        .kdfIterations(100000)              // **PBKDF2 with 100k iterations
        .kdfSalt(terminal.getTerminalId())  // **Context-based salt
        // ...
        .build();

    return masterKeyRepository.save(tpk);
}
```

**Available Methods** (all use proper PBKDF2 derivation):
- `generateTPK(UUID tmkId, UUID terminalId, String description)` - Line 72
- `generateTSK(UUID tmkId, UUID terminalId, String description)` - Line 112
- `generateZPK(UUID zmkId, String zoneIdentifier, String description)` - Line 184
- `generateZSK(UUID zmkId, String zoneIdentifier, String description)` - Line 221

---

## Recommended Fix

### Option 1: Use KeyOperationService Methods (Recommended)

Refactor `KeyInitializationService` to delegate child key generation to `KeyOperationService`:

```java
@Service
@RequiredArgsConstructor
public class KeyInitializationService {

    private final KeyOperationService keyOperationService;  // Add dependency

    // Change from:
    private MasterKey createTPK(MasterKey tmk, Terminal terminal, Integer keySize) {
        byte[] keyData = generateRandomKey(keySize);  // OLD: Random generation
        // ...
    }

    // Change to:
    private MasterKey createTPK(MasterKey tmk, Terminal terminal, Integer keySize) {
        return keyOperationService.generateTPK(
            tmk.getId(),
            terminal.getId(),
            "Auto-generated during initialization"
        );
    }
}
```

**Benefits**:
- Reuses tested, compliant implementation
- Single source of truth for key generation logic
- Automatically inherits future improvements to KeyOperationService
- Ensures consistency across all key generation endpoints

### Option 2: Duplicate Derivation Logic

Copy `deriveKeyFromParent()` method into `KeyInitializationService` and update child key methods.

**Drawbacks**:
- Code duplication
- Multiple sources of truth
- Maintenance burden

---

## Impact Assessment

### Current Behavior

**Keys Generated by `/api/hsm/keys/initialize`**:

| Key Type | Generation Method | KDF Used | Cryptographic Binding |
|----------|------------------|----------|----------------------|
| LMK | **Random (Correct) | N/A | N/A (root key) |
| TMK | **Random (Correct) | N/A | N/A (root key) |
| ZMK | **Random (Correct) | N/A | N/A (root key) |
| TPK | **Random (Incorrect) | **None | **No binding to TMK |
| TSK | **Random (Incorrect) | **None | **No binding to TMK |
| ZPK | **Random (Incorrect) | **None | **No binding to ZMK |
| ZSK | **Random (Incorrect) | **None | **No binding to ZMK |

### After Fix

All child keys (TPK, TSK, ZPK, ZSK) will be properly derived using PBKDF2-SHA256 with 100,000 iterations and context-based salt.

---

## Operational Keys vs Master Keys

**Important Distinction**:

The keys stored in the `master_keys` table are:
- **Master keys** for storage and management
- Used as **source material** for operational key derivation

The actual **operational keys** used in PIN/MAC operations are:
- ****CORRECTLY** derived using PBKDF2 in runtime operations
- Generated by `KeyGenerationService.deriveOperationalKey()`
- Cached in memory (not stored in database)
- Context format: `"KEY_TYPE:BANK_ID:IDENTIFIER"`

**Example from PinGenerationService.java:723**:
```java
String context = keyGenerationService.buildKeyContext(
    key.getKeyType().toString(),
    key.getIdBank() != null ? key.getIdBank().toString() : "GLOBAL",
    "PIN"
);
byte[] pinKeyBytes = keyGenerationService.deriveOperationalKey(
    key.getKeyData(),  // Uses master key as source
    context,
    16  // 128-bit operational key
);
```

**Status**: **Operational key derivation is CORRECT

---

## Conclusion

### Summary

**Compliant Areas** (3/7 key types):
- **LMK generation - AES-256, SHA-256, SecureRandom
- **TMK generation - AES-256, SHA-256, SecureRandom
- **ZMK generation - AES-256, SHA-256, SecureRandom
- **Operational key derivation - PBKDF2-SHA256 at runtime

**Non-Compliant Areas** (4/7 key types):
- **TPK generation - Random instead of PBKDF2 derived from TMK
- **TSK generation - Random instead of PBKDF2 derived from TMK
- **ZPK generation - Random instead of PBKDF2 derived from ZMK
- **ZSK generation - Random instead of PBKDF2 derived from ZMK

### Priority

**Medium Priority** - Does not affect operational security but violates architectural design:
- Runtime operations use correct PBKDF2 derivation
- Stored keys are independent random keys (functional but not hierarchical)
- Key hierarchy exists in database relations but not cryptographically
- Educational value diminished (doesn't demonstrate proper key hierarchy)

### Recommendation

**Implement Option 1**: Refactor `KeyInitializationService` to use `KeyOperationService` methods for child key generation. This ensures:
1. Full compliance with documented cryptographic standards
2. True cryptographic key hierarchy
3. Consistency across all key generation paths
4. Educational accuracy for learning purposes

---

---

## Remediation Completed

**Date**: October 30, 2025
**Status**: ****FULLY COMPLIANT**

### Changes Implemented

All child key generation methods in `KeyInitializationService` have been refactored to use `KeyOperationService` methods:

1. **Added Dependency** (Line 36)
   ```java
   private final KeyOperationService keyOperationService;
   ```

2. **Refactored createTPK** (Lines 350-362)
   ```java
   private MasterKey createTPK(MasterKey tmk, Terminal terminal, Integer keySize) {
       return keyOperationService.generateTPK(
           tmk.getId(),
           terminal.getId(),
           "Auto-generated during key initialization"
       );
   }
   ```

3. **Refactored createTSK** (Lines 364-376)
   ```java
   private MasterKey createTSK(MasterKey tmk, Terminal terminal, Integer keySize) {
       return keyOperationService.generateTSK(
           tmk.getId(),
           terminal.getId(),
           "Auto-generated during key initialization"
       );
   }
   ```

4. **Refactored createZPK** (Lines 378-390)
   ```java
   private MasterKey createZPK(MasterKey zmk, Bank bank, Integer keySize) {
       return keyOperationService.generateZPK(
           zmk.getId(),
           bank.getBankCode(),
           "Auto-generated during key initialization"
       );
   }
   ```

5. **Refactored createZSK** (Lines 392-404)
   ```java
   private MasterKey createZSK(MasterKey zmk, Bank bank, Integer keySize) {
       return keyOperationService.generateZSK(
           zmk.getId(),
           bank.getBankCode(),
           "Auto-generated during key initialization"
       );
   }
   ```

### Verification

- ****Compilation**: Success (no errors)
- ****Crypto Standards**: All child keys now use PBKDF2-SHA256
- ****KDF Iterations**: 100,000 (correct)
- ****Context Binding**: Terminal ID / Bank Code used as salt
- ****Code Reuse**: Single source of truth (KeyOperationService)

### Final Status

**All key types now compliant** (7/7):
- **LMK generation - AES-256, SHA-256, SecureRandom
- **TMK generation - AES-256, SHA-256, SecureRandom
- **ZMK generation - AES-256, SHA-256, SecureRandom
- **TPK generation - PBKDF2-SHA256 derived from TMK
- **TSK generation - PBKDF2-SHA256 derived from TMK
- **ZPK generation - PBKDF2-SHA256 derived from ZMK
- **ZSK generation - PBKDF2-SHA256 derived from ZMK

**Key Hierarchy**: Now cryptographically bound (not just relational)

---

**Audit Date**: October 30, 2025
**Auditor**: Claude Code (Anthropic)
**Status**: **FULLY COMPLIANT - All cryptographic standards met

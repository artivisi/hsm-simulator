# Skenario Pengujian PIN Block Generation dan Validation HSM Simulator

Dokumen ini berisi skenario pengujian untuk fitur PIN block generation dan validation pada HSM Simulator. Skenario ini dirancang untuk keperluan edukasi dan demonstrasi proses enkripsi PIN block yang digunakan dalam sistem perbankan.

## Ringkasan Skenario

### Konfigurasi PIN Block Operations
- **Format PIN Block**: ISO-0, ISO-1, ISO-2, ISO-3
- **Panjang PIN**: 4-12 digit
- **PAN (Primary Account Number)**: 16 digit
- **Encryption Key**: ZMK (Zone Master Key) atau TMK (Terminal Master Key)
- **Interface**: Web HSM Simulator
- **Tujuan**: Pembelajaran dan demonstrasi

### Fitur Khusus
- Visualisasi proses PIN block generation (selalu aktif)
- Penjelasan format-format PIN block yang berbeda
- Simulasi enkripsi dan dekripsi PIN block
- Validasi PIN block yang valid/invalid
- Step-by-step process explanation untuk setiap operasi

---

## 1. Skenario Generate PIN Block

### Test Case 1.1: Setup PIN Block Generation
**ID**: TC-PINBLK-001
**Deskripsi**: Memverifikasi setup awal PIN block generation melalui web interface
**Prasyarat**:
- Pengguna login ke HSM Simulator
- Master key sudah tersedia di HSM
- Menu PIN Block Operations tersedia

**Langkah-langkah**:
1. Login ke HSM Simulator
2. Navigasi ke menu "PIN Operations" → "Generate PIN Block"
3. System menampilkan form PIN block generation:
   - PAN field (16 digit)
   - PIN field (4-12 digit)
   - PIN Block Format dropdown (ISO-0, ISO-1, ISO-2, ISO-3)
   - Encryption Key selection
4. System otomatis menampilkan penjelasan educational (mode selalu aktif)

**Expected Result**:
- Form PIN block generation tampil dengan benar
- Format PIN block options tersedia
- Educational content otomatis ditampilkan (selalu aktif)
- Field validation ready

### Test Case 1.2: Generate PIN Block Format ISO-0
**ID**: TC-PINBLK-002
**Deskripsi**: Generate PIN block menggunakan format ISO-0
**Prasyarat**: Form PIN block generation tersedia
**Langkah-langkah**:
1. Input PAN: `1234567890123456`
2. Input PIN: `1234`
3. Pilih PIN Block Format: `ISO-0`
4. Pilih Encryption Key: `ZMK-001`
5. System otomatis menampilkan step-by-step process (mode pembelajaran selalu aktif):
6. Process yang ditampilkan:
   - PIN: 1234 → 041234F (format padded)
   - PAN: 1234567890123456 → 567890123456 (digit 5-15 + F)
   - PIN Block: 041234F XOR 567890123456F = hasil XOR
7. Klik "Generate PIN Block"
8. Review hasil:
   - Clear PIN Block: [hasil XOR dalam hex]
   - Encrypted PIN Block: [hasil enkripsi]
   - Key Check Value: [KCV]

**Expected Result**:
- PIN block berhasil di-generate
- Step-by-step process otomatis ditampilkan (selalu aktif)
- Clear dan encrypted PIN block tampil
- Format calculation sesuai ISO-0 standard
- Educational explanation selalu tersedia

### Test Case 1.3: Generate PIN Block Format ISO-1
**ID**: TC-PINBLK-003
**Deskripsi**: Generate PIN block menggunakan format ISO-1
**Prasyarat**: Form PIN block generation tersedia
**Langkah-langkah**:
1. Input PAN: `9876543210987654`
2. Input PIN: `567890`
3. Pilih PIN Block Format: `ISO-1`
4. Pilih Encryption Key: `ZMK-001`
5. System otomatis menampilkan process
6. System otomatis menampilkan educational explanation:
   - ISO-1 uses PAN digit 5-15 (11 digits)
   - PIN padded to 16 characters with "F"
   - XOR operation between PIN and PAN
7. Klik "Generate PIN Block"
8. Review hasil dan format explanation

**Expected Result**:
- PIN block ISO-1 berhasil di-generate
- Perbedaan format ISO-0 vs ISO-1 dijelaskan
- Educational content untuk ISO-1 format tersedia
- Calculation steps valid

### Test Case 1.4: Generate PIN Block Format ISO-2
**ID**: TC-PINBLK-004
**Deskripsi**: Generate PIN block menggunakan format ISO-2
**Prasyarat**: Form PIN block generation tersedia
**Langkah-langkah**:
1. Input PAN: `4111111111111111`
2. Input PIN: `9876`
3. Pilih PIN Block Format: `ISO-2`
4. System otomatis menampilkan educational explanation:
   - ISO-2 uses PAN digit 4-15 (12 digits)
   - Different padding mechanism
   - Special handling for different PAN lengths
5. Klik "Generate PIN Block"

**Expected Result**:
- PIN block ISO-2 berhasil di-generate
- Perbedaan format ISO-2 dijelaskan
- Educational content tersedia

### Test Case 1.5: Generate PIN Block Format ISO-3
**ID**: TC-PINBLK-005
**Deskripsi**: Generate PIN block menggunakan format ISO-3
**Prasyarat**: Form PIN block generation tersedia
**Langkah-langkah**:
1. Input PAN: `5555555555554444`
2. Input PIN: `2468`
3. Pilih PIN Block Format: `ISO-3`
4. System otomatis menampilkan educational explanation:
   - ISO-3 uses different PAN selection
   - Alternative padding method
   - Security considerations
5. Klik "Generate PIN Block"

**Expected Result**:
- PIN block ISO-3 berhasil di-generate
- Penjelasan format ISO-3 tersedia
- Educational content lengkap

### Test Case 1.6: Validation Input Generate PIN Block
**ID**: TC-PINBLK-006
**Deskripsi**: Validasi input field untuk generate PIN block
**Prasyarat**: Form PIN block generation tersedia
**Langkah-langkah**:
1. Test invalid PAN inputs:
   - Kosong
   - < 16 digit
   - > 16 digit
   - Mengandung huruf
   - Mengandung karakter khusus
2. Test invalid PIN inputs:
   - Kosong
   - < 4 digit
   - > 12 digit
   - Mengandung huruf
   - Sequential: "1234", "4321"
3. Test invalid format selection:
   - Tidak memilih format
   - Format tidak tersedia
4. Test invalid key selection:
   - Tidak memilih key
   - Key tidak valid

**Expected Result**:
- System reject input yang tidak valid
- Error message jelas dan spesifik
- Real-time validation feedback
- Prevent submission dengan data invalid

---

## 2. Skenario Validate PIN Block

### Test Case 2.1: Setup PIN Block Validation
**ID**: TC-PINBLK-007
**Deskripsi**: Memverifikasi setup awal PIN block validation
**Prasyarat**:
- Pengguna login ke HSM Simulator
- Master key tersedia
- PIN block data tersedia untuk testing

**Langkah-langkah**:
1. Login ke HSM Simulator
2. Navigasi ke menu "PIN Operations" → "Validate PIN Block"
3. System menampilkan form validation:
   - Encrypted PIN Block field
   - PAN field
   - Expected PIN field (untuk comparison)
   - PIN Block Format selection
   - Encryption Key selection
4. System otomatis menampilkan educational content (mode selalu aktif)

**Expected Result**:
- Form PIN block validation tampil dengan benar
- Educational mode selalu aktif
- Field validation ready

### Test Case 2.2: Validate Valid PIN Block
**ID**: TC-PINBLK-008
**Deskripsi**: Validasi PIN block yang valid
**Prasyarat**: Form PIN block validation tersedia
**Langkah-langkah**:
1. Input data yang valid:
   - Encrypted PIN Block: [hasil dari generate sebelumnya]
   - PAN: `1234567890123456`
   - Expected PIN: `1234`
   - Format: `ISO-0`
   - Key: `ZMK-001`
2. System otomatis menampilkan validation process
3. Review educational steps:
   - Decrypt PIN block dengan ZMK
   - Extract PIN dari clear PIN block
   - Compare dengan expected PIN
   - Validation result
4. Klik "Validate PIN Block"
5. Review hasil:
   - Validation Status: VALID ✓
   - Extracted PIN: 1234
   - Match with Expected: ✓
   - Process Timeline

**Expected Result**:
- PIN block valid berhasil di-identifikasi
- Step-by-step validation process ditampilkan
- Educational explanation untuk validation steps
- Match result jelas

### Test Case 2.3: Validate Invalid PIN Block
**ID**: TC-PINBLK-009
**Deskripsi**: Validasi PIN block yang invalid
**Prasyarat**: Form PIN block validation tersedia
**Langkah-langkah**:
1. Input invalid data:
   - Encrypted PIN Block: [data terenkripsi salah]
   - PAN: `1234567890123456`
   - Expected PIN: `9999`
   - Format: `ISO-0`
   - Key: `ZMK-001`
2. Klik "Validate PIN Block"
3. Review hasil:
   - Validation Status: INVALID ✗
   - Error Details: [penjelasan error]
   - Debug Information: [untuk pembelajaran]

**Expected Result**:
- System detect PIN block yang invalid
- Error information jelas dan edukatif
- Debug information untuk pembelajaran
- Security considerations dijelaskan

### Test Case 2.4: Validate dengan Wrong Format
**ID**: TC-PINBLK-010
**Deskripsi**: Validasi PIN block dengan format yang salah
**Prasyarat**: Form PIN block validation tersedia
**Langkah-langkah**:
1. Generate PIN block dengan format ISO-0
2. Coba validasi dengan format ISO-1
3. Review error message dan explanation
4. Test dengan semua kombinasi format mismatch

**Expected Result**:
- System detect format mismatch
- Penjelasan perbedaan format
- Educational content tentang format compatibility

### Test Case 2.5: Validate dengan Wrong Key
**ID**: TC-PINBLK-011
**Deskripsi**: Validasi PIN block dengan key yang salah
**Prasyarat**: Form PIN block validation tersedia
**Langkah-langkah**:
1. Generate PIN block dengan ZMK-001
2. Coba validasi dengan ZMK-002
3. Review decryption error
4. Review educational content tentang key management

**Expected Result**:
- System detect wrong key usage
- Penjelasan key management
- Error handling yang tepat

### Test Case 2.6: Batch PIN Block Validation
**ID**: TC-PINBLK-012
**Deskripsi**: Validasi multiple PIN blocks sekaligus
**Prasyarat**: Multiple PIN block data tersedia
**Langkah-langkah**:
1. Navigasi ke "Batch Validation"
2. Upload file CSV berisi:
   - Encrypted PIN Block
   - PAN
   - Expected PIN
   - Format
3. Klik "Start Batch Validation"
4. Monitor progress:
   - Total records: X
   - Processed: Y
   - Valid: Z
   - Invalid: W
5. Review hasil dalam table format
6. Download validation report

**Expected Result**:
- Batch validation berhasil dijalankan
- Real-time progress monitoring
- Comprehensive result report
- Export capability

---

## 3. Skenario Educational Features

### Test Case 3.1: PIN Block Format Comparison
**ID**: TC-PINBLK-013
**Deskripsi**: Membandingkan berbagai PIN block formats
**Prasyarat**: Educational mode aktif
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "PIN Block Formats"
2. Input PAN yang sama: `1234567890123456`
3. Input PIN yang sama: `1234`
4. Generate untuk semua format (ISO-0, ISO-1, ISO-2, ISO-3)
5. Review comparison table:
   - Format | PAN Digits Used | Padding Method | Result | Security Level
   - ISO-0  | 5-15           | F padding      | [hex]  | Medium
   - ISO-1  | 5-15           | F padding      | [hex]  | Medium
   - ISO-2  | 4-15           | Different      | [hex]  | High
   - ISO-3  | Variable       | Special        | [hex]  | High
6. Review detailed explanation untuk setiap format

**Expected Result**:
- Format comparison table tersedia
- Educational explanation lengkap
- Interactive format selection
- Visual representation

### Test Case 3.2: Step-by-Step PIN Block Calculator
**ID**: TC-PINBLK-014
**Deskripsi**: Interactive calculator untuk PIN block generation
**Prasyarat**: Educational tools tersedia
**Langkah-langkah**:
1. Navigasi ke "PIN Block Calculator"
2. Mode: "Step-by-Step"
3. Input PAN: `1234567890123456`
4. Input PIN: `1234`
5. Klik "Start Calculation"
6. Ikuti interactive steps:
   - Step 1: PIN Formatting
     - PIN: 1234
     - Length: 4
     - Padded PIN: 041234F
   - Step 2: PAN Selection
     - PAN: 1234567890123456
     - Selected Digits: 567890123456
     - Formatted PAN: 567890123456F
   - Step 3: XOR Operation
     - 041234F XOR 567890123456F = [hasil]
     - Show binary calculation
   - Step 4: Result
     - Clear PIN Block: [hasil hex]
     - Educational notes
7. Test dengan berbagai input combinations

**Expected Result**:
- Interactive step-by-step calculator
- Visual representation per step
- Binary/hex conversion tools
- Educational notes per step

### Test Case 3.3: PIN Block Security Analysis
**ID**: TC-PINBLK-015
**Deskripsi**: Analisis keamanan PIN block
**Prasyarat**: Educational mode selalu aktif
**Langkah-langkah**:
1. Navigasi ke "Security Analysis"
2. Input PAN dan PIN
3. Generate PIN block untuk semua format
4. Review security analysis:
   - Entropy calculation
   - Pattern detection
   - Weakness identification
   - Format comparison
5. Review security recommendations:
   - Best practices
   - Common vulnerabilities
   - Protection methods

**Expected Result**:
- Security analysis report
- Vulnerability assessment
- Educational recommendations
- Interactive security tools

### Test Case 3.4: Error Analysis and Debugging
**ID**: TC-PINBLK-016
**Deskripsi**: Tools untuk analisis error PIN block
**Prasyarat**: Invalid PIN block samples tersedia
**Langkah-langkah**:
1. Navigasi ke "Debug Tools"
2. Input invalid PIN block data
3. Run error analysis:
   - Format validation
   - Length validation
   - Character validation
   - Encryption validation
4. Review detailed error report:
   - Error type
   - Location
   - Suggested fix
   - Educational explanation

**Expected Result**:
- Comprehensive error analysis
- Debugging guidance
- Educational error explanations
- Interactive debugging tools

---

## 4. Skenario Advanced Features

### Test Case 4.1: PIN Block Transformation
**ID**: TC-PINBLK-017
**Deskripsi**: Transform PIN block antar format
**Prasyarat**: Multiple PIN block samples tersedia
**Langkah-langkah**:
1. Navigasi ke "PIN Block Transformation"
2. Input source PIN block (format ISO-0)
3. Select target format (ISO-1)
4. Klik "Transform Format"
5. Review transformation process:
   - Decrypt source PIN block
   - Extract original PIN
   - Generate new format PIN block
   - Compare results
6. Test semua kombinasi format transformation

**Expected Result**:
- Format transformation successful
- Educational transformation process
- Comparison between formats
- Validation of transformation accuracy

### Test Case 4.2: PIN Block History and Audit
**ID**: TC-PINBLK-018
**Deskripsi**: Melihat history operasi PIN block
**Prasyarat**: PIN block operations sudah dilakukan
**Langkah-langkah**:
1. Navigasi ke "PIN Block History"
2. Review operation history:
   - Timestamp
   - Operation type (Generate/Validate)
   - PAN (masked)
   - Format used
   - Key used
   - Result
3. Filter history berdasarkan:
   - Date range
   - Operation type
   - Format
   - Result status
4. Export history report

**Expected Result**:
- Comprehensive operation history
- Advanced filtering capability
- Secure PAN masking
- Export functionality

---

## 5. Skenario Error Handling dan Edge Cases

### Test Case 5.1: Invalid Input Handling
**ID**: TC-PINBLK-020
**Deskripsi**: Handle various invalid input scenarios
**Prasyarat**: PIN block forms tersedia
**Langkah-langkah**:
1. Test invalid PAN formats:
   - PAN dengan checksum invalid
   - PAN dengan leading zeros
   - PAN dengan test card numbers
2. Test invalid PIN formats:
   - PIN dengan repeating digits: 1111, 2222
   - PIN dengan sequential digits: 1234, 4321
   - PIN dengan birth date patterns
3. Test boundary conditions:
   - PAN minimum/maximum length
   - PIN minimum/maximum length
4. Review error handling dan educational messages

**Expected Result**:
- Graceful error handling
- Educational error messages
- Input validation comprehensive
- Security considerations explained


---

## 6. Skenario REST API Testing

### Test Case 6.1: Generate PIN Block via API
**ID**: TC-API-001
**Deskripsi**: Generate PIN block menggunakan REST API endpoint
**Prasyarat**: HSM Simulator berjalan di `http://localhost:8080`

**Endpoint**: `POST /api/hsm/pin/generate-pinblock`

**Langkah-langkah**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/generate-pinblock \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pan": "4111111111111111",
    "pin": "1234",
    "format": "ISO-0"
  }'
```

**Expected Response**:
```json
{
  "encryptedPinBlock": "A1B2C3D4E5F6789012345678901234AB",
  "format": "ISO-0",
  "pvv": "0236",
  "keyId": "LMK-SAMPLE-001",
  "keyType": "LMK"
}
```

**Validasi**:
- Response status: 200 OK
- `encryptedPinBlock` berisi 32+ hex characters
- `pvv` berisi 4 digits
- `keyId` dan `keyType` sesuai

### Test Case 6.2: PIN Verification Method A (PIN Block Comparison)
**ID**: TC-API-002
**Deskripsi**: Verify PIN menggunakan metode perbandingan PIN block
**Prasyarat**: PIN block sudah di-generate

**Endpoint**: `POST /api/hsm/pin/verify-with-translation`

**Langkah-langkah**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-translation \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pinBlockUnderLMK": "A1B2C3D4E5F6789012345678901234AB",
    "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
    "terminalId": "TRM-ISS001-ATM-001",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0"
  }'
```

**Expected Response**:
```json
{
  "valid": true,
  "message": "PIN verified successfully",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "lmkKeyId": "LMK-SAMPLE-001",
  "tpkKeyId": "TPK-SAMPLE-001"
}
```

**Validasi**:
- Response status: 200 OK
- `valid` field sesuai dengan kondisi PIN
- Verbose logging muncul di console HSM
- Message field informatif

### Test Case 6.3: PIN Verification Method B (PVV) ⭐ Recommended
**ID**: TC-API-003
**Deskripsi**: Verify PIN menggunakan PVV method (ISO 9564)
**Prasyarat**: PVV sudah disimpan di database

**Endpoint**: `POST /api/hsm/pin/verify-with-pvv`

**Langkah-langkah**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/verify-with-pvv \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pinBlockUnderTPK": "1234567890ABCDEF1234567890ABCDEF",
    "storedPVV": "0236",
    "terminalId": "TRM-ISS001-ATM-001",
    "pan": "4111111111111111",
    "pinFormat": "ISO-0"
  }'
```

**Expected Response**:
```json
{
  "valid": true,
  "message": "PIN verified successfully using PVV",
  "method": "PVV (PIN Verification Value)",
  "terminalId": "TRM-ISS001-ATM-001",
  "pan": "4111111111111111",
  "pinFormat": "ISO-0",
  "tpkKeyId": "TPK-SAMPLE-001",
  "pvkKeyId": "LMK-SAMPLE-001",
  "storedPVV": "0236"
}
```

**Validasi**:
- Response status: 200 OK
- `valid` field sesuai
- `method` menunjukkan "PVV"
- Verbose logging menampilkan PVV calculation steps

### Test Case 6.4: Encrypt PIN with Key
**ID**: TC-API-004
**Deskripsi**: Encrypt PIN untuk card issuance
**Prasyarat**: Encryption key (LMK) tersedia

**Endpoint**: `POST /api/hsm/pin/encrypt`

**Langkah-langkah**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/encrypt \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pin": "1234",
    "accountNumber": "4111111111111111",
    "format": "ISO-0",
    "keyId": "YOUR-LMK-KEY-UUID"
  }'
```

**Expected Response**:
```json
{
  "encryptedPinBlock": "8F4A2E1D9C7B5A3E6F8D2C4B7A9E5D3C",
  "format": "ISO-0",
  "pvv": "0236"
}
```

**Validasi**:
- Response returns both encrypted PIN block AND PVV
- PVV is 4 digits
- Store PVV in database for Method B verification

### Test Case 6.5: API Error Handling
**ID**: TC-API-005
**Deskripsi**: Test error responses dari API
**Prasyarat**: HSM Simulator berjalan

**Test Invalid PIN Length**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/generate-pinblock \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pan": "4111111111111111",
    "pin": "12",
    "format": "ISO-0"
  }'
```

**Expected Response**:
```json
{
  "error": "PIN length must be between 4 and 12 digits"
}
```

**Test Non-Numeric PIN**:
```bash
curl -X POST http://localhost:8080/api/hsm/pin/generate-pinblock \
  -H "Content-Type: application/json" \
  -u admin:admin \
  -d '{
    "pan": "4111111111111111",
    "pin": "12ab",
    "format": "ISO-0"
  }'
```

**Expected Response**:
```json
{
  "error": "PIN must contain only digits"
}
```

**Validasi**:
- Proper HTTP error codes (400)
- Descriptive error messages
- Input validation working

### Test Case 6.6: Verbose Logging Verification
**ID**: TC-API-006
**Deskripsi**: Verify verbose logging output
**Prasyarat**: Application logs accessible

**Langkah-langkah**:
1. Jalankan API call untuk PVV verification
2. Monitor application console/log output
3. Verify step-by-step logging muncul:
   - Key retrieval
   - PIN block decryption
   - PIN extraction
   - PVV calculation (dengan algorithm details)
   - Comparison result

**Expected Log Output** (example):
```
========================================
PIN VERIFICATION - METHOD: PVV (PIN Verification Value)
========================================
Input Parameters:
  - PAN: 411111******1111
  - PIN Format: ISO-0
  - Stored PVV: 5672
----------------------------------------
STEP 1: Retrieve Cryptographic Keys
----------------------------------------
TPK Key Retrieved:
  - Key ID: TPK-SAMPLE-001
  - Algorithm: AES
...
========================================
VERIFICATION RESULT: SUCCESS
========================================
```

**Validasi**:
- All steps logged clearly
- Intermediate values shown (masked where appropriate)
- Algorithm details explained
- Result clearly indicated

---

## Kesimpulan

Skenario pengujian ini mencakup aspek PIN block generation dan validation untuk HSM Simulator dengan fokus pada:

1. **Functional Testing**: Generate dan validate PIN block dengan berbagai format
2. **Educational Testing**: Step-by-step processes dan interactive learning tools
3. **Error Handling**: Comprehensive error handling dengan educational messages
4. **Advanced Features**: Transformasi format dan history tracking
5. **REST API Testing**: Comprehensive API testing untuk integration dengan external systems
6. **PVV Method**: Testing untuk industry-standard PVV verification (ISO 9564)

Semua skenario dirancang untuk memberikan pembelajaran yang komprehensif tentang PIN block operations dalam konteks perbankan dan security.
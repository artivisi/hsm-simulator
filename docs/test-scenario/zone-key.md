# Skenario Pengujian Zone Key Management HSM Simulator

Dokumen ini berisi skenario pengujian khusus untuk Zone Key Management pada HSM Simulator. Skenario mencakup zone master key generation, zone session/PIN key management, dan PIN translation dengan fitur debugging.

## Ringkasan Skenario

### Konfigurasi Zone Key Management
- **Zone Types**: Support multiple zone types (Acquirer, Issuer, Switch)
- **Key Hierarchy**: Zone Master Key → Zone Session Key → PIN Key
- **Interface**: Web HSM Simulator
- **Tujuan**: Pembelajaran dan demonstrasi zone key management

### Fitur Khusus
- Zone master key generation dengan custodian involvement
- Zone session/PIN key derivation dari zone master key
- PIN translation antar zone dengan debugging capabilities
- Educational view untuk cryptographic operations

---

## 1. Skenario Zone Master Key Generation

### Test Case 1.1: Zone Creation dan Master Key Setup
**ID**: TC-ZMK-001
**Deskripsi**: Membuat zone baru dan setup zone master key
**Prasyarat**:
- Pengguna login sebagai zone administrator
- Minimal 2 key custodians tersedia
- Zone configuration parameters sudah disiapkan

**Langkah-langkah**:
1. Login ke HSM Simulator sebagai zone administrator
2. Navigasi ke menu "Zone Management" → "Create Zone"
3. Isi zone configuration form:
   - Zone ID: "ACQ-ZONE-001"
   - Zone Name: "Acquirer Network Zone"
   - Zone Type: "Acquirer"
   - Description: "Zone for acquirer network operations"
   - Key Custodians: 3 persons
   - Threshold: 2
4. Klik "Create Zone"
5. Di halaman zone master key setup, klik "Start Zone Key Ceremony"
6. Configure key ceremony parameters:
   - Algorithm: "AES-256"
   - Key Size: 256 bits
   - Ceremony Type: "Zone Master Key Generation"
7. Klik "Proceed to Custodian Setup"

**Expected Result**:
- Zone berhasil dibuat dengan unique zone ID
- Zone master key ceremony ter-initiasi
- System generate ceremony ID (contoh: ZMK-CER-2025-001)
- Navigation ke custodian setup berhasil

### Test Case 1.2: Zone Master Key Custodian Registration
**ID**: TC-ZMK-002
**Deskripsi**: Mendaftarkan custodians untuk zone master key ceremony
**Prasyarat**: Zone sudah dibuat dan ceremony di-initiasi
**Langkah-langkah**:
1. Di halaman custodian setup, klik "Add Custodian"
2. Isi data custodian pertama:
   - Name: "Zone Custodian A"
   - Email: "zone.custodian.a@example.com"
   - Phone: "+62-812-3456-7891"
   - Role: "Zone Key Custodian"
3. Klik "Save Custodian"
4. Ulangi langkah 2-3 untuk Custodian B dan C
5. Klik "Proceed to Zone Master Key Generation"

**Expected Result**:
- 3 custodians berhasil didaftarkan
- Role assignment sebagai "Zone Key Custodian" berhasil
- System siap untuk zone master key generation

### Test Case 1.3: Zone Master Key Generation Process
**ID**: TC-ZMK-003
**Deskripsi**: Generate zone master key dengan multi-party approval
**Prasyarat**: 3 custodians sudah terdaftar
**Langkah-langkah**:
1. Administrator klik "Start Zone Master Key Generation"
2. System generate unique contribution links untuk masing-masing custodian
3. System kirim email ke semua custodians:
   - Subject: "Zone Master Key Ceremony - Contribution Required - ZMK-CER-2025-001"
   - Body: Contains unique contribution link dan instructions
4. Custodian A menerima email dan berkontribusi:
   - Buka contribution link
   - Masukkan passphrase: `ZoneM@sterK3yCustodianA2025!`
   - Konfirmasi contribution
5. Custodian B berkontribusi: `Z0neB@ckupCustodianK3y!`
6. Custodian C berkontribusi: `ThirdZ0neCustodian#Secur3P@ss!`
7. System otomatis generate zone master key setelah semua kontribusi lengkap
8. System generate Shamir's Secret Sharing shares untuk recovery
9. System kirim key shares ke masing-masing custodian

**Expected Result**:
- Zone master key berhasil di-generate dari 3 custodian contributions
- Key shares untuk recovery terdistribusi ke custodians
- System update zone status: "Active - Zone Master Key Generated"
- Complete audit trail untuk zone master key generation

### Test Case 1.4: Zone Master Key Verification
**ID**: TC-ZMK-004
**Deskripsi**: Verifikasi zone master key yang sudah di-generate
**Prasyarat**: Zone master key generation completed
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Zone Details"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "View Zone Master Key Details"
4. Review zone master key information:
   - Key ID: "ZMK-ACQ-001"
   - Generation Date: [timestamp]
   - Key Fingerprint: [fingerprint]
   - Algorithm: "AES-256"
   - Key Size: 256 bits
5. Klik "Test Zone Master Key"
6. Test basic encryption/decryption dengan zone master key

**Expected Result**:
- Zone master key information ditampilkan dengan lengkap
- Basic encryption/decryption test berhasil
- Zone master key ready untuk session key generation

---

## 2. Skenario Zone Session Key dan PIN Key Management

### Test Case 2.1: Zone Session Key Generation
**ID**: TC-ZSK-001
**Deskripsi**: Generate zone session key dari zone master key
**Prasyarat**:
- Zone master key sudah aktif
- User login sebagai zone operator
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Zone Details"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "Generate Session Key"
4. Configure session key parameters:
   - Key Type: "Zone Session Key"
   - Key ID: "ZSK-ACQ-001-2025"
   - Validity Period: 24 hours
   - Purpose: "Acquirer Operations"
5. Klik "Generate Session Key"
6. Review generated session key:
   - Session Key ID: "ZSK-ACQ-001-2025"
   - Derived from: "ZMK-ACQ-001"
   - Generation Time: [timestamp]
   - Expiration Time: [timestamp + 24h]
7. Klik "Test Session Key" untuk verifikasi

**Expected Result**:
- Zone session key berhasil di-generate dari zone master key
- Session key memiliki unique ID dan validity period
- Key derivation process tercatat di audit log
- Session key test berhasil

### Test Case 2.2: PIN Key Generation
**ID**: TC-PIN-001
**Deskripsi**: Generate PIN key dari zone session key
**Prasyarat**:
- Zone session key sudah aktif
- User login sebagai PIN manager
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "PIN Management"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "Generate PIN Key"
4. Configure PIN key parameters:
   - Key Type: "PIN Encryption Key"
   - Key ID: "PIN-ACQ-001-2025"
   - PIN Block Format: "ANSI X9.8"
   - Key Usage: "PIN Encryption/Decryption"
5. Klik "Generate PIN Key"
6. Review generated PIN key:
   - PIN Key ID: "PIN-ACQ-001-2025"
   - Derived from: "ZSK-ACQ-001-2025"
   - PIN Block Format: "ANSI X9.8"
   - Generation Time: [timestamp]
7. Klik "Test PIN Key" untuk verifikasi

**Expected Result**:
- PIN key berhasil di-generate dari zone session key
- PIN key siap digunakan untuk PIN encryption/decryption
- Key hierarchy tercatat: Zone Master Key → Zone Session Key → PIN Key
- PIN key test berhasil

### Test Case 2.3: Key Hierarchy Verification
**ID**: TC-HIER-001
**Deskripsi**: Verifikasi key hierarchy dan relationship
**Prasyarat**: Zone master key, session key, dan PIN key sudah ada
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Key Hierarchy View"
2. Pilih zone "ACQ-ZONE-001"
3. Review key hierarchy:
   - Level 1: Zone Master Key (ZMK-ACQ-001)
   - Level 2: Zone Session Key (ZSK-ACQ-001-2025)
   - Level 3: PIN Key (PIN-ACQ-001-2025)
4. Klik "Verify Key Chain"
5. System verify key derivation chain:
   - ZSK → ZMK verification
   - PIN Key → ZSK verification
   - Complete chain verification
6. Review key relationship details:
   - Derivation algorithms
   - Cryptographic parameters
   - Timestamps and validity periods

**Expected Result**:
- Key hierarchy ditampilkan dengan visual representation
- Key derivation chain verification berhasil
- Relationship antar keys terverifikasi
- Complete audit trail untuk key hierarchy

---

## 3. Skenario PIN Translation

### Test Case 3.1: PIN Encryption Preparation
**ID**: TC-TRNS-001
**Deskripsi**: Menyiapkan data untuk PIN translation testing
**Prasyarat**:
- Zone PIN key sudah aktif
- PIN block format sudah dikonfigurasi
**Langkah-langkah**:
1. Navigasi ke "PIN Translation" → "Test Setup"
2. Configure test parameters:
   - Source Zone: "ACQ-ZONE-001"
   - Destination Zone: "ISS-ZONE-001"
   - PIN Block Format: "ANSI X9.8"
   - Test PIN: "1234"
3. Generate test data:
   - Account Number: "1234567890123456"
   - PIN Block: [generated PIN block]
4. Klik "Generate Test Vectors"
5. Review generated test data untuk PIN translation

**Expected Result**:
- Test data untuk PIN translation berhasil di-generate
- PIN block format sesuai dengan konfigurasi
- Test data siap untuk translation process

### Test Case 3.2: PIN Translation Process
**ID**: TC-TRNS-002
**Deskripsi**: Melakukan PIN translation antar zone
**Prasyarat**: Test data sudah disiapkan
**Langkah-langkah**:
1. Navigasi ke "PIN Translation" → "Translate PIN"
2. Input translation parameters:
   - Source Zone: "ACQ-ZONE-001"
   - Destination Zone: "ISS-ZONE-001"
   - Source PIN Key: "PIN-ACQ-001-2025"
   - Destination PIN Key: "PIN-ISS-001-2025"
   - Account Number: "1234567890123456"
   - Source PIN Block: [source PIN block]
3. Klik "Start Translation"
4. System process PIN translation:
   - Decrypt PIN block dengan source PIN key
   - Extract original PIN
   - Encrypt PIN dengan destination PIN key
   - Generate destination PIN block
5. Review translation result:
   - Original PIN: "1234"
   - Source PIN Block: [source block]
   - Destination PIN Block: [destination block]
   - Translation Status: "Success"
6. Verifikasi PIN consistency:
   - Decrypt destination PIN block
   - Compare dengan original PIN

**Expected Result**:
- PIN translation berhasil dilakukan
- PIN integrity terjaga (source PIN == destination PIN)
- Translation process tercatat di audit log
- PIN block format valid untuk kedua zone

### Test Case 3.3: PIN Translation Debug Mode
**ID**: TC-TRNS-003
**Deskripsi**: Menggunakan debug mode untuk PIN translation
**Prasyarat**: PIN translation sudah terkonfigurasi
**Langkah-langkah**:
1. Navigasi ke "PIN Translation" → "Debug Mode"
2. Enable debug features:
   - Enable "Show Intermediate Values"
   - Enable "Display Cryptographic Operations"
   - Enable "Step-by-Step Execution"
3. Jalankan PIN translation dengan debug mode:
   - Input test data yang sama dengan TC-TRNS-002
   - Klik "Debug Translation"
4. Review step-by-step process:
   - Step 1: Decrypt source PIN block
   - Step 2: Extract PIN from PIN block
   - Step 3: Validate PIN format
   - Step 4: Encrypt with destination key
   - Step 5: Generate destination PIN block
5. Analyze intermediate values:
   - Decrypted PIN block content
   - PIN extraction process
   - Cryptographic operations detail
   - Key usage verification
6. Review cryptographic operations:
   - Algorithms used for each step
   - Key derivation processes
   - Hash calculations
   - Padding schemes

**Expected Result**:
- Debug mode menampilkan detail proses PIN translation
- Intermediate values dapat dilihat untuk pembelajaran
- Step-by-step execution membantu memahami proses
- Cryptographic operations terdokumentasi dengan lengkap
- Educational value untuk memahami PIN translation

### Test Case 3.4: PIN Translation Error Handling
**ID**: TC-TRNS-004
**Deskripsi**: Handle error scenarios dalam PIN translation
**Prasyarat**: PIN translation functionality sudah tersedia
**Langkah-langkah**:
1. Test invalid PIN block format:
   - Input PIN block dengan format salah
   - Verifikasi error message
2. Test invalid account number:
   - Input account number tidak valid
   - Verifikasi validation error
3. Test key mismatch:
   - Gunakan PIN key yang tidak cocok dengan zone
   - Verifikasi key validation error
4. Test network issues:
   - Simulasikan network failure selama translation
   - Verifikasi error handling
5. Test boundary conditions:
   - PIN dengan panjang tidak standard
   - Account number dengan format khusus
   - Verifikasi handling edge cases

**Expected Result**:
- System mendeteksi dan handle PIN block format errors
- Account number validation berfungsi dengan baik
- Key validation mencegah penggunaan key yang salah
- Network issues ditangani dengan graceful
- Boundary conditions handled appropriately
- Clear error messages untuk setiap scenario

---

## 4. Skenario Zone Key Management Operations

### Test Case 4.1: Zone Key Rotation
**ID**: TC-ROT-001
**Deskripsi**: Melakukan rotation zone master key
**Prasyarat**:
- Zone sudah aktif dengan master key
- Key custodians tersedia
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Key Rotation"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "Start Zone Master Key Rotation"
4. Configure rotation parameters:
   - Rotation Reason: "Scheduled Rotation"
   - New Key Algorithm: "AES-256"
   - Keep Old Key Active: Yes (graceful transition)
5. Jalankan rotation ceremony:
   - Generate new zone master key
   - Distribute new key shares
   - Maintain old key for transition period
6. Verifikasi rotation status:
   - New key generation success
   - Old key still active for transition
   - Session key update process

**Expected Result**:
- Zone master key berhasil di-rotasi
- Graceful transition dengan old key masih aktif
- Session keys ter-update ke new master key
- Complete audit trail untuk rotation process

### Test Case 4.2: Zone Key Backup dan Recovery
**ID**: TC-BACK-001
**Deskripsi**: Backup dan recovery zone master key
**Prasyarat**:
- Zone master key sudah aktif
- Backup procedures sudah dikonfigurasi
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Key Backup"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "Generate Key Backup"
4. Configure backup parameters:
   - Backup Format: "Encrypted Archive"
   - Backup Location: "Secure Storage"
   - Encryption Key: "Backup Encryption Key"
5. Generate backup package:
   - Zone master key shares
   - Configuration data
   - Audit trail
6. Test recovery process:
   - Simulate key loss scenario
   - Use backup package for recovery
   - Verify recovered key functionality

**Expected Result**:
- Zone master key backup berhasil di-generate
- Backup package terenkripsi dengan aman
- Recovery process berhasil
- Recovered key berfungsi normal

### Test Case 4.3: Zone Key Deactivation
**ID**: TC-DEACT-001
**Deskripsi**: Menonaktifkan zone dan keys
**Prasyarat**:
- Zone tidak lagi digunakan
- Deauthorization approval sudah diperoleh
**Langkah-langkah**:
1. Navigasi ke "Zone Management" → "Zone Deactivation"
2. Pilih zone "ACQ-ZONE-001"
3. Klik "Start Zone Deactivation"
4. Configure deactivation parameters:
   - Deactivation Reason: "Zone Decommissioning"
   - Key Destruction Method: "Secure Erase"
   - Grace Period: 7 days
5. Execute deactivation process:
   - Revoke all session keys
   - Secure erase of PIN keys
   - Mark zone as inactive
6. Verify deactivation status:
   - Zone status: "Decommissioned"
   - All keys: "Securely Erased"
   - Audit log: Complete

**Expected Result**:
- Zone berhasil di-deactivate
- Semua keys ter-erase dengan aman
- Complete audit trail untuk deactivation
- Zone tidak dapat digunakan kembali

---

## 5. Skenario Educational Features

### Test Case 5.1: Zone Key Educational View
**ID**: TC-EDU-001
**Deskripsi**: Educational view untuk zone key operations
**Prasyarat**: Zone key management sudah aktif
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "Zone Key Explorer"
2. Pilih zone "ACQ-ZONE-001"
3. Explore educational features:
   - Key hierarchy visualization
   - Cryptographic operations explanation
   - Key derivation process
4. View mathematical details:
   - Key derivation algorithms
   - Cryptographic parameters
   - Security considerations
5. Interactive learning:
   - Step-by-step key generation
   - Interactive PIN translation demo
   - Practice exercises

**Expected Result**:
- Educational view menampilkan zone key operations secara visual
- Mathematical operations dijelaskan dengan jelas
- Interactive learning tools berfungsi dengan baik
- Pembelajaran zone key management menjadi lebih mudah dipahami

### Test Case 5.2: PIN Translation Visualization
**ID**: TC-VIS-001
**Deskripsi**: Visualisasi PIN translation process
**Prasyarat**: PIN translation functionality tersedia
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "PIN Translation Visualizer"
2. Configure visualization parameters:
   - Show cryptographic operations
   - Display intermediate values
   - Step-by-step mode
3. Run PIN translation with visualization:
   - Input test PIN dan account number
   - Execute translation dengan visual mode
4. Review visual representation:
   - PIN block format visualization
   - Encryption/decryption flow
   - Key usage visualization
5. Export visualization report untuk dokumentasi

**Expected Result**:
- PIN translation process divisualisasikan dengan jelas
- Cryptographic operations dapat dipahami secara visual
- Educational value untuk pembelajaran PIN translation
- Visualization report dapat di-export

---

## 6. Skenario Security dan Compliance

### Test Case 6.1: Zone Key Security Audit
**ID**: TC-AUDIT-001
**Deskripsi**: Security audit untuk zone key management
**Prasyarat**: Zone key operations sudah dilakukan
**Langkah-langkah**:
1. Navigasi ke "Security Audit" → "Zone Key Audit"
2. Pilih zone "ACQ-ZONE-001"
3. Review security aspects:
   - Key generation compliance
   - Access control verification
   - Key usage monitoring
4. Generate security report:
   - Compliance status
   - Security findings
   - Recommendations
5. Export audit report

**Expected Result**:
- Security audit report ter-generate
- Compliance status terverifikasi
- Security findings terdokumentasi
- Recommendations untuk improvement

### Test Case 6.2: Zone Key Compliance Reporting
**ID**: TC-COMP-001
**Deskripsi**: Generate compliance reports untuk zone keys
**Prasyarat**: Audit data tersedia
**Langkah-langkah**:
1. Navigasi ke "Compliance" → "Zone Key Reports"
2. Configure report parameters:
   - Report Type: "Zone Key Compliance"
   - Date Range: [relevant period]
   - Compliance Standards: PCI DSS, ISO 27001
3. Generate compliance report:
   - Key lifecycle compliance
   - Security controls verification
   - Audit trail completeness
4. Review compliance indicators:
   - Compliance status
   - Exception reports
   - Remediation actions

**Expected Result**:
- Compliance report ter-generate sesuai standar
- Key lifecycle compliance terverifikasi
- Exception reports terdokumentasi
- Remediation actions teridentifikasi

---

## 7. Skenario End-to-End PIN Verification antara Acquirer dan Issuer

### Test Case 7.1: Setup Dual HSM Environment
**ID**: TC-E2E-001
**Deskripsi**: Menyiapkan environment dengan 2 instance HSM untuk Acquirer dan Issuer
**Prasyarat**:
- Dua instance HSM Simulator running pada port berbeda
- Network connectivity antara kedua HSM instance
- Acquirer dan Issuer zone keys sudah di-generate

**Langkah-langkah**:
1. Start HSM Simulator untuk Acquirer:
   - Port: 8080
   - Zone: ACQ-ZONE-001
   - Zone Master Key: ZMK-ACQ-001
   - Zone PIN Key: PIN-ACQ-001-2025
2. Start HSM Simulator untuk Issuer:
   - Port: 8081
   - Zone: ISS-ZONE-001
   - Zone Master Key: ZMK-ISS-001
   - Zone PIN Key: PIN-ISS-001-2025
3. Verifikasi connectivity antara kedua HSM instance
4. Test basic communication antara HSM instances
5. Configure exchange parameters untuk PIN translation

**Expected Result**:
- Dua HSM instances running dengan konfigurasi yang benar
- Network connectivity antara instances terverifikasi
- Zone keys siap untuk end-to-end testing

### Test Case 7.2: PIN Entry dan Encryption di Terminal Acquirer
**ID**: TC-E2E-002
**Deskripsi**: Simulasi PIN entry dan encryption di terminal Acquirer
**Prasyarat**:
- HSM Acquirer instance aktif
- TPK (Terminal PIN Key) sudah di-load ke HSM Acquirer
- ZPK (Zone PIN Key) sudah di-generate

**Langkah-langkah**:
1. Simulasi terminal mengirim request ke HSM Acquirer:
   ```
   Request: Encrypt PIN with TPK
   Parameters:
   - Terminal ID: "TERM-ACQ-001"
   - PAN: "1234567890123456"
   - PIN: "1234" (entered by customer)
   - TPK Key ID: "TPK-TERM-001"
   - PIN Block Format: "ANSI X9.8"
   ```
2. HSM Acquirer process PIN encryption:
   - Generate PIN block dari PIN dan PAN
   - Encrypt PIN block dengan TPK
   - Return encrypted PIN block ke terminal
3. Terminal mengirim encrypted PIN block ke aplikasi Acquirer
4. Aplikasi Acquirer mengirim ke HSM Acquirer untuk translation:
   ```
   Request: Translate PIN TPK to ZPK
   Parameters:
   - Encrypted PIN Block (TPK): [encrypted_pin_block]
   - TPK Key ID: "TPK-TERM-001"
   - ZPK Key ID: "PIN-ACQ-001-2025"
   - PAN: "1234567890123456"
   ```
5. HSM Acquirer process translation:
   - Decrypt PIN block dengan TPK
   - Extract original PIN
   - Re-encrypt PIN block dengan ZPK
   - Return ZPK-encrypted PIN block

**Expected Result**:
- PIN berhasil di-encrypt dengan TPK di terminal
- PIN translation dari TPK ke ZPK berhasil di HSM Acquirer
- ZPK-encrypted PIN block siap dikirim ke Issuer

### Test Case 7.3: PIN Transmission dari Acquirer ke Issuer
**ID**: TC-E2E-003
**Deskripsi**: Simulasi transmisi PIN block dari Acquirer ke Issuer
**Prasyarat**:
- PIN block sudah di-encrypt dengan ZPK Acquirer
- ZPK exchange antara Acquirer dan Issuer sudah dikonfigurasi

**Langkah-langkah**:
1. Aplikasi Acquirer mengirim transaction request ke Issuer:
   ```
   Authorization Request:
   - MTI: "0100"
   - PAN: "1234567890123456"
   - Processing Code: "00"
   - Amount: "000000100000"
   - PIN Block (ZPK ACQ): [zpk_encrypted_pin_block]
   - Acquirer Institution ID: "12345"
   - Issuer Institution ID: "67890"
   ```
2. Network layer mengirim request ke Issuer system
3. Issuer menerima request dan menforward ke HSM Issuer
4. Issuer mengambil reference PIN block dari database:
   ```
   Database Query:
   - PAN: "1234567890123456"
   - Retrieve stored encrypted PIN block (encrypted with LMK)
   ```

**Expected Result**:
- Transaction request berhasil dikirim dari Acquirer ke Issuer
- PIN block dalam format ZPK Acquirer tersampaikan dengan aman
- Reference PIN block berhasil di-retrieve dari database Issuer

### Test Case 7.4: PIN Verification di HSM Issuer
**ID**: TC-E2E-004
**Deskripsi**: Proses PIN verification di HSM Issuer
**Prasyarat**:
- HSM Issuer instance aktif
- PIN block dari Acquirer diterima
- Reference PIN block dari database tersedia

**Langkah-langkah**:
1. Aplikasi Issuer mengirim verification request ke HSM Issuer:
   ```
   Request: Verify PIN
   Parameters:
   - Incoming PIN Block (ZPK ACQ): [incoming_pin_block]
   - ZPK ACQ Key ID: "PIN-ACQ-001-2025"
   - Reference PIN Block (from DB): [reference_pin_block]
   - LMK Key ID: "LMK-ISS-001"
   - PAN: "1234567890123456"
   - Verification Mode: "Match"
   ```
2. HSM Issuer process verification:
   - Step 1: Decrypt incoming PIN block dengan ZPK ACQ
   - Step 2: Extract PIN dari incoming PIN block
   - Step 3: Decrypt reference PIN block dengan LMK (Local Master Key)
   - Step 4: Extract reference PIN dari reference PIN block
   - Step 5: Compare PIN dan reference PIN
   - Step 6: Generate verification result
3. HSM Issuer return verification result:
   ```
   Response:
   - Verification Status: "MATCH" / "NO_MATCH"
   - Verification Code: "00" (Success) / "55" (Invalid PIN)
   - Error Details: [if applicable]
   ```
4. Issuer system membuat authorization response:
   ```
   Response to Acquirer:
   - MTI: "0110"
   - Response Code: "00" (Approved) / "55" (Declined - Invalid PIN)
   - Authorization Code: [if approved]
   ```

**Expected Result**:
- PIN verification berhasil dilakukan di HSM Issuer
- Verification result akurat (MATCH untuk PIN yang benar, NO_MATCH untuk PIN yang salah)
- Authorization response dikirim kembali ke Acquirer
- Complete audit trail untuk PIN verification process

### Test Case 7.5: Debug Mode untuk End-to-End PIN Verification
**ID**: TC-E2E-005
**Deskripsi**: Debug mode untuk end-to-end PIN verification process
**Prasyarat**:
- Debug mode diaktifkan di kedua HSM instances
- Test data yang sama seperti TC-E2E-002 hingga TC-E2E-004

**Langkah-langkah**:
1. Enable debug mode di HSM Acquirer:
   - Enable "Show PIN Block Decryption"
   - Enable "Display Key Usage"
   - Enable "Step-by-Step Processing"
2. Enable debug mode di HSM Issuer:
   - Enable "Show PIN Comparison"
   - Enable "Display Verification Process"
   - Enable "Log Cryptographic Operations"
3. Jalankan end-to-end test dengan debug mode:
   - Input test data yang sama
   - Execute complete PIN verification flow
4. Analyze debug output dari HSM Acquirer:
   ```
   Debug Output - HSM Acquirer:
   Step 1: PIN Block Generation
   - PAN: 1234567890123456
   - PIN: 1234
   - PIN Block Format: ANSI X9.8
   - Raw PIN Block: 041234FEDCBA9876
   - TPK Encrypted: A1B2C3D4E5F6...

   Step 2: TPK to ZPK Translation
   - Decrypting with TPK: SUCCESS
   - Extracted PIN: 1234
   - Re-encrypting with ZPK: SUCCESS
   - ZPK Encrypted PIN Block: X9Y8Z7W6V5U4...
   ```
5. Analyze debug output dari HSM Issuer:
   ```
   Debug Output - HSM Issuer:
   Step 1: Decrypt Incoming PIN Block
   - ZPK ACQ Decryption: SUCCESS
   - Extracted PIN: 1234
   - PIN Format: VALID

   Step 2: Decrypt Reference PIN Block
   - LMK Decryption: SUCCESS
   - Extracted Reference PIN: 1234
   - Reference PIN Format: VALID

   Step 3: PIN Comparison
   - PIN Comparison: 1234 == 1234
   - Result: MATCH
   - Verification Code: 00
   ```
6. Review cryptographic operations:
   - Key usage verification
   - Algorithm execution details
   - Security parameter validation

**Expected Result**:
- Debug mode menampilkan complete end-to-end PIN verification flow
- Step-by-step process terlihat di kedua HSM instances
- Intermediate values (PIN blocks, keys) terlihat untuk pembelajaran
- Cryptographic operations terdokumentasi dengan detail
- Educational value untuk memahami PIN verification process

### Test Case 7.6: Basic Error Scenarios dalam End-to-End PIN Verification
**ID**: TC-E2E-006
**Deskripsi**: Handle basic error scenarios dalam end-to-end PIN verification
**Prasyarat**:
- Dual HSM environment sudah siap
- Error simulation capabilities diaktifkan

**Langkah-langkah**:

#### Scenario A: Invalid PIN Entry
1. Simulasikan customer memasukkan PIN salah:
   - PIN yang dimasukkan: "9999" (salah)
   - Reference PIN di database: "1234" (benar)
2. Execute end-to-end verification
3. Verifikasi response:
   - Verification result: "NO_MATCH"
   - Response Code: "55" (Invalid PIN)
   - Error handling: Graceful rejection

#### Scenario B: Key Mismatch
1. Gunakan ZPK key yang tidak cocok:
   - Acquirer mengirim dengan ZPK ACQ
   - Issuer mencoba decrypt dengan ZPK yang berbeda
2. Verifikasi error response:
   - Key validation error
   - Decryption failure
   - Security violation logging

#### Scenario C: Corrupted PIN Block
1. Kirim PIN block yang corrupted:
   - Invalid PIN block format
   - Truncated data
   - Invalid checksum
2. Verifikasi validation:
   - Format validation failure
   - Data integrity check
   - Error reporting

**Expected Result**:
- System mendeteksi dan handle invalid PIN dengan tepat
- Key validation mencegah penggunaan key yang salah
- Data integrity validation berfungsi dengan baik
- Basic error logging dan reporting
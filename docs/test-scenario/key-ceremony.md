# Skenario Pengujian Key Ceremony HSM Simulator

Dokumen ini berisi skenario pengujian khusus untuk Key Ceremony pada HSM Simulator menggunakan web interface. Skenario mencakup inisialisasi HSM dan pemulihan master key dengan skema 3 custodian dan threshold 2-of-3.

## Ringkasan Skenario

### Konfigurasi Key Ceremony
- **Jumlah Key Custodian**: 3 orang
- **Threshold untuk Pemulihan**: 2 dari 3 custodian
- **Mekanisme**: Shamir's Secret Sharing
- **Interface**: Web HSM Simulator (bukan REST API)
- **Tujuan**: Pembelajaran dan demonstrasi

### Fitur Khusus
- View komponen kunci terenkripsi dan plaintext untuk pembelajaran
- Kalkulasi manual dengan bantuan interface
- Penjelasan matematika Shamir's Secret Sharing
- Simulasi proses key ceremony yang lengkap

---

## 1. Skenario Inisialisasi HSM (Initialization Ceremony)

### Test Case 1.1: Setup Awal Key Ceremony
**ID**: TC-KEYCER-001
**Deskripsi**: Memverifikasi setup awal key ceremony melalui web interface
**Prasyarat**:
- Pengguna login sebagai administrator
- HSM dalam keadaan fresh/factory reset
- 3 custodian siap untuk ceremony

**Langkah-langkah**:
1. Login ke HSM Simulator sebagai administrator
2. Navigasi ke menu "Key Management" → "Key Ceremony"
3. Klik tombol "Start New Ceremony"
4. Isi form ceremony setup:
   - Ceremony Name: "Initial Setup Ceremony"
   - Number of Custodians: 3
   - Threshold: 2
   - Purpose: "HSM Initialization"
5. Klik "Continue to Custodian Setup"

**Expected Result**:
- Halaman ceremony setup tampil dengan benar
- Validasi input berhasil (minimal 2 custodians, threshold ≤ jumlah custodian)
- Navigasi ke halaman custodian setup berhasil
- Ceremony ID di-generate (contoh: CER-2025-001)

### Test Case 1.2: Registrasi Custodian
**ID**: TC-KEYCER-002
**Deskripsi**: Mendaftarkan 3 key custodian melalui web interface
**Prasyarat**: Ceremony setup sudah dilakukan
**Langkah-langkah**:
1. Di halaman custodian setup, klik "Add Custodian"
2. Isi data custodian pertama:
   - Name: "Custodian A"
   - Email: "custodian.a@example.com"
   - Phone: "+62-812-3456-7890"
   - Department: "IT Security"
3. Klik "Save Custodian"
4. Ulangi langkah 2-3 untuk Custodian B dan C
5. Klik "Proceed to Key Generation"

**Expected Result**:
- Setiap custodian berhasil didaftarkan
- Validasi email dan phone number berhasil
- Daftar custodian tampil dengan lengkap
- Tombol "Proceed to Key Generation" aktif setelah 3 custodian terdaftar

### Test Case 1.3: Asynchronous Multi-Party Master Key Generation
**ID**: TC-KEYCER-003
**Deskripsi**: Proses generate master key dengan kontribusi asynchronous via email link
**Prasyarat**: 3 custodian sudah terdaftar dengan email valid
**Langkah-langkah**:

#### Phase 1: Setup dan Security Warning (Administrator)
1. **Administrator** login ke HSM Simulator
2. Navigasi ke menu "Key Management" → "Key Ceremony" → "CER-2025-001"
3. Di halaman key generation, system menampilkan security warning:
   - ⚠️ **PERINGATAN KEAMANAN** ⚠️
   - "Mekanisme kontribusi via email ini tidak aman untuk production!"
   - "Seharusnya dilakukan secara fisik di secure room dengan pengawasan"
   - "Ini adalah simulasi untuk tujuan pembelajaran saja"
4. **Administrator** membaca penjelasan tentang proses asynchronous:
   - Setiap custodian akan menerima unique contribution link via email
   - Custodian dapat memberikan kontribusi kapan saja dalam 24 jam
   - Master key akan di-generate otomatis ketika semua kontribusi lengkap
   - Key shares akan dikirim via email setelah master key ter-generate
5. **Administrator** klik checkbox "I understand this is for educational purposes only"
6. **Administrator** pilih key algorithm:
   - Algorithm: "AES-256"
   - Key Size: 256 bits
7. **Administrator** klik "Start Asynchronous Key Generation"

#### Phase 2: Generate dan Kirim Contribution Links (Administrator)
8. System generate unique contribution links untuk masing-masing custodian:
   - Link untuk Custodian A: `https://hsm-simulator/contribute/CER-2025-001/A/[unique-token]`
   - Link untuk Custodian B: `https://hsm-simulator/contribute/CER-2025-001/B/[unique-token]`
   - Link untuk Custodian C: `https://hsm-simulator/contribute/CER-2025-001/C/[unique-token]`
9. System menampilkan konfirmasi kepada **Administrator**:
   - "Unique contribution links generated"
   - "Links will be sent to custodian emails"
   - "Each link expires in 24 hours"
10. **Administrator** klik "Send Contribution Links"
11. System mengirim email ke masing-masing custodian:
    - **Subject**: "HSM Key Ceremony - Contribution Required - CER-2025-001"
    - **Body**:
      ```
      Dear [Custodian Name],

      You have been selected as a key custodian for HSM Ceremony CER-2025-001.

      Please contribute your passphrase component using the link below:
      Link: [unique-contribution-link]
      Expires: [expiration-date]

      SECURITY NOTICE: This is a simulation for educational purposes only.
      In production, this should be done in a secure physical environment.

      Best regards,
      HSM Simulator Team
      ```
12. System menampilkan status dashboard kepada **Administrator**:
    - Ceremony ID: CER-2025-001
    - Status: "Awaiting Contributions"
    - Contributions Received: 0/3
    - Links Sent: ✓
    - Links Expire: 2025-01-16T10:00:00Z

#### Phase 3: Custodian A Contribution (Custodian A)
13. **Custodian A** menerima email dan membuka contribution link
14. System menampilkan halaman contribution:
    - "Key Contribution for Ceremony CER-2025-001"
    - "Custodian: A"
    - "Please enter your passphrase contribution"
    - Passphrase requirements:
      - Minimum 16 characters
      - Must contain uppercase letters
      - Must contain lowercase letters
      - Must contain numbers
      - Must contain special characters
15. **Custodian A** memasukkan passphrase: `MySecureP@ssw0rdForCustodianA!`
16. System validate passphrase complexity:
    - Length: 32 characters ✓
    - Uppercase: 4 characters ✓
    - Lowercase: 20 characters ✓
    - Numbers: 2 characters ✓
    - Special Characters: 2 characters ✓
    - Entropy Score: 8.5/10 ✓
17. System menampilkan preview:
    - Contribution ID: "CONT-A"
    - Passphrase Strength: "Very Strong"
    - Entropy Score: "8.5/10"
    - Fingerprint: "hash_of_contribution_a"
18. **Custodian A** konfirmasi dengan clicking "Submit Contribution"
19. System menampilkan success message:
    - "Thank you! Your contribution has been recorded."
    - "Contribution ID: CONT-A"
    - "Timestamp: 2025-01-15T11:15:00Z"
    - "You will receive your key share via email once all contributions are complete."

#### Phase 4: Custodian B Contribution (Custodian B)
20. **Custodian B** menerima email dan membuka link beberapa jam kemudian
21. System menampilkan halaman contribution yang sama
22. **Custodian B** memasukkan passphrase: `B@ckupK3yC0ntributorStr0ng!`
23. System validate complexity:
    - Length: 28 characters ✓
    - Meets all complexity requirements ✓
    - Entropy Score: 8.2/10 ✓
24. **Custodian B** konfirmasi contribution
25. System menampilkan success message

#### Phase 5: Custodian C Contribution (Custodian C)
26. **Custodian C** menerima email dan memberikan kontribusi
27. **Custodian C** memasukkan passphrase: `ThirdCustodian#Secur3P@ssphrase2025!`
28. System validate complexity ✓
29. **Custodian C** konfirmasi contribution

#### Phase 6: Automatic Master Key Generation (System)
30. Setelah contribution ketiga diterima, **system** otomatis:
    - Update status: "All contributions received"
    - Trigger master key generation process
    - Combine semua passphrase contributions menggunakan cryptographic hash:
      ```
      Combined_Entropy = HASH(Cont_A || Cont_B || Cont_C)
      Master_Key = KDF(Combined_Entropy, "HSM-Master-Key-2025")
      ```
    - Generate master key fingerprint
31. **System** update ceremony status:
    - Status: "Master Key Generated"
    - Master Key ID: "MK-001"
    - Generation Time: "2025-01-15T14:30:00Z"
    - Key Fingerprint: "master_key_fingerprint"
32. **Administrator** dapat melihat status update di dashboard

#### Phase 7: Generate dan Kirim Key Shares (System)
33. **System** generate key shares menggunakan Shamir's Secret Sharing:
    - Threshold: 2-of-3
    - Prime: Large prime number
    - Generate shares untuk masing-masing custodian
34. **System** kirim email key shares ke masing-masing custodian:
    - **Subject**: "HSM Key Ceremony - Your Key Share - CER-2025-001"
    - **Body**:
      ```
      Dear [Custodian Name],

      Your key share for ceremony CER-2025-001 is ready.

      Share ID: [SHARE_ID]
      Share Data: [encrypted_share_data]
      Verification Hash: [verification_hash]

      Please store this securely. This share is required for key recovery.

      SECURITY WARNING: Store this information securely. In production,
      this should be distributed physically in a secure environment.

      Best regards,
      HSM Simulator Team
      ```
35. **System** update final status:
    - Status: "Completed"
    - Key Shares Distributed: ✓
    - Completion Time: "2025-01-15T14:35:00Z"
36. **Administrator** melihat completion status di dashboard

**Expected Result**:
- **Administrator** dapat memulai dan monitor seluruh proses ceremony
- Setiap custodian menerima unique contribution link via email
- **Custodian** dapat berkontribusi secara asynchronous kapan saja
- Passphrase complexity validation yang robust
- **System** otomatis detect dan generate master key ketika semua kontribusi lengkap
- **System** otomatis kirim key shares via email setelah master key ter-generate
- **Administrator** dapat melihat real-time progress di dashboard
- Complete audit trail untuk seluruh proses
- Security warnings jelas untuk educational purposes
- System handle expired links dan invalid contributions

#### Security Features Validated:
- ✅ Role-based access control (Administrator vs Custodian)
- ✅ Unique contribution links dengan expiration
- ✅ Strong passphrase complexity requirements
- ✅ Asynchronous contribution tracking
- ✅ Automatic master key generation
- ✅ Secure email delivery mechanism
- ✅ Real-time progress monitoring
- ✅ Comprehensive audit logging
- ✅ Educational security warnings

#### Role Responsibilities:
- **Administrator**: Memulai ceremony, monitor progress, view reports
- **Custodian**: Menerima email, memberikan kontribusi via link, menerima key shares
- **System**: Automate key generation, handle email delivery, manage status updates


### Test Case 1.4: Ceremony Documentation dan Audit
**ID**: TC-KEYCER-004
**Deskripsi**: Generate dokumentasi ceremony dan audit trail
**Prasyarat**: Key ceremony completed successfully
**Langkah-langkah**:
1. Di halaman completion, klik "Generate Ceremony Report"
2. Review ceremony report:
   - Ceremony Details
   - Custodian Information
   - Key Generation Logs
   - Share Distribution Records
   - Audit Trail
3. Download report (PDF format)
4. Klik "View Mathematical Explanation"
5. Review penjelasan Shamir's Secret Sharing

**Expected Result**:
- Ceremony report ter-generate dengan lengkap
- Report berisi timestamp, participants, dan semua actions
- Mathematical explanation:
  - Penjelasan Shamir's Secret Sharing
  - Formula matematika yang digunakan
  - Contoh perhitungan dengan bilangan kecil
  - Visualisasi polynomial interpolation
- Report dapat di-download dalam format PDF

---

## 2. Skenario Pemulihan Master Key (Restoration Ceremony)

### Test Case 2.1: Inisiasi Restoration Ceremony
**ID**: TC-KEYCER-005
**Deskripsi**: Memulai proses restoration ceremony
**Prasyarat**:
- HSM memerlukan master key restoration
- Minimal 2 custodian tersedia
- Authorization untuk restoration sudah disetujui

**Langkah-langkah**:
1. Login sebagai administrator
2. Navigasi ke "Key Management" → "Key Restoration"
3. Klik "Start Restoration Ceremony"
4. Isi restoration form:
   - Restoration Reason: "System Migration"
   - Emergency Level: "Normal"
   - Expected Participants: 2
5. Upload authorization document
6. Klik "Proceed to Custodian Assembly"

**Expected Result**:
- Restoration ceremony berhasil di-inisiasi
- System generate Restoration ID (contoh: RES-2025-001)
- Authorization document berhasil di-upload
- Navigasi ke halaman custodian assembly berhasil

### Test Case 2.2: Assembly dan Verifikasi Custodian
**ID**: TC-KEYCER-006
**Deskripsi**: Mengumpulkan dan memverifikasi custodian yang tersedia
**Prasyarat**: Restoration ceremony sudah di-inisiasi
**Langkah-langkah**:
1. Di halaman custodian assembly, klik "Check Custodian Availability"
2. System menampilkan status semua 3 custodian:
   - Custodian A: Available
   - Custodian B: Available
   - Custodian C: Unavailable (on leave)
3. Pilih 2 custodian yang tersedia (A dan B)
4. Klik "Send Invitation to Selected Custodians"
5. **System** kirim email invitation ke custodian yang dipilih:
   - **Subject**: "HSM Key Restoration Required - RES-2025-001"
   - **Body**:
     ```
     Dear [Custodian Name],

     Your participation is required for HSM Key Restoration Ceremony RES-2025-001.

     Ceremony Details:
     - Restoration ID: RES-2025-001
     - Reason: System Migration
     - Expected Participants: 2 of 3 custodians
     - Your Role: Key Custodian

     Please login to the HSM Simulator to confirm your participation.

     SECURITY NOTICE: This is a simulation for educational purposes only.

     Best regards,
     HSM Simulator Team
     ```
6. **System** kirim email notification ke **Administrator**:
   - **Subject**: "Restoration Ceremony RES-2025-001 - Invitations Sent"
   - **Body**:
     ```
     Dear Administrator,

     Restoration ceremony invitations have been sent to selected custodians.

     Ceremony ID: RES-2025-001
     Invited Custodians: Custodian A, Custodian B
     Pending Responses: 2
     Ceremony Status: Awaiting Custodian Confirmation

     You can monitor the progress in the restoration dashboard.

     Best regards,
     HSM Simulator Team
     ```
7. Tunggu custodian menerima undangan dan login
8. Setiap custodian yang login akan menerima konfirmasi email:
   - **Subject**: "Restoration Participation Confirmed - RES-2025-001"
   - **Body**:
     ```
     Dear [Custodian Name],

     Your participation in restoration ceremony RES-2025-001 has been confirmed.

     Next Steps:
     1. Prepare your key share for submission
     2. Wait for further instructions from the administrator
     3. Be ready for share submission when requested

     Ceremony Dashboard: [dashboard-link]

     Best regards,
     HSM Simulator Team
     ```
9. **System** update status secara real-time dan kirim progress update ke **Administrator**

**Expected Result**:
- System menampilkan real-time availability status
- Email invitation berhasil dikirim ke custodian yang dipilih
- Email notification berhasil dikirim ke administrator
- Custodian menerima konfirmasi email setelah login
- Administrator menerima progress updates
- System update status secara real-time
- Minimal threshold (2 custodians) terpenuhi
- Complete audit trail untuk semua komunikasi email

### Test Case 2.3: Submit Key Shares untuk Reconstruction
**ID**: TC-KEYCER-007
**Deskripsi**: Proses submit key shares untuk rekonstruksi master key
**Prasyarat**: 2 custodian sudah siap dan ter-verifikasi
**Langkah-langkah**:
1. **Administrator** klik "Start Share Collection" di dashboard
2. **System** kirim email notifikasi ke semua custodian yang terkonfirmasi:
   - **Subject**: "Restoration Ceremony RES-2025-001 - Ready for Share Submission"
   - **Body**:
     ```
     Dear [Custodian Name],

     Restoration ceremony RES-2025-001 is now ready for share submission.

     Action Required:
     1. Login to HSM Simulator
     2. Navigate to Restoration Ceremony → RES-2025-001
     3. Submit your key share when requested

     Your Share Information:
     - Share ID: [SHARE_ID]
     - Ceremony: RES-2025-001
     - Deadline: [submission_deadline]

     Please submit your share as soon as possible.

     Best regards,
     HSM Simulator Team
     ```
3. **Custodian A** login dan navigasi ke restoration page
4. Upload share package atau input share manual:
   - Share ID: "SHARE-A"
   - Encrypted Share: [base64_encoded_share]
   - Verification Hash: [verification_hash]
5. Klik "Submit Share"
6. **System** verifikasi share integrity:
   - Hash verification
   - Share format validation
   - Timestamp recording
7. **System** kirim email konfirmasi ke **Custodian A**:
   - **Subject**: "Share Submission Confirmed - RES-2025-001"
   - **Body**:
     ```
     Dear Custodian A,

     Your key share submission for restoration ceremony RES-2025-001 has been confirmed.

     Submission Details:
     - Share ID: SHARE-A
     - Submission Time: [timestamp]
     - Verification Status: ✓ Success
     - Ceremony Status: 1 of 2 shares received

     Thank you for your participation.

     Best regards,
     HSM Simulator Team
     ```
8. **System** kirim email progress update ke **Administrator**:
   - **Subject**: "Share Submission Update - RES-2025-001"
   - **Body**:
     ```
     Dear Administrator,

     Share submission progress update for restoration ceremony RES-2025-001.

     Progress:
     - Total Shares Required: 2
     - Shares Received: 1 (Custodian A)
     - Shares Pending: 1 (Custodian B)
     - Ceremony Status: Awaiting Final Share

     Next custodian notified for submission.

     Best regards,
     HSM Simulator Team
     ```
9. System tampilkan: "1 of 2 shares received"
10. **Custodian B** login dan ulangi langkah 3-8
11. **System** kirim email final notification:
    - **Ke Administrator**: "Threshold Met! Starting Reconstruction - RES-2025-001"
    - **Ke semua custodian**: "Restoration Process Started - RES-2025-001"
12. System tampilkan: "Threshold met! Starting reconstruction..."

**Expected Result**:
- Share verification berhasil untuk kedua custodian
- System mencegah duplicate share submission
- Real-time status update untuk share collection
- Automatic reconstruction trigger ketika threshold terpenuhi
- Email notifikasi terkirim ke semua stakeholder (Administrator dan Custodian)
- Complete audit trail untuk semua submissions dan komunikasi
- Progress updates tersedia untuk monitoring

### Test Case 2.4: Master Key Reconstruction
**ID**: TC-KEYCER-008
**Deskripsi**: Proses rekonstruksi master key dari key shares
**Prasyarat**: 2 key shares sudah berhasil di-submit
**Langkah-langkah**:
1. System otomatis jalankan Shamir's Secret Sharing reconstruction
2. **System** kirim email notifikasi ke **Administrator**:
   - **Subject**: "Reconstruction in Progress - RES-2025-001"
   - **Body**:
     ```
     Dear Administrator,

     Master key reconstruction for restoration ceremony RES-2025-001 has started.

     Reconstruction Status:
     - Phase 1: Reconstructing polynomial...
     - Phase 2: Calculating master key...
     - Phase 3: Verifying key integrity...
     - Estimated Completion: [estimated_time]

     You can monitor the real-time progress in the dashboard.

     Best regards,
     HSM Simulator Team
     ```
3. Tampilkan progress reconstruction:
   - "Reconstructing polynomial..."
   - "Calculating master key..."
   - "Verifying key integrity..."
4. **System** kirim progress update email ke semua participants:
   - **Subject**: "Reconstruction Progress Update - RES-2025-001"
   - **Body**:
     ```
     Dear Participants,

     Master key reconstruction progress for ceremony RES-2025-001.

     Current Progress: [current_phase]
     Completion: [percentage]%
     Estimated Time Remaining: [time_remaining]

     Next Steps:
     - Key verification
     - System activation
     - Final validation

     Best regards,
     HSM Simulator Team
     ```
5. Tampilkan reconstruction result:
   - Master Key ID: "MK-001"
   - Reconstruction Success: ✓
   - Key Checksum: [checksum_value]
   - Reconstruction Time: [duration]
6. **System** kirim email sukses ke semua stakeholders:
   - **Ke Administrator**: "Reconstruction Completed Successfully - RES-2025-001"
   - **Ke Custodian**: "Master Key Restored - RES-2025-001"
   - **Body**:
     ```
     Dear [Recipient],

     Master key reconstruction for ceremony RES-2025-001 has been completed successfully.

     Reconstruction Summary:
     - Master Key ID: MK-001
     - Reconstruction Status: ✓ Success
     - Key Checksum: [checksum_value]
     - Reconstruction Time: [duration]
     - Verification Status: ✓ Passed

     Next Steps: System activation and final verification.

     Best regards,
     HSM Simulator Team
     ```
7. Klik "View Reconstruction Details"
8. Review step-by-step reconstruction process

**Expected Result**:
- Master key berhasil di-reconstruct dari 2 shares
- Reconstruction process documented dengan lengkap
- Mathematical verification berhasil
- System menampilkan visualisasi reconstruction process
- Key integrity terverifikasi melalui checksum
- Email notifikasi progress dan status terkirim ke semua stakeholders
- Real-time monitoring capabilities untuk administrator
- Complete audit trail untuk reconstruction process

### Test Case 2.5: Activation dan Verification
**ID**: TC-KEYCER-009
**Deskripsi**: Aktivasi master key yang di-reconstruct dan verifikasi sistem
**Prasyarat**: Master key reconstruction berhasil
**Langkah-langkah**:
1. Di halaman activation, klik "Activate Restored Master Key"
2. **System** kirim email notifikasi ke **Administrator**:
   - **Subject**: "Starting Key Activation - RES-2025-001"
   - **Body**:
     ```
     Dear Administrator,

     Master key activation process for restoration ceremony RES-2025-001 is starting.

     Activation Process:
     - Phase 1: Security validation
     - Phase 2: System compatibility check
     - Phase 3: Functional testing
     - Phase 4: Final activation

     Estimated Duration: [duration]

     Best regards,
     HSM Simulator Team
     ```
3. System perform security checks:
   - Key format validation
   - Key strength verification
   - System compatibility check
4. **System** kirim email progress update:
   - **Subject**: "Security Checks Completed - RES-2025-001"
   - **Body**:
     ```
     Dear Administrator,

     Security checks for master key activation have been completed.

     Security Validation Results:
     - Key Format: ✓ Valid
     - Key Strength: ✓ Meets Requirements
     - System Compatibility: ✓ Compatible
     - Risk Assessment: ✓ Low Risk

     Proceeding to functional testing.

     Best regards,
     HSM Simulator Team
     ```
5. Klik "Verify System Functionality"
6. Test basic HSM operations:
   - Test encryption dengan restored key
   - Test key generation
   - Test access control
7. **System** kirim email final test results:
   - **Subject**: "Functional Testing Completed - RES-2025-001"
   - **Body**:
     ```
     Dear Administrator,

     Functional testing for restored master key has been completed.

     Test Results:
     - Encryption Test: ✓ Success
     - Key Generation: ✓ Success
     - Access Control: ✓ Success
     - Performance Test: ✓ Within Limits

     All tests passed. Ready for final activation.

     Best regards,
     HSM Simulator Team
     ```
8. Klik "Complete Restoration"
9. **System** kirim email completion notification ke semua stakeholders:
   - **Ke Administrator**: "Restoration Ceremony Completed - RES-2025-001"
   - **Ke Custodian**: "HSM System Restored Successfully - RES-2025-001"
   - **Ke Security Team**: "Security Verification Complete - RES-2025-001"
   - **Body**:
     ```
     Dear [Recipient],

     HSM Master Key Restoration Ceremony RES-2025-001 has been completed successfully.

     Completion Summary:
     - Master Key: Successfully restored and activated
     - System Status: Ready for production use
     - Security Verification: ✓ All checks passed
     - Functional Testing: ✓ All operations normal
     - Documentation: Generated and archived

     The HSM system is now fully operational with the restored master key.

     Best regards,
     HSM Simulator Team
     ```

**Expected Result**:
- Master key berhasil di-aktifkan di HSM
- Semua security checks berhasil
- Basic HSM operations berfungsi normal
- System ready untuk production use
- Restoration ceremony documentation ter-generate
- Email notifikasi lengkap untuk semua fase activation
- Stakeholder communication yang komprehensif
- Complete audit trail untuk seluruh restoration process

---

## 3. Skenario Pembelajaran dan Edukasi

### Test Case 3.1: View Key Components untuk Pembelajaran
**ID**: TC-KEYCER-010
**Deskripsi**: Menampilkan komponen kunci untuk pembelajaran
**Prasyarat**: Key ceremony completed (initialization atau restoration)
**Langkah-langkah**:
1. Navigasi ke "Key Management" → "Educational View"
2. Pilih ceremony ID dari dropdown
3. Klik "Show Key Components"
4. Review educational information:
   - Original Master Key (hexadecimal)
   - Individual Key Shares (hexadecimal)
   - Encrypted Key Shares
   - Verification Hashes
   - Shamir's Secret Sharing Parameters
5. Klik "View Mathematical Details" untuk penjelasan parameter

**Expected Result**:
- Key components ditampilkan dalam format yang mudah dipahami
- Mathematical parameters ditampilkan:
  - Prime number (p)
  - Threshold (k)
  - Total shares (n)
  - Polynomial coefficients
- Educational explanation tersedia untuk setiap parameter


### Test Case 3.2: Manual Calculation dengan Interface Bantuan
**ID**: TC-KEYCER-011
**Deskripsi**: Simulasi perhitungan manual Shamir's Secret Sharing
**Prasyarat**: Key ceremony data tersedia
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "Manual Calculator"
2. Pilih "Reconstruction Calculator"
3. Input sample data:
   - Prime (p): 1613 (contoh bilangan kecil)
   - Threshold (k): 2
   - Share 1: (1, 1494)
   - Share 2: (2, 329)
4. Klik "Calculate Step-by-Step"
5. Ikuti guided calculation:
   - Langkah 1: Setup system of equations
   - Langkah 2: Calculate Lagrange basis polynomials
   - Langkah 3: Compute secret value
   - Langkah 4: Verify result
6. Compare dengan system calculation

**Expected Result**:
- Step-by-step calculator guide user melalui perhitungan
- Mathematical formulas ditampilkan dengan jelas
- User dapat memahami proses interpolation
- System verification memastikan perhitungan manual benar
- Educational tooltips untuk setiap mathematical concept


---

## 4. Skenario Error Handling dan Edge Cases

### Test Case 4.1: Passphrase Complexity Validation
**ID**: TC-KEYCER-012
**Deskripsi**: Validasi kompleksitas passphrase contribution dari custodian
**Prasyarat**: Contribution link sudah dikirim ke custodian
**Langkah-langkah**:
1. Custodian membuka contribution link
2. Coba submit passphrase yang tidak memenuhi requirements:
   - Terlalu pendek: "short123" (< 16 chars)
   - Tidak ada uppercase: "lowercasepassword123!"
   - Tidak ada lowercase: "UPPERCASEPASSWORD123!"
   - Tidak ada numbers: "NoNumbersHere!"
   - Tidak ada special chars: "SimplePassword123"
   - Pattern yang predictable: "Password123!", "1234567890123456"
3. Verifikasi system response:
   - Real-time validation feedback
   - Specific error messages untuk setiap requirement
   - Visual indicators untuk requirements yang terpenuhi
   - Entropy score calculation
4. Test dengan passphrase yang weak tapi memenuhi technical requirements:
   - "Password123!" (technically valid but weak)
   - "Qwerty1234!" (common pattern)
5. Verifikasi warning untuk weak meskipun technically valid

**Expected Result**:
- System reject passphrase yang tidak memenuhi complexity requirements
- Real-time validation dengan specific feedback
- Entropy score calculation untuk mengukur strength
- Warning untuk passphrase yang technically valid tapi weak
- Clear guidance untuk improve passphrase strength

### Test Case 4.2: Contribution Link Management
**ID**: TC-KEYCER-013
**Deskripsi**: Manajemen contribution link yang expired dan invalid access
**Prasyarat**: Ceremony sudah dimulai dengan contribution links
**Langkah-langkah**:
1. Test valid contribution link:
   - Custodian A buka link sebelum expiration
   - Verify successful access
   - Complete contribution process
2. Test expired contribution link:
   - Tunggu hingga link expired (24 jam)
   - Custodian B coba akses expired link
   - Verify system response
3. Test invalid/tampered link:
   - Modify token parameter di URL
   - Coba akses dengan invalid token
   - Verify error handling
4. Test reuse prevention:
   - Custodian C coba akses link setelah sudah berkontribusi
   - Verify system mencegah double contribution
5. Test link cancellation:
   - Administrator cancel ceremony yang sedang berjalan
   - Verify semua links menjadi invalid

**Expected Result**:
- Expired links ditolak dengan clear error message
- Invalid/tampered links detected dan rejected
- Double contribution prevention berfungsi
- Link cancellation invalidates semua remaining links
- Comprehensive audit trail untuk semua access attempts

### Test Case 4.3: Invalid Share Submission
**ID**: TC-KEYCER-014
**Deskripsi**: Handle submission share yang invalid
**Prasyarat**: Restoration ceremony in progress
**Langkah-langkah**:
1. Selama restoration, submit invalid share:
   - Share dengan format salah
   - Share dengan hash yang tidak cocok
   - Share dari ceremony yang berbeda
2. Verifikasi error handling
3. Coba submit share yang sama dua kali
4. Test dengan share yang sudah expired

**Expected Result**:
- System mendeteksi dan reject invalid shares
- Clear error messages untuk setiap jenis error
- Prevent duplicate share submission
- Audit log mencatat semua invalid submission attempts
- System tetap stable setelah invalid submissions

### Test Case 4.4: Network Issues During Ceremony
**ID**: TC-KEYCER-015
**Deskripsi**: Handle network connectivity issues selama ceremony
**Prasyarat**: Key ceremony sedang berjalan
**Langkah-langkah**:
1. Simulasikan network disconnect:
   - Matikan network selama share submission
   - Refresh browser dengan state yang belum tersimpan
   - Multiple concurrent sessions
2. Verifikasi session persistence
3. Test reconnection capability
4. Verify data integrity setelah reconnect

**Expected Result**:
- Session state tetap terjaga
- Auto-save functionality untuk data yang sudah di-input
- Graceful handling network failures
- Data integrity terjaga setelah reconnect
- Clear indication kepada user tentang connection status

### Test Case 4.5: Asynchronous Contribution Scenarios
**ID**: TC-KEYCER-016
**Deskripsi**: Handle berbagai skenario contribution asynchronous
**Prasyarat**: Ceremony sudah dimulai dengan contribution links
**Langkah-langkah**:

#### Scenario A: Partial Contributions
1. Start ceremony dengan 3 custodians
2. Hanya 2 custodians yang berkontribusi dalam 24 jam:
   - Custodian A: Kontribusi setelah 1 jam
   - Custodian B: Kontribusi setelah 6 jam
   - Custodian C: Tidak berkontribusi (link expired)
3. Verifikasi system behavior:
   - Status tetap "Awaiting Contributions"
   - Warning messages untuk custodian yang belum berkontribusi
   - Automatic expiration setelah 24 jam

#### Scenario B: Late Contribution
1. Start ceremony
2. Custodian A dan B berkontribusi tepat waktu
3. Custodian C mencoba berkontribusi 1 jam sebelum expiration
4. Verifikasi system menerima late contribution
5. Test automatic master key generation setelah contribution lengkap

#### Scenario C: Ceremony Timeout
1. Start ceremony
2. Hanya 1 custodian yang berkontribusi
3. Tunggu hingga 24 jam expiration
4. Verifikasi system:
   - Ceremony status berubah menjadi "Expired"
   - Email notification ke semua participants
   - Administrator dapat extend atau cancel ceremony

#### Scenario D: Concurrent Contributions
1. Multiple custodians mencoba berkontribusi secara bersamaan
2. Test race condition prevention:
   - Simultaneous form submissions
   - Database locking mechanisms
   - Atomic operations
3. Verify data integrity dan consistent state

**Expected Result**:
- Graceful handling partial contributions
- Proper expiration management
- Late contributions diterima jika masih dalam time window
- Automatic status transitions
- Race condition prevention
- Clear notifications untuk timeout scenarios

### Test Case 4.6: Email Delivery Issues
**ID**: TC-KEYCER-017
**Deskripsi**: Handle issues dengan email delivery system
**Prasyarat**: Email configuration untuk testing
**Langkah-langkah**:

#### Scenario A: Failed Email Delivery
1. Simulasikan email server failure
2. Start ceremony dan generate contribution links
3. System detect failed email delivery
4. Verify fallback mechanisms:
   - Retry logic dengan exponential backoff
   - Administrator notification
   - Manual link distribution option

#### Scenario B: Bounced Emails
1. Custodian email address invalid
2. System receive bounce notification
3. Verify system response:
   - Update ceremony status
   - Notify administrator
   - Allow email address correction

#### Scenario C: Spam Filter Issues
1. Contribution emails masuk ke spam folder
2. Custodian tidak menerima email dalam 1 jam
3. System detect undelivered contributions
4. Verify reminder mechanism:
   - Automatic reminder emails
   - Administrator notification
   - Alternative distribution methods

**Expected Result**:
- Robust email delivery dengan retry mechanisms
- Clear handling delivery failures
- Administrator notifications untuk issues
- Alternative distribution methods
- Complete audit trail untuk email delivery attempts

### Test Case 4.7: Concurrent Access Scenarios
**ID**: TC-KEYCER-018
**Deskripsi**: Handle multiple users mengakses ceremony simultaneously
**Prasyarat**: Multiple user accounts tersedia
**Langkah-langkah**:
1. Login dengan 3 users secara bersamaan
2. Akses ceremony yang sama:
   - Administrator: Manage ceremony
   - Custodian: Submit contributions via separate links
   - Observer: View progress dashboard
3. Test conflicting operations:
   - Simultaneous contribution submissions
   - Concurrent ceremony modifications
   - Real-time status updates
4. Verify race condition prevention

**Expected Result**:
- Proper access control untuk berbagai user roles
- Real-time updates untuk semua connected users
- Prevention of conflicting operations
- Optimistic locking untuk concurrent modifications
- Clear indication kepada user tentang operasi yang sedang berjalan

---

## 5. Skenario Audit dan Compliance

### Test Case 5.1: Comprehensive Audit Trail
**ID**: TC-KEYCER-019
**Deskripsi**: Verifikasi audit trail untuk semua ceremony activities
**Prasyarat**: Key ceremony sudah selesai
**Langkah-langkah**:
1. Navigasi ke "Audit Logs" → "Ceremony Logs"
2. Filter logs untuk ceremony ID tertentu (CER-2025-001)
3. Review audit entries:
   - Ceremony initiation dan setup
   - Contribution link generation dan distribution
   - Email delivery logs
   - Individual custodian contributions (timestamp, IP address, passphrase hash)
   - Master key generation process
   - Key shares generation dan distribution
   - System status transitions
   - All access attempts (successful dan failed)
4. Export audit log dalam berbagai format (CSV, PDF, JSON)
5. Verify log integrity dan immutability dengan cryptographic hashes

**Expected Result**:
- Comprehensive audit trail untuk semua ceremony activities
- Detailed tracking untuk setiap contribution (who, when, from where)
- Email delivery logs dengan status codes
- Cryptographic verification untuk log integrity
- Complete chain of custody untuk sensitive operations
- Export capability dalam multiple formats
- Advanced search dan filter functionality

### Test Case 5.2: Email Communication Audit
**ID**: TC-KEYCER-020
**Deskripsi**: Audit trail untuk semua email communications
**Prasyarat**: Ceremony sudah selesai
**Langkah-langkah**:
1. Navigasi ke "Audit Logs" → "Email Communications"
2. Review email logs untuk ceremony:
   - Contribution request emails
   - Key share distribution emails
   - Reminder emails
   - Error notification emails
   - Bounce dan delivery failure notifications
3. Verify email content audit:
   - Email subject dan body
   - Recipient information
   - Send timestamps
   - Delivery status
   - Link usage tracking
4. Test email content reconstruction capabilities

**Expected Result**:
- Complete audit trail untuk semua email communications
- Content reconstruction untuk compliance purposes
- Delivery status tracking dengan detailed logs
- Link usage analytics dan tracking
- Secure storage untuk sensitive email content
- Compliance dengan email retention policies

### Test Case 5.3: Compliance Reporting
**ID**: TC-KEYCER-021
**Deskripsi**: Generate compliance reports untuk regulatory requirements
**Prasyarat**: Audit data tersedia
**Langkah-langkah**:
1. Navigasi ke "Compliance" → "Generate Report"
2. Pilih report type:
   - Asynchronous Key Ceremony Summary
   - Email Security Compliance Report
   - Passphrase Strength Analysis
   - Timeline Analysis Report
   - Security Incident Report
3. Set date range dan filters
4. Generate report
5. Review compliance indicators:
   - Security warnings adherence
   - Email delivery success rate
   - Passphrase complexity compliance
   - Timeline adherence
   - Incident response effectiveness

**Expected Result**:
- Comprehensive compliance reports khusus untuk asynchronous ceremonies
- Email security compliance metrics
- Passphrase strength analytics
- Timeline adherence reporting
- Security incident documentation
- Digital signature untuk report authenticity
- Historical compliance tracking dengan trend analysis

---
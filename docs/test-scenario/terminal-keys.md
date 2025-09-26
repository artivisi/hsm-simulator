# Skenario Pengujian Terminal Master Key Initialization HSM Simulator

Dokumen ini berisi skenario pengujian untuk fitur Terminal Master Key (TMK) initialization pada HSM Simulator. Skenario ini dirancang untuk keperluan edukasi dan demonstrasi proses inisialisasi dan manajemen kunci terminal dalam sistem perbankan.

## Ringkasan Skenario

### Konfigurasi Terminal Master Key Operations
- **Kunci Utama**: ZMK (Zone Master Key) untuk mengenkripsi TMK
- **Terminal Master Key**: TMK untuk operasi terminal
- **Key Length**: 128-bit, 192-bit, atau 256-bit AES
- **Interface**: Web HSM Simulator
- **Tujuan**: Pembelajaran dan demonstrasi

### Fitur Khusus
- Key generation dengan berbagai algorithm options
- Key distribution dan activation
- Key rotation dan replacement
- Educational visualization dari key lifecycle
- Simulasi key export/import untuk terminal deployment

---

## 1. Skenario Terminal Master Key Generation

### Test Case 1.1: Setup TMK Generation
**ID**: TC-TMK-001
**Deskripsi**: Memverifikasi setup awal TMK generation melalui web interface
**Prasyarat**:
- Pengguna login ke HSM Simulator
- ZMK (Zone Master Key) sudah tersedia
- Menu Key Management tersedia

**Langkah-langkah**:
1. Login ke HSM Simulator
2. Navigasi ke menu "Key Management" → "Terminal Keys"
3. Klik "Generate New TMK"
4. System menampilkan form TMK generation:
   - Terminal ID field
   - Key Type dropdown (AES-128, AES-192, AES-256)
   - Key Scheme selection
   - ZMK selection
   - Expiration date
5. System otomatis menampilkan educational content (mode selalu aktif)

**Expected Result**:
- Form TMK generation tampil dengan benar
- Key type options tersedia (AES-128, AES-192, AES-256)
- ZMK list populated dengan kunci yang tersedia
- Educational content otomatis ditampilkan
- Field validation ready

### Test Case 1.2: Generate TMK AES-128
**ID**: TC-TMK-002
**Deskripsi**: Generate TMK dengan AES-128 encryption
**Prasyarat**: Form TMK generation tersedia
**Langkah-langkah**:
1. Input Terminal ID: `TERM-001`
2. Pilih Key Type: `AES-128`
3. Pilih Key Scheme: `ANSI X9.17`
4. Pilih ZMK: `ZMK-001`
5. Set Expiration Date: `2025-12-31`
6. System otomatis menampilkan educational process:
   - Random number generation process
   - Key derivation explanation
   - ZMK encryption process
7. Klik "Generate TMK"
8. Review hasil:
   - Clear TMK: [hasil dalam hex]
   - Encrypted TMK: [hasil enkripsi]
   - Key Check Value: [KCV]
   - Component Check Value: [CCV]

**Expected Result**:
- TMK AES-128 berhasil di-generate
- Step-by-step process otomatis ditampilkan
- Clear dan encrypted TMK tampil
- Key generation process sesuai ANSI X9.17 standard
- Educational explanation selalu tersedia

### Test Case 1.3: Generate TMK AES-256
**ID**: TC-TMK-003
**Deskripsi**: Generate TMK dengan AES-256 encryption
**Prasyarat**: Form TMK generation tersedia
**Langkah-langkah**:
1. Input Terminal ID: `TERM-002`
2. Pilih Key Type: `AES-256`
3. Pilih Key Scheme: `Variant X9.17`
4. Pilih ZMK: `ZMK-001`
5. System otomatis menampilkan educational explanation:
   - Difference between AES-128 and AES-256
   - Security level comparison
   - Performance considerations
6. Klik "Generate TMK"
7. Review hasil dan educational content

**Expected Result**:
- TMK AES-256 berhasil di-generate
- Perbandingan security level dijelaskan
- Educational content untuk AES-256 tersedia
- Key strength analysis ditampilkan

### Test Case 1.4: Batch TMK Generation
**ID**: TC-TMK-004
**Deskripsi**: Generate multiple TMK untuk beberapa terminal sekaligus
**Prasyarat**: TMK generation form tersedia
**Langkah-langkah**:
1. Navigasi ke "Batch TMK Generation"
2. Upload file CSV berisi:
   - Terminal ID, Key Type, Key Scheme, Expiration
   - TERM-003, AES-128, ANSI X9.17, 2025-12-31
   - TERM-004, AES-192, Variant X9.17, 2025-06-30
   - TERM-005, AES-256, ANSI X9.17, 2026-03-31
3. Klik "Generate Batch TMK"
4. Monitor progress:
   - Total terminals: 3
   - Processed: X
   - Success: Y
   - Failed: Z
5. Review hasil dalam table format
6. Download TMK package

**Expected Result**:
- Batch TMK generation berhasil dijalankan
- Real-time progress monitoring
- Comprehensive result report
- Export capability untuk deployment

---

## 2. Skenario Terminal Master Key Distribution

### Test Case 2.1: TMK Package Preparation
**ID**: TC-TMK-005
**Deskripsi**: Prepare TMK package untuk distribution ke terminal
**Prasyarat**: TMK sudah berhasil di-generate
**Langkah-langkah**:
1. Navigasi ke "Key Distribution" → "Prepare TMK Package"
2. Pilih terminal dari list: `TERM-001`
3. Review TMK information:
   - Terminal ID: TERM-001
   - Key Type: AES-128
   - Key Scheme: ANSI X9.17
   - Encrypted TMK: [hex value]
   - KCV: [value]
   - Expiration: 2025-12-31
4. Pilih distribution method:
   - Manual entry
   - Secure file transfer
   - API integration
5. Klik "Prepare Package"
6. Review educational content tentang key distribution security

**Expected Result**:
- TMK package berhasil disiapkan
- Distribution options tersedia
- Educational security content ditampilkan
- Package ready untuk deployment

### Test Case 2.2: Secure TMK Distribution
**ID**: TC-TMK-006
**Deskripsi**: Simulasi secure distribution TMK ke terminal
**Prasyarat**: TMK package sudah disiapkan
**Langkah-langkah**:
1. Pilih "Secure File Transfer" method
2. System generate secure package:
   - Encrypted TMK file
   - Distribution manifest
   - Security certificates
   - Installation instructions
3. Klik "Download Secure Package"
4. Review educational distribution process:
   - Secure channel requirements
   - Authentication procedures
   - Installation verification
5. Test package integrity check

**Expected Result**:
- Secure TMK package ter-download
- Package integrity verified
- Educational distribution process ditampilkan
- Security measures dijelaskan

### Test Case 2.3: Terminal Key Loading Simulation
**ID**: TC-TMK-007
**Deskripsi**: Simulasi proses loading TMK ke terminal
**Prasyarat**: TMK package tersedia
**Langkah-langkah**:
1. Navigasi ke "Terminal Simulation" → "Key Loading"
2. Upload TMK package
3. Simulate terminal key loading process:
   - Package verification
   - Authentication check
   - Key decryption (simulated)
   - Key validation
   - Activation confirmation
4. Review step-by-step loading process
5. Verify educational content about secure loading

**Expected Result**:
- Terminal key loading simulation berhasil
- Step-by-step process visualized
- Educational security content ditampilkan
- Loading verification completed

---

## 3. Skenario Terminal Master Key Management

### Test Case 3.1: TMK Status Monitoring
**ID**: TC-TMK-008
**Deskripsi**: Monitor status dan health TMK yang aktif
**Prasyarat**: TMK sudah ter-deploy ke terminal
**Langkah-langkah**:
1. Navigasi ke "Key Management" → "TMK Status"
2. Review TMK dashboard:
   - Active TMK list
   - Terminal status
   - Key health indicators
   - Expiration warnings
   - Usage statistics
3. Klik pada TMK untuk detail view:
   - Key information
   - Deployment history
   - Usage logs
   - Security events
4. Test filtering dan search capabilities

**Expected Result**:
- TMK status monitoring dashboard tersedia
- Real-time key health indicators
- Detailed key information accessible
- Advanced filtering dan search working

### Test Case 3.2: TMK Rotation
**ID**: TC-TMK-009
**Deskripsi**: Rotate TMK yang sudah ada dengan kunci baru
**Prasyarat**: TMK aktif tersedia untuk rotation
**Langkah-langkah**:
1. Navigasi ke "Key Management" → "TMK Rotation"
2. Pilih TMK untuk rotation: `TERM-001`
3. Review current TMK information
4. Configure rotation parameters:
   - New key type (AES-128/192/256)
   - Rotation schedule
   - Backup strategy
   - Transition period
5. Klik "Start Rotation Process"
6. Review educational rotation process:
   - Key transition planning
   - Synchronization requirements
   - Fallback procedures
7. Monitor rotation progress

**Expected Result**:
- TMK rotation process berhasil di-inisiasi
- Rotation parameters terkonfigurasi
- Educational rotation process ditampilkan
- Progress monitoring available

### Test Case 3.3: TMK Backup and Recovery
**ID**: TC-TMK-010
**Deskripsi**: Backup dan recovery TMK untuk disaster recovery
**Prasyarat**: TMK aktif tersedia
**Langkah-langkah**:
1. Navigasi ke "Key Management" → "TMK Backup"
2. Pilih TMK untuk backup: `TERM-001`
3. Configure backup parameters:
   - Backup method (encrypted storage)
   - Retention period
   - Access controls
   - Recovery procedures
4. Klik "Create Backup"
5. Review backup confirmation:
   - Backup ID generated
   - Storage location
   - Encryption details
   - Recovery instructions
6. Test recovery simulation

**Expected Result**:
- TMK backup berhasil dibuat
- Backup parameters terkonfigurasi
- Recovery procedures documented
- Educational content about backup security

---

## 4. Skenario Educational Features

### Test Case 4.1: TMK Lifecycle Visualization
**ID**: TC-TMK-011
**Deskripsi**: Visualisasi lengkap TMK lifecycle
**Prasyarat**: Educational mode aktif
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "TMK Lifecycle"
2. Review lifecycle stages:
   - Key generation
   - Distribution
   - Activation
   - Monitoring
   - Rotation
   - Decommissioning
3. Click pada setiap stage untuk detailed explanation
4. Review interactive timeline visualization
5. Test scenario-based learning modules

**Expected Result**:
- Complete TMK lifecycle visualization
- Interactive stage exploration
- Detailed educational content
- Timeline visualization working

### Test Case 4.2: Key Strength Analysis
**ID**: TC-TMK-012
**Deskripsi**: Analisis kekuatan dan security TMK
**Prasyarat**: TMK data tersedia
**Langkah-langkah**:
1. Navigasi ke "Security Analysis" → "Key Strength"
2. Pilih TMK untuk analysis: `TERM-001`
3. Review strength metrics:
   - Key length analysis
   - Algorithm security
   - Entropy calculation
   - Vulnerability assessment
   - Compliance checking
4. Review security recommendations:
   - Key rotation schedules
   - Security best practices
   - Regulatory compliance
5. Generate security report

**Expected Result**:
- Comprehensive key strength analysis
- Security metrics calculated
- Recommendations generated
- Educational security content

### Test Case 4.3: TMK Encryption Process Demo
**ID**: TC-TMK-013
**Deskripsi**: Demonstrasi proses enkripsi TMK dengan ZMK
**Prasyarat**: ZMK dan TMK tersedia
**Langkah-langkah**:
1. Navigasi ke "Educational Tools" → "Encryption Demo"
2. Select demonstration mode: "TMK under ZMK"
3. Input sample TMK: [sample key]
4. Select ZMK: `ZMK-001`
5. Review step-by-step encryption:
   - Key formatting
   - Encryption algorithm selection
   - IV generation
   - Encryption process
   - Result validation
6. Test dengan berbagai key combinations

**Expected Result**:
- Step-by-step encryption demonstration
- Interactive algorithm selection
- Visual encryption process
- Educational explanations per step

---

## 5. Skenario Error Handling dan Edge Cases

### Test Case 5.1: Invalid TMK Generation Parameters
**ID**: TC-TMK-014
**Deskripsi**: Handle invalid parameters saat TMK generation
**Prasyarat**: TMK generation form tersedia
**Langkah-langkah**:
1. Test invalid Terminal ID:
   - Kosong
   - Format invalid
   - Duplicate ID
2. Test invalid key parameters:
   - Key type tidak valid
   - Key scheme tidak kompatibel
   - Expiration date di masa lalu
3. Test invalid ZMK selection:
   - ZMK tidak aktif
   - ZMK expired
   - ZMK tidak compatible
4. Review error handling dan educational messages

**Expected Result**:
- System reject invalid parameters
- Clear error messages dengan educational content
- Real-time validation feedback
- Graceful error handling

### Test Case 5.2: TMK Distribution Failures
**ID**: TC-TMK-015
**Deskripsi**: Handle failures saat TMK distribution
**Prasyarat**: TMK distribution in progress
**Langkah-langkah**:
1. Simulasikan distribution failures:
   - Network connectivity issues
   - Terminal unreachable
   - Authentication failures
   - Package corruption
2. Test retry mechanisms
3. Review fallback procedures
4. Verify error logging dan notification

**Expected Result**:
- Graceful handling distribution failures
- Automatic retry mechanisms
- Clear error notifications
- Comprehensive error logging

### Test Case 5.3: TMK Key Compromise Response
**ID**: TC-TMK-016
**Deskripsi**: Response procedures untuk compromised TMK
**Prasyarat**: TMK aktif tersedia
**Langkah-langkah**:
1. Simulasikan key compromise detection:
   - Anomalous usage patterns
   - Security breach indicators
   - Unauthorized access attempts
2. Test compromise response:
   - Immediate key revocation
   - Emergency key rotation
   - Terminal lockdown procedures
   - Incident reporting
3. Review educational content about incident response

**Expected Result**:
- Immediate compromise response triggered
- Emergency procedures executed
- Incident documentation generated
- Educational incident response content

---

## 6. Skenario Advanced Features

### Test Case 6.1: TMK Key Hierarchy Management
**ID**: TC-TMK-017
**Deskripsi**: Manage complex TMK key hierarchies
**Prasyarat**: Multiple TMK levels tersedia
**Langkah-langkah**:
1. Navigasi ke "Key Hierarchy" → "TMK Structure"
2. Review key hierarchy visualization:
   - ZMK level
   - TMK level
   - Session key level
   - Working key level
3. Test hierarchy operations:
   - Key derivation paths
   - Key relationship mapping
   - Hierarchy validation
4. Generate hierarchy report

**Expected Result**:
- Key hierarchy visualization working
- Hierarchical relationships defined
- Derivation paths validated
- Comprehensive hierarchy reporting

### Test Case 6.2: Terminal Session Key Initialization
**ID**: TC-TMK-018
**Deskripsi**: Inisialisasi Terminal Session Key (TSK) untuk transaksi
**Prasyarat**: TMK sudah aktif dan tersedia
**Langkah-langkah**:
1. Navigasi ke "Session Keys" → "Initialize TSK"
2. Pilih terminal dari list: `TERM-001`
3. Review TMK information yang akan digunakan:
   - Terminal ID: TERM-001
   - TMK ID: TMK-001
   - Key Type: AES-128
   - Key Status: Active
4. Configure session key parameters:
   - Session Duration: 24 hours
   - Transaction Limit: 1000
   - Key Usage Type: Encryption/Decryption
   - Security Level: High
5. Klik "Generate Session Key"
6. System otomatis menampilkan educational process:
   - Key derivation dari TMK
   - Session key generation process
   - Key uniqueness verification
   - Security validation
7. Review hasil session key:
   - Session Key ID: TSK-001-20250115
   - Clear Session Key: [hex value]
   - Encrypted Session Key: [encrypted value]
   - Key Check Value: [KCV]
   - Valid Until: [timestamp]
   - Transaction Count: 0/1000
8. Test session key activation

**Expected Result**:
- Session key berhasil di-generate dari TMK
- Key derivation process documented
- Educational content tentang session key lifecycle
- Session key parameters terkonfigurasi dengan benar
- Activation verification successful

### Test Case 6.3: Session Key Usage and Monitoring
**ID**: TC-TMK-019
**Deskripsi**: Monitor dan manage active session keys
**Prasyarat**: Session key sudah ter-inisialisasi
**Langkah-langkah**:
1. Navigasi ke "Session Keys" → "Active Sessions"
2. Review session key dashboard:
   - Active session keys list
   - Key usage statistics
   - Expiration monitoring
   - Security alerts
3. Klik pada session key untuk detail view:
   - Key information dan parameters
   - Usage history
   - Transaction log
   - Security events
4. Test real-time monitoring:
   - Transaction count updates
   - Time remaining countdown
   - Security status indicators
5. Review educational content tentang session key security

**Expected Result**:
- Session key monitoring dashboard functional
- Real-time usage statistics working
- Key expiration monitoring active
- Educational security content available

### Test Case 6.4: Session Key Rotation
**ID**: TC-TMK-020
**Deskripsi**: Rotasi session key yang aktif
**Prasyarat**: Session key aktif tersedia
**Langkah-langkah**:
1. Navigasi ke "Session Keys" → "Key Rotation"
2. Pilih session key untuk rotation: `TSK-001-20250115`
3. Review current session key status:
   - Current usage: 150/1000 transactions
   - Time remaining: 6 hours
   - Security status: Normal
4. Configure rotation parameters:
   - Rotation trigger: Time-based
   - New key duration: 24 hours
   - Preserve transaction limits: Yes
   - Grace period: 1 hour
5. Klik "Initiate Rotation"
6. System otomatis menampilkan rotation process:
   - New key generation
   - Key transition planning
   - Seamless handover procedure
7. Monitor rotation progress
8. Verify educational content about key rotation security

**Expected Result**:
- Session key rotation successfully initiated
- Rotation parameters properly configured
- Educational rotation process displayed
- Progress monitoring available
- Graceful key transition working

### Test Case 6.5: Session Key Emergency Procedures
**ID**: TC-TMK-021
**Deskripsi**: Handle emergency session key scenarios
**Prasyarat**: Session key aktif tersedia
**Langkah-langkah**:
1. Simulasikan emergency scenarios:
   - Session key compromise detection
   - Terminal disconnection events
   - Security breach indicators
2. Test emergency response procedures:
   - Immediate key revocation
   - Emergency key replacement
   - Transaction rollback procedures
   - Security incident reporting
3. Navigasi ke "Session Keys" → "Emergency Response"
4. Execute emergency key revocation:
   - Select compromised session key
   - Confirm revocation reason
   - Execute immediate revocation
   - Generate incident report
5. Review educational content about emergency procedures

**Expected Result**:
- Emergency response procedures functional
- Immediate key revocation working
- Incident documentation generated
- Educational emergency content available
- Security breach handling proper

### Test Case 6.6: Session Key Batch Operations
**ID**: TC-TMK-022
**Deskripsi**: Batch operations untuk multiple session keys
**Prasyarat**: Multiple session keys tersedia
**Langkah-langkah**:
1. Navigasi ke "Session Keys" → "Batch Operations"
2. Configure batch operation type:
   - Mass session key generation
   - Bulk key rotation
   - Scheduled key expiration
3. Select target terminals/groups:
   - Terminal group: "ATM-Network"
   - Region: "Jakarta"
   - Terminal count: 50
4. Configure batch parameters:
   - Key generation parameters
   - Rotation schedule
   - Execution timing
5. Klik "Execute Batch Operation"
6. Monitor batch execution:
   - Progress tracking
   - Success/failure rates
   - Error handling
7. Review batch operation report

**Expected Result**:
- Batch session key operations successful
- Progress monitoring functional
- Error handling for failed operations
- Comprehensive batch reporting
- Educational content about batch operations

### Test Case 6.7: DUKPT (Derived Unique Key Per Transaction) Initialization
**ID**: TC-TMK-023
**Deskripsi**: Inisialisasi DUKPT untuk generate transaction keys unik
**Prasyarat**: TMK sudah aktif dan tersedia
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Initialize DUKPT"
2. Pilih terminal dari list: `TERM-001`
3. Review TMK information yang akan digunakan:
   - Terminal ID: TERM-001
   - TMK ID: TMK-001
   - Key Type: AES-128/DES
4. Configure DUKPT parameters:
   - Key Derivation Method: ANSI X9.24
   - Initial Transaction Counter: 0
   - Maximum Transactions: 1,000,000
   - Key Usage: PIN Encryption/Data Encryption
   - Security Level: High
5. Generate Base Derivation Key (BDK):
   - BDK Generation Method: From TMK
   - BDK ID: BDK-001-TERM001
   - Key Check Value: [KCV]
6. Klik "Initialize DUKPT"
7. System otomatis menampilkan educational process:
   - DUKPT algorithm explanation
   - Key derivation hierarchy
   - Transaction counter mechanics
   - Key uniqueness guarantee
8. Review DUKPT initialization results:
   - BDK ID: BDK-001-TERM001
   - Initial Key Serial Number: KSN-001
   - Transaction Counter: 0
   - Maximum Transactions: 1,000,000
   - Key Derivation Path: TMK → BDK → Transaction Keys
9. Test DUKPT activation

**Expected Result**:
- DUKPT berhasil di-inisialisasi dari TMK
- Base Derivation Key (BDK) ter-generate dengan benar
- Key Serial Number (KSN) ter-assign
- Educational content tentang DUKPT algorithm
- Transaction counter mechanism working
- Key uniqueness guarantee validated

### Test Case 6.8: DUKPT Transaction Key Generation
**ID**: TC-TMK-024
**Deskripsi**: Generate transaction keys unik menggunakan DUKPT
**Prasyarat**: DUKPT sudah ter-inisialisasi
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Transaction Key Generator"
2. Pilih DUKPT instance: `BDK-001-TERM001`
3. Review current DUKPT status:
   - Current Transaction Counter: 0
   - Key Serial Number: KSN-001
   - Available Transactions: 1,000,000
4. Configure transaction parameters:
   - Transaction Type: PIN Encryption
   - Transaction Amount: [amount]
   - Transaction ID: TXN-001
5. Klik "Generate Transaction Key"
6. System otomatis menampilkan key generation process:
   - Increment transaction counter
   - Update Key Serial Number
   - Derive transaction key using DUKPT algorithm
   - Generate unique key for this transaction
7. Review transaction key results:
   - Transaction Key ID: TK-001-TXN001
   - Key Serial Number: KSN-001-000001
   - Transaction Counter: 1
   - Clear Transaction Key: [hex value]
   - Key Check Value: [KCV]
   - Key Uniqueness: Verified
8. Test transaction key usage simulation

**Expected Result**:
- Transaction key unik berhasil di-generate
- Key Serial Number ter-update dengan benar
- Transaction counter ter-increment
- Key uniqueness guaranteed for each transaction
- Educational process visualization working
- Forward security maintained

### Test Case 6.9: DUKPT Key Chain Management
**ID**: TC-TMK-025
**Deskripsi**: Manage DUKPT key chains dan key rotation
**Prasyarat**: DUKPT aktif dengan beberapa transaksi
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Key Chain Management"
2. Review active key chains:
   - BDK-001-TERM001: 150/1,000,000 transactions
   - Key Serial Number progression
   - Key usage statistics
3. Test key chain operations:
   - View key derivation history
   - Monitor transaction counter progress
   - Check remaining key capacity
4. Configure key rotation parameters:
   - Rotation trigger: Transaction count (80% used)
   - New BDK generation method
   - Grace period for remaining transactions
5. Simulate key chain rotation:
   - Generate new BDK
   - Migrate active transactions
   - Verify forward security
6. Review educational content about key chain lifecycle

**Expected Result**:
- Key chain management dashboard functional
- Transaction counter monitoring working
- Key rotation procedures documented
- Forward security maintained during rotation
- Educational key chain content available

### Test Case 6.10: DUKPT Security and Validation
**ID**: TC-TMK-026
**Deskripsi**: Validate DUKPT security properties dan key uniqueness
**Prasyarat**: DUKPT aktif dengan transaction history
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Security Validation"
2. Run security validation tests:
   - Key uniqueness verification
   - Forward security validation
   - Key derivation integrity check
   - Transaction counter validation
3. Test key collision detection:
   - Generate 1000 transaction keys
   - Verify all keys are unique
   - Check key strength and randomness
4. Review security properties:
   - Each key is cryptographically unique
   - Compromised key doesn't compromise others
   - Key derivation is deterministic but secure
   - Transaction counter prevents reuse
5. Generate security validation report
6. Review educational content about DUKPT security

**Expected Result**:
- Security validation tests pass
- Key uniqueness guaranteed
- Forward security validated
- Comprehensive security report generated
- Educational security content available

### Test Case 6.11: DUKPT Batch Operations and Performance
**ID**: TC-TMK-027
**Deskripsi**: Batch operations dan performance testing untuk DUKPT
**Prasyarat**: Multiple DUKPT instances tersedia
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Batch Operations"
2. Configure batch operation type:
   - Mass DUKPT initialization
   - Bulk transaction key generation
   - Key chain rotation batch
3. Select target terminals:
   - Terminal group: "POS-Network"
   - Region: "All Branches"
   - Terminal count: 100
4. Configure performance test parameters:
   - Concurrent key generation: 50 terminals
   - Transaction rate: 1000 transactions/second
   - Test duration: 10 minutes
5. Execute batch DUKPT initialization
6. Monitor performance metrics:
   - Key generation rate
   - System resource usage
   - Response times
   - Error rates
7. Execute stress test:
   - High-volume transaction simulation
   - Key generation under load
   - System stability validation
8. Review performance test report
9. Analyze scalability results

**Expected Result**:
- Batch DUKPT operations successful
- Performance metrics within acceptable limits
- System stability under high load
- Scalability validation passed
- Comprehensive performance report generated
- Educational content about DUKPT performance

### Test Case 6.12: DUKPT Emergency and Recovery Procedures
**ID**: TC-TMK-028
**Deskripsi**: Handle emergency scenarios dan recovery untuk DUKPT
**Prasyarat**: DUKPT aktif dengan transaction history
**Langkah-langkah**:
1. Simulasikan emergency scenarios:
   - BDK compromise detection
   - Transaction counter corruption
   - Key derivation failure
   - System crash during key generation
2. Test emergency response procedures:
   - Immediate BDK revocation
   - Emergency key chain rotation
   - Transaction recovery procedures
   - State restoration mechanisms
3. Navigasi ke "DUKPT" → "Emergency Response"
4. Execute BDK compromise response:
   - Identify compromised BDK
   - Revoke all affected transaction keys
   - Generate emergency replacement BDK
   - Migrate active transactions safely
5. Test transaction counter recovery:
   - Restore counter from backup
   - Validate counter integrity
   - Resume transaction processing
6. Review educational content about DUKPT emergency procedures

**Expected Result**:
- Emergency response procedures functional
- BDK compromise handling successful
- Transaction recovery working
- System restoration completed
- Emergency documentation generated
- Educational emergency content available

### Test Case 6.13: DUKPT Compliance and Audit
**ID**: TC-TMK-029
**Deskripsi**: Compliance testing dan audit trail untuk DUKPT operations
**Prasyarat**: DUKPT dengan transaction history
**Langkah-langkah**:
1. Navigasi ke "DUKPT" → "Compliance Audit"
2. Run compliance validation:
   - ANSI X9.24 standard compliance
   - PCI DSS requirements validation
   - Key management compliance
   - Transaction logging compliance
3. Review audit trail features:
   - Complete key derivation history
   - Transaction key usage logs
   - Key rotation records
   - Security event logs
4. Generate compliance reports:
   - ANSI X9.24 compliance certificate
   - PCI DSS compliance report
   - Security audit summary
   - Risk assessment report
5. Test audit log integrity:
   - Log immutability verification
   - Cryptographic log validation
   - Chain of custody verification
6. Review educational content about DUKPT compliance

**Expected Result**:
- Compliance validation passed
- Complete audit trail maintained
- Compliance reports generated
- Log integrity validated
- Educational compliance content available
- Regulatory requirements satisfied

### Test Case 6.14: TMK API Integration Testing
**ID**: TC-TMK-030
**Deskripsi**: Test API integration untuk TMK operations
**Prasyarat**: API endpoints tersedia
**Langkah-langkah**:
1. Navigasi ke "API Testing" → "TMK Operations"
2. Test API endpoints:
   - TMK generation
   - TMK distribution
   - TMK status check
   - TMK rotation
3. Review API documentation
4. Test error scenarios
5. Generate API test report

**Expected Result**:
- API endpoints functional
- Comprehensive API documentation
- Error handling working
- Test coverage complete

---

## Kesimpulan

Skenario pengujian ini mencakup aspek Terminal Key management untuk HSM Simulator dengan fokus pada:

1. **Terminal Master Key (TMK)**: Generation, distribution, dan management
2. **Terminal Session Key (TSK)**: Inisialisasi, monitoring, dan rotasi
3. **DUKPT (Derived Unique Key Per Transaction)**: Key generation unik per transaksi
4. **Key Distribution**: Secure distribution dan terminal deployment
5. **Key Management**: Monitoring, rotation, backup, dan emergency procedures
6. **Educational Features**: Interactive learning tools dan visualizations
7. **Error Handling**: Comprehensive error handling dan incident response
8. **Advanced Features**: Key hierarchies, batch operations, dan API integration

### Fitur Terminal Session Key mencakup:
- **Session Key Initialization**: Generate TSK dari TMK dengan parameter konfigurasi
- **Usage Monitoring**: Real-time monitoring transaksi dan key lifecycle
- **Key Rotation**: Rotasi session key berdasarkan waktu atau usage
- **Emergency Procedures**: Penanganan kompromisasi key dan emergency response
- **Batch Operations**: Operasi massal untuk multiple session keys

### Fitur DUKPT mencakup:
- **DUKPT Initialization**: Generate Base Derivation Key (BDK) dari TMK
- **Transaction Key Generation**: Key unik untuk setiap transaksi dengan Key Serial Number
- **Key Chain Management**: Manajemen key chain dan rotasi BDK
- **Security Validation**: Validasi key uniqueness dan forward security
- **Performance Testing**: Batch operations dan stress testing
- **Emergency Recovery**: Penanganan BDK compromise dan transaction recovery
- **Compliance Audit**: ANSI X9.24 dan PCI DSS compliance validation

### Fitur Khusus DUKPT:
- **Key Uniqueness**: Setiap transaksi menggunakan key yang cryptographically unik
- **Forward Security**: Kompromisasi satu key tidak mempengaruhi key lainnya
- **Transaction Counter**: Mencegah key reuse dengan counter mechanism
- **Key Serial Number (KSN)**: Tracking key derivation path
- **Base Derivation Key (BDK)**: Root key untuk derive transaction keys
- **ANSI X9.24 Compliance**: Standard compliance untuk DUKPT implementation

Semua skenario dirancang untuk memberikan pembelajaran yang komprehensif tentang Terminal Key operations dalam konteks perbankan dan security, dengan educational mode yang selalu aktif untuk memaksimalkan pemahaman user, khususnya untuk DUKPT yang merupakan standar industri untuk PIN encryption di POS terminals.
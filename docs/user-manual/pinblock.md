# User Manual: PIN Block Operations HSM Simulator

Dokumentasi ini menjelaskan cara menggunakan fitur PIN Block Operations pada HSM Simulator untuk pembelajaran dan demonstrasi proses enkripsi PIN block yang digunakan dalam sistem perbankan.

## 🎯 Overview

### Apa itu PIN Block?
PIN Block adalah format standar industri untuk mengenkripsi Personal Identification Number (PIN) dalam transaksi perbankan. PIN block menggabungkan PIN pengguna dengan informasi kartu (PAN) untuk menghasilkan data terenkripsi yang aman.

### Format PIN Block yang Didukung
- **ISO-0**: Format standar dengan PAN digit 5-15
- **ISO-1**: Format alternatif dengan PAN digit 5-15
- **ISO-2**: Format enhanced dengan PAN digit 4-15
- **ISO-3**: Format kustom dengan keamanan tambahan

### Educational Mode
HSM Simulator selalu beroperasi dalam mode educational, dimana semua proses dijelaskan secara visual dan langkah demi langkah untuk membantu pemahaman.

---

## 🔄 PIN Block Generation Process Flow

### Complete PIN Block Generation Flow
```mermaid
graph TB
    subgraph "Input Phase"
        A[User Input PAN] --> B[User Input PIN]
        B --> C[Select Format ISO-0/1/2/3]
        C --> D[Select Encryption Key]
    end

    subgraph "PIN Formatting"
        D --> E{PIN Length Check}
        E -->|Valid| F[Format PIN: 041234F]
        E -->|Invalid| G[Show Error Message]
        F --> H[Add Padding Characters]
    end

    subgraph "PAN Processing"
        D --> I[Parse PAN Structure]
        I --> J{Format Selection}
        J -->|ISO-0| K[Extract Digits 5-15]
        J -->|ISO-1| L[Extract Digits 5-15]
        J -->|ISO-2| M[Extract Digits 4-15]
        J -->|ISO-3| N[Custom PAN Selection]
        K --> O[Format PAN: 567890123456F]
        L --> O
        M --> P[Format PAN: Different Selection]
        N --> Q[Format PAN: Enhanced Selection]
    end

    subgraph "XOR Operation"
        H --> R[Formatted PIN Block]
        O --> R
        P --> R
        Q --> R
        R --> S[Perform XOR: PIN ⊕ PAN]
        S --> T[Clear PIN Block Result]
    end

    subgraph "Encryption"
        T --> U[Apply Encryption Key]
        U --> V[Generate Encrypted PIN Block]
        V --> W[Calculate Key Check Value]
        W --> X[Final Output]
    end

    G --> A

    style X fill:#e8f5e8
    style G fill:#ffebee
```

### Detailed PIN Block Formation Process (ISO-0 Format)
```mermaid
graph LR
    subgraph "PIN Processing"
        A[Original PIN: 1234] --> B[PIN Length: 4]
        B --> C[Control Byte: 04]
        C --> D[PIN + Control: 041234]
        D --> E[Add Padding F: 041234F]
        E --> F[Formatted PIN: 041234F]
    end

    subgraph "PAN Processing"
        G[Original PAN: 1234567890123456] --> H[PAN Length: 16]
        H --> I[Extract Digits 5-15]
        I --> J[Digits: 567890123456]
        J --> K[Add Padding F: 567890123456F]
        K --> L[Formatted PAN: 567890123456F]
    end

    subgraph "XOR Calculation"
        F --> M[Formatted PIN: 041234F]
        L --> N[Formatted PAN: 567890123456F]
        M --> O[XOR Operation]
        N --> O
        O --> P[Clear PIN Block: Result]

        subgraph "Binary XOR Example"
            Q[041234F] --> R[Binary PIN]
            S[567890123456F] --> T[Binary PAN]
            R --> U[XOR Calculation]
            T --> U
            U --> V[Binary Result]
            V --> W[Hex Result]
        end
    end

    style P fill:#e8f5e8
```

### Format Comparison Visualization
```mermaid
graph TB
    subgraph "ISO-0 Format"
        A[PAN: 1234567890123456] --> B[Use Digits 5-15]
        B --> C[PAN Block: 567890123456F]
        C --> D[Combined: PIN ⊕ PAN]
    end

    subgraph "ISO-1 Format"
        E[PAN: 1234567890123456] --> F[Use Digits 5-15]
        F --> G[PAN Block: 567890123456F]
        G --> H[Different Padding Method]
        H --> I[Combined: PIN ⊕ PAN]
    end

    subgraph "ISO-2 Format"
        J[PAN: 1234567890123456] --> K[Use Digits 4-15]
        K --> L[PAN Block: 67890123456F]
        L --> M[Enhanced Security]
        M --> N[Combined: PIN ⊕ PAN]
    end

    subgraph "ISO-3 Format"
        O[PAN: 1234567890123456] --> P[Custom Selection]
        P --> Q[Variable PAN Block]
        Q --> R[Additional Security]
        R --> S[Combined: PIN ⊕ PAN]
    end

    style A fill:#e3f2fd
    style E fill:#f3e5f5
    style J fill:#e8f5e8
    style O fill:#fff3e0
```

---

## 🔐 PIN Block Generation

### Mengakses PIN Block Generation

1. Login ke HSM Simulator
2. Navigasi ke menu **PIN Operations** → **Generate PIN Block**
3. System akan menampilkan form generation dengan educational explanations

### Input Parameters

#### 1. PAN (Primary Account Number)
- **Format**: 16 digit numeric
- **Contoh**: `1234567890123456`
- **Validasi**:
  - Harus tepat 16 digit
  - Hanya berisi angka 0-9
  - System otomatis menampilkan PAN structure analysis

#### 2. PIN (Personal Identification Number)
- **Panjang**: 4-12 digit
- **Contoh**: `1234`
- **Validasi**:
  - Minimal 4 digit, maksimal 12 digit
  - Hanya berisi angka 0-9
  - System akan menampilkan PIN strength analysis
  - Warning untuk PIN yang lemah (sequential: 1234, repeating: 1111)

#### 3. PIN Block Format
- **Pilihan**: ISO-0, ISO-1, ISO-2, ISO-3
- **Educational Info**: System otomatis menampilkan penjelasan perbedaan format
- **Recommendation**: ISO-0 untuk pembelajaran dasar

#### 4. Encryption Key
- **Pilihan**: ZMK (Zone Master Key) atau TMK (Terminal Master Key)
- **Key Check Value**: System menampilkan KCV untuk verifikasi key

### Proses Generation

Setelah menginput semua parameter:

1. **Step 1: PIN Formatting**
   - System menampilkan PIN format conversion
   - Contoh: PIN `1234` → `041234F` (dengan padding)
   - Penjelasan padding mechanism

2. **Step 2: PAN Selection**
   - System menampilkan bagian PAN yang digunakan
   - Contoh untuk ISO-0: digit 5-15 dari PAN
   - Visual representation PAN structure

3. **Step 3: XOR Operation**
   - System menampilkan perhitungan XOR antara formatted PIN dan PAN
   - Binary visualization untuk pembelajaran
   - Step-by-step calculation

4. **Step 4: Encryption**
   - System menampilkan proses enkripsi clear PIN block
   - Key usage demonstration
   - Final encrypted PIN block result

### Output Results

System menampilkan:
- **Clear PIN Block**: Hasil XOR dalam hexadecimal
- **Encrypted PIN Block**: Hasil enkripsi dengan master key
- **Key Check Value**: Untuk verifikasi key
- **Process Timeline**: Visual timeline proses generation
- **Educational Notes**: Penjelasan tambahan

---

## 🔍 PIN Block Validation

### Mengakses PIN Block Validation

1. Login ke HSM Simulator
2. Navigasi ke menu **PIN Operations** → **Validate PIN Block**
3. System menampilkan form validation

### Input Parameters

#### 1. Encrypted PIN Block
- **Format**: Hexadecimal string (32 karakter)
- **Contoh**: `AF12B3C4D5E6F789...`
- **Source**: Hasil dari generation sebelumnya

#### 2. PAN (Primary Account Number)
- **Format**: 16 digit numeric
- **Contoh**: `1234567890123456`
- **Requirement**: Harus sama dengan PAN saat generation

#### 3. Expected PIN
- **Format**: 4-12 digit
- **Contoh**: `1234`
- **Purpose**: Untuk comparison dengan extracted PIN

#### 4. PIN Block Format
- **Format**: Harus sama dengan format saat generation
- **Validation**: System validasi format compatibility

### Proses Validation

Setelah menginput semua parameter:

1. **Step 1: Decryption**
   - System mendekripsi encrypted PIN block
   - Key verification menggunakan KCV
   - Visual decryption process

2. **Step 2: PIN Extraction**
   - System mengekstrak PIN dari clear PIN block
   - Reverse process dari generation
   - Step-by-step extraction

3. **Step 3: Comparison**
   - System membandingkan extracted PIN dengan expected PIN
   - Visual comparison result
   - Match/no-match indication

### Validation Results

System menampilkan:
- **Validation Status**: ✓ VALID atau ✗ INVALID
- **Extracted PIN**: PIN yang diekstrak dari PIN block
- **Match Result**: Hasil comparison dengan expected PIN
- **Error Details**: Jika invalid, system menampilkan penjelasan error
- **Debug Information**: Educational info untuk troubleshooting

---

## 📚 Educational Features

### PIN Block Format Comparison

Akses dari: **Educational Tools** → **PIN Block Formats**

1. Input PAN dan PIN yang sama
2. Generate untuk semua format (ISO-0, ISO-1, ISO-2, ISO-3)
3. System menampilkan comparison table:
   - Format | PAN Digits Used | Padding Method | Result
   - ISO-0  | 5-15           | F padding      | [hasil]
   - ISO-1  | 5-15           | F padding      | [hasil]
   - ISO-2  | 4-15           | Different      | [hasil]
   - ISO-3  | Variable       | Special        | [hasil]

### Step-by-Step Calculator

Akses dari: **Educational Tools** → **PIN Block Calculator**

1. Input PAN dan PIN
2. System menampilkan interactive calculation:
   - **Step 1**: PIN formatting dengan visual representation
   - **Step 2**: PAN selection dengan highlighting
   - **Step 3**: XOR operation dengan binary visualization
   - **Step 4**: Result dengan explanation
3. User dapat explore different inputs dan melihat real-time changes

### Security Analysis

Akses dari: **Educational Tools** → **Security Analysis**

1. Input PAN dan PIN
2. System menampilkan security assessment:
   - **PIN Strength**: Entropy calculation, pattern detection
   - **Format Security**: Security level per format
   - **Vulnerability Analysis**: Potential weaknesses identification
   - **Recommendations**: Best practices

### Error Analysis Tool

Akses dari: **Educational Tools** → **Debug Tools**

1. Input invalid PIN block data
2. System menampilkan comprehensive error analysis:
   - **Error Type**: Format, length, character, encryption errors
   - **Location**: Bagian mana yang bermasalah
   - **Suggested Fix**: Rekomendasi perbaikan
   - **Educational Explanation**: Penjelasan mengapa error terjadi

---

## 🔄 Advanced Features

### PIN Block Transformation

Akses dari: **PIN Operations** → **Transform PIN Block**

1. Input source PIN block (format tertentu)
2. Select target format
3. System menampilkan transformation process:
   - Decrypt source PIN block
   - Extract original PIN
   - Generate new format PIN block
   - Compare results
4. Educational content tentang format compatibility

### PIN Block History

Akses dari: **PIN Operations** → **PIN Block History**

1. System menampilkan operation history:
   - Timestamp
   - Operation type (Generate/Validate)
   - PAN (masked untuk security: `1234XXXXXXXX3456`)
   - Format used
   - Result status
2. Filter berdasarkan date range, format, atau result
3. Export history untuk documentation

---

## ⚠️ Error Handling

### Common Errors dan Solutions

#### 1. Invalid PAN Format
- **Error**: "PAN must be 16 digits"
- **Solution**: Input tepat 16 digit numeric
- **Educational Info**: Penjelasan PAN structure

#### 2. Invalid PIN Length
- **Error**: "PIN must be 4-12 digits"
- **Solution**: Input PIN dengan panjang 4-12 digit
- **Educational Info**: Security considerations untuk PIN length

#### 3. Weak PIN Detection
- **Warning**: "PIN contains sequential digits"
- **Solution**: Gunakan PIN yang lebih random
- **Educational Info**: PIN security best practices

#### 4. Format Mismatch
- **Error**: "PIN block format mismatch"
- **Solution**: Gunakan format yang sama saat generation dan validation
- **Educational Info**: Format compatibility explanation

#### 5. Encryption Error
- **Error**: "Failed to decrypt PIN block"
- **Solution**: Verify key dan encrypted PIN block
- **Educational Info**: Encryption process explanation

---

## 🎓 Learning Path

### Beginner Level
1. **Basic Generation**: Generate PIN block format ISO-0
2. **Basic Validation**: Validate PIN block yang valid
3. **Format Understanding**: Pelajari perbedaan format ISO-0, ISO-1, ISO-2, ISO-3
4. **Security Basics**: Pahami keamanan PIN block

### Intermediate Level
1. **Advanced Formats**: Generate PIN block format ISO-2 dan ISO-3
2. **Transformation**: Convert antar format PIN block
3. **Error Analysis**: Debug invalid PIN blocks
4. **Security Analysis**: Analisis keamanan PIN dan PAN

### Advanced Level
1. **Batch Operations**: Generate dan validate multiple PIN blocks
2. **History Analysis**: Analisis pattern dari operation history
3. **Custom Scenarios**: Create custom test cases
4. **Implementation Understanding**: Pahami implementation details

---

## 💡 Tips dan Best Practices

### Untuk Pembelajaran
1. **Mulai dengan ISO-0**: Format paling sederhana untuk pemahaman dasar
2. **Gunakan Educational Tools**: Manfaatkan semua educational features
3. **Eksperimen dengan Berbagai Input**: Test dengan berbagai PAN dan PIN combinations
4. **Review Error Messages**: Error messages mengandung educational content
5. **Gunakan Comparison Tools**: Bandingkan hasil antar format

### Security Considerations
1. **PIN Strength**: Selalu gunakan PIN yang kuat (bukan sequential atau repeating)
2. **Format Selection**: Pilih format yang sesuai dengan kebutuhan
3. **Key Management**: Pahami perbedaan ZMK dan TMK
4. **Data Protection**: HSM Simulator secara otomatis mask sensitive data

### Troubleshooting
1. **Check Input Format**: Pastikan PAN 16 digit dan PIN 4-12 digit
2. **Verify Format Consistency**: Gunakan format yang sama untuk generation dan validation
3. **Review Educational Content**: Manfaatkan educational explanations untuk troubleshooting
4. **Use Debug Tools**: Gunakan debug tools untuk analisis error

---

## 🏦 Core Banking Integration

### PIN Block Storage di Core Banking System

Dalam sistem perbankan sebenarnya, PIN block yang dihasilkan dari HSM disimpan di database core banking issuer dengan mekanisme keamanan yang ketat.

#### Storage Architecture
```mermaid
graph TB
    subgraph "Card Issuance Process"
        A[Customer Application] --> B[Card Issuance System]
        B --> C[HSM Request PIN Block]
        C --> D[Generate Encrypted PIN Block]
        D --> E[Store to Core Banking Database]
    end

    subgraph "Database Storage Structure"
        E --> F[Customer Account Table]
        F --> G[Cards Table]
        G --> H[PIN Blocks Table]

        H --> I[PIN Block ID]
        H --> J[Card ID FK]
        H --> K[Encrypted PIN Block]
        H --> L[Key Used]
        H --> M[Format Type]
        H --> N[Creation Date]
        H --> O[Expiration Date]
        H --> P[Status: Active/Inactive]
    end

    subgraph "Security Layers"
        K --> Q[Database Encryption]
        Q --> R[Access Control]
        R --> S[Audit Logging]
        S --> T[Backup Encryption]
    end

    style D fill:#e8f5e8
    style H fill:#e3f2fd
    style K fill:#fff3e0
```

#### PIN Block Table Schema
```mermaid
erDiagram
    PIN_BLOCKS ||--o{ ACCOUNTS : "card_id"
    PIN_BLOCKS {
        string pin_block_id PK
        string card_id FK
        string encrypted_pin_block "Encrypted PIN block data"
        string encryption_key_id "Key used for encryption"
        string format_type "ISO-0/1/2/3"
        datetime creation_date
        datetime expiration_date
        string status "Active/Inactive/Expired"
        string created_by
        datetime last_modified
        string key_check_value "For verification"
    }

    ACCOUNTS {
        string account_id PK
        string customer_id
        string card_number "Masked PAN"
        string card_status
        datetime issue_date
        datetime expiry_date
    }
```

### Transaction Verification Process

Ketika nasabah melakukan transaksi (misal: di ATM), sistem perlu memverifikasi PIN yang dimasukkan.

#### Verification Flow
```mermaid
sequenceDiagram
    participant C as Customer
    participant T as Terminal/ATM
    participant S as Switch/Network
    participant I as Issuer Core Banking
    participant H as HSM
    participant D as Database

    C->>T: Insert Card + Enter PIN
    T->>T: Generate PIN Block (on-terminal)
    T->>S: Send Transaction Request + PIN Block
    S->>I: Forward Request to Issuer
    I->>D: Retrieve Stored PIN Block
    D->>I: Return Encrypted PIN Block
    I->>H: Verification Request
    Note right of H: Request contains:<br/>- Received PIN Block<br/>- Stored PIN Block<br/>- Verification Key
    H->>H: Decrypt Both PIN Blocks
    H->>H: Compare PIN Values
    H->>I: Verification Result
    I->>S: Authorization Decision
    S->>T: Response to Terminal
    T->>C: Transaction Result
```

#### Detailed Verification Process
```mermaid
graph TB
    subgraph "Transaction Initiation"
        A[Customer inserts card] --> B[Enter PIN at ATM]
        B --> C[ATM generates PIN Block]
        C --> D[Send to Acquirer]
    end

    subgraph "Network Routing"
        D --> E[Acquirer Bank]
        E --> F[Network Switch]
        F --> G[Issuer Bank]
    end

    subgraph "Issuer Verification"
        G --> H[Receive Transaction Request]
        H --> I[Retrieve Card Information]
        I --> J[Fetch Stored PIN Block]
        J --> K[Prepare HSM Request]

        K --> L[HSM Verification]
        L --> M{PIN Match?}
        M -->|Yes| N[Generate Authorization Code]
        M -->|No| O[Generate Decline Code]

        N --> P[Send Positive Response]
        O --> Q[Send Negative Response]
    end

    subgraph "Response Routing"
        P --> R[Network Switch]
        Q --> R
        R --> S[Acquirer Bank]
        S --> T[ATM Terminal]
        T --> U[Display Result to Customer]
    end

    style N fill:#e8f5e8
    style O fill:#ffebee
    style L fill:#e3f2fd
```

### HSM Verification Operations

#### Verification Request Structure
```mermaid
graph LR
    subgraph "HSM Verification Input"
        A[Received PIN Block] --> B[Stored PIN Block]
        B --> C[Verification Key]
        C --> D[Key Check Value]
        D --> E[Transaction Data]
    end

    subgraph "HSM Processing"
        E --> F[Decrypt Received PIN Block]
        F --> G[Extract PIN]

        A --> H[Decrypt Stored PIN Block]
        H --> I[Extract Stored PIN]

        G --> J{PIN Values Match?}
        I --> J

        J -->|Yes| K[Generate Positive Response]
        J -->|No| L[Generate Negative Response]

        K --> M[Include Authorization Code]
        L --> N[Include Error Code]
    end

    subgraph "Output"
        M --> O[Success Response]
        N --> P[Failure Response]
    end

    style K fill:#e8f5e8
    style L fill:#ffebee
```

### Security Considerations in Production

#### Real-world Security Measures
```mermaid
graph TB
    subgraph "Production Environment Security"
        A[HSM Physical Security] --> B[Tamper-evident Packaging]
        B --> C[Secure Facility Access]
        C --> D[Multi-factor Authentication]

        E[Network Security] --> F[Encrypted Communication]
        F --> G[Firewall Rules]
        G --> H[Intrusion Detection]

        I[Operational Security] --> J[Key Rotation Policies]
        J --> K[Audit Logging]
        K --> L[Regular Security Audits]
    end

    subgraph "Data Protection"
        M[Database Encryption] --> N[TDE - Transparent Data Encryption]
        N --> O[Column-level Encryption]
        O --> P[Secure Backup Procedures]

        Q[Access Control] --> R[Role-based Access]
        R --> S[Least Privilege Principle]
        S --> T[Regular Access Reviews]
    end

    style A fill:#e8f5e8
    style E fill:#e3f2fd
    style I fill:#fff3e0
    style M fill:#f3e5f5
    style Q fill:#e1f5fe
```

### PIN Block Lifecycle Management

#### From Issuance to Expiration
```mermaid
graph LR
    subgraph "PIN Block Lifecycle"
        A[Card Issuance] --> B[PIN Block Generation]
        B --> C[Secure Storage]
        C --> D[Active Usage]
        D --> E[Regular Verification]
        E --> F[Card Expiry]
        F --> G[PIN Block Retirement]
        G --> H[Secure Deletion]
    end

    subgraph "Maintenance Operations"
        I[Key Rotation] --> J[PIN Block Re-encryption]
        J --> K[Update Database Records]
        K --> L[Verification Testing]

        M[Card Replacement] --> N[New PIN Block Generation]
        N --> O[Old PIN Block Invalidation]
        O --> P[Cleanup Old Records]
    end

    style A fill:#e8f5e8
    style I fill:#e3f2fd
    style M fill:#fff3e0
```

### Educational vs Production Differences

#### Key Differences
| Aspect | HSM Simulator | Production System |
|--------|---------------|-------------------|
| **Key Management** | Educational keys only | Hardware Security Modules |
| **Storage** | In-memory/database | Encrypted databases with audit |
| **Network** | Local web interface | Secure network protocols |
| **Authentication** | Basic login | Multi-factor authentication |
| **Auditing** | Basic logging | Comprehensive audit trails |
| **Security** | Educational focus | Regulatory compliance |
| **Performance** | Learning optimized | High availability required |

---

## 🎫 PIN Issuance Methods

### PIN Selection dan Distribution Methods

Dalam industri perbankan, ada beberapa metode untuk nasabah memilih dan menerima PIN mereka. HSM Simulator mensimulasikan proses ini untuk pembelajaran.

### Method 1: PIN Pad/ATM First Time PIN Selection

#### Proses PIN Selection via ATM
```mermaid
sequenceDiagram
    participant C as Customer
    participant A as ATM
    participant S as Switch/Network
    participant I as Issuer System
    participant H as HSM
    participant D as Database

    Note over C,A: Step 1: Card Activation
    C->>A: Insert new card
    A->>A: Read card data
    A->>C: Display "First time use"
    A->>C: Prompt "Create new PIN"

    Note over C,A: Step 2: PIN Entry
    C->>A: Enter desired PIN (6 digits)
    A->>A: Confirm PIN entry
    C->>A: Re-enter PIN for confirmation
    A->>A: Validate PIN match

    Note over A,I: Step 3: PIN Block Generation
    A->>A: Generate PIN Block (ISO-0)
    A->>S: Send PIN Block + Card Data
    S->>I: Forward to issuer
    I->>H: Request PIN encryption
    H->>H: Generate encrypted PIN block
    H->>I: Return encrypted PIN block

    Note over I,D: Step 4: Store PIN Block
    I->>D: Store encrypted PIN block
    D->>I: Confirmation
    I->>S: Success response
    S->>A: Success to ATM
    A->>C: "PIN created successfully"

    Note over C,A: Step 5: Activation Complete
    A->>C: "Card is now active"
    A->>C: "Remove card"
```

#### Detailed PIN Pad Selection Flow
```mermaid
graph TB
    subgraph "Card Insertion & Recognition"
        A[Customer inserts new card] --> B[ATM reads card data]
        B --> C[Detect first-time use]
        C --> D[Display activation screen]
    end

    subgraph "PIN Selection Process"
        D --> E[Prompt: "Create new PIN"]
        E --> F[Customer enters PIN]
        F --> G[Validate PIN format]
        G --> H{PIN valid?}
        H -->|No| I[Show error message]
        I --> E
        H -->|Yes| J[Prompt: "Confirm PIN"]
        J --> K[Customer re-enters PIN]
        K --> L{PINs match?}
        L -->|No| M[Show mismatch error]
        M --> E
        L -->|Yes| N[Proceed to storage]
    end

    subgraph "PIN Block Generation & Storage"
        N --> O[ATM generates PIN block]
        O --> P[Encrypt with terminal key]
        P --> Q[Send to issuer system]
        Q --> R[Store in database]
        R --> S[Send confirmation]
        S --> T[Activation complete]
    end

    style T fill:#e8f5e8
    style I fill:#ffebee
    style M fill:#ffebee
```

#### ATM PIN Selection Screen Flow
```mermaid
stateDiagram-v2
    [*] --> CardInserted: Insert Card
    CardInserted --> ReadCardData: Read Card
    ReadCardData --> FirstTimeCheck: Check Status

    FirstTimeCheck --> ExistingCard: Existing PIN
    FirstTimeCheck --> NewCard: No PIN Set

    ExistingCard --> NormalOperation: Enter PIN
    NewCard --> CreatePIN: "Create New PIN"

    CreatePIN --> EnterNewPIN: Enter PIN
    EnterNewPIN --> ValidatePIN: Validate Format
    ValidatePIN --> ConfirmPIN: "Confirm PIN"
    ConfirmPIN --> VerifyMatch: Verify Match
    VerifyMatch --> PINMatched: Match ✓
    VerifyMatch --> PINMismatch: Mismatch ✗

    PINMismatch --> CreatePIN: Try Again
    PINMatched --> GeneratePINBlock: Generate PIN Block
    GeneratePINBlock --> StorePIN: Store & Activate
    StorePIN --> ActivationComplete: "Success!"

    ActivationComplete --> [*]: Remove Card

    NormalOperation --> [*]: Normal Transaction
```

### Method 2: PIN Mailer Distribution

#### PIN Mailer Generation and Delivery Process
```mermaid
graph TB
    subgraph "Batch Card Issuance"
        A[Bank decides to issue new cards] --> B[Generate card data]
        B --> C[Bulk card printing]
        C --> D[Card personalization]
    end

    subgraph "PIN Generation & Security"
        D --> E[HSM generates random PINs]
        E --> F[Generate PIN blocks]
        F --> G[Encrypt PIN blocks]
        G --> H[Store PIN blocks in database]
    end

    subgraph "PIN Mailer Production"
        E --> I[Prepare PIN data for printing]
        I --> J[Secure printing facility]
        J --> K[Print PIN mailers]
        K --> L[Quality control]
        L --> M[Secure packaging]
    end

    subgraph "Distribution Process"
        M --> N[Secure logistics]
        N --> O[Branch distribution]
        O --> P[Customer handover]
    end

    subgraph "Customer Activation"
        P --> Q[Customer receives card]
        Q --> R[Customer receives PIN mailer]
        R --> S[Separate delivery channels]
        S --> T[Customer activates card]
    end

    style E fill:#e8f5e8
    style J fill:#e3f2fd
    style T fill:#fff3e0
```

#### PIN Mailer Security Features
```mermaid
graph LR
    subgraph "Physical Security"
        A[Tamper-evident envelope] --> B[Security printing]
        B --> C[Unique serial numbers]
        C --> D[Barcode/QR tracking]
    end

    subgraph "Data Security"
        E[Encrypted PIN data] --> F[Secure transmission]
        F --> G[Access controlled printing]
        G --> H[Destruction of materials]
    end

    subgraph "Process Security"
        I[Segregation of duties] --> J[Dual control]
        J --> K[Audit trail]
        K --> L[Time-stamped logs]
    end

    subgraph "Delivery Security"
        M[Separate from cards] --> N[Secure courier]
        N --> O[Signature required]
        O --> P[Delivery confirmation]
    end

    style A fill:#e8f5e8
    style E fill:#e3f2fd
    style I fill:#fff3e0
    style M fill:#f3e5f5
```

#### PIN Mailer Lifecycle
```mermaid
sequenceDiagram
    participant BS as Banking System
    participant HSM as HSM
    participant PP as Print Provider
    participant BC as Branch Courier
    participant C as Customer

    Note over BS,HSM: Step 1: PIN Generation
    BS->>HSM: Generate 1000 random PINs
    HSM->>BS: Return encrypted PINs
    BS->>BS: Store in database

    Note over BS,PP: Step 2: Mailer Production
    BS->>PP: Send encrypted PIN data
    PP->>PP: Secure print setup
    PP->>PP: Print PIN mailers
    PP->>PP: Quality verification

    Note over PP,BC: Step 3: Secure Distribution
    PP->>BC: Secure package transfer
    BC->>BC: Verify package integrity
    BC->>BC: Store in secure area

    Note over BC,C: Step 4: Customer Handover
    C->>BC: Present identification
    BC->>BC: Verify customer identity
    BC->>C: Issue sealed PIN mailer
    C->>C: Open private location
    C->>C: Memorize PIN
    C->>C: Destroy mailer

    Note over C: Step 5: First Use
    C->>C: Use card + PIN at ATM
```

### Method 3: Instant PIN Issuance (Branch Banking)

#### Branch Counter PIN Selection
```mermaid
graph TB
    subgraph "Customer Visit"
        A[Customer visits branch] --> B[Request new card]
        B --> C[Present identification]
        C --> D[Verification complete]
    end

    subgraph "PIN Selection at Counter"
        D --> E[Teller offers PIN options]
        E --> F[Customer chooses selection method]
        F --> G{Selection Method}
        G -->|Self-selected| H[Customer enters PIN on PIN pad]
        G -->|System-generated| I[System generates random PIN]
        G -->|Phone delivery| J[Secure SMS/call delivery]
    end

    subgraph "PIN Processing"
        H --> K[Encrypt PIN immediately]
        I --> K
        J --> K
        K --> L[Store in core banking]
        L --> M[Issue card immediately]
    end

    subgraph "Customer Experience"
        M --> N[Customer receives card]
        N --> O[Customer knows PIN]
        O --> P[Can use immediately]
    end

    style P fill:#e8f5e8
```

### Comparison of PIN Issuance Methods

#### Method Comparison Table
| Method | Security | Convenience | Customer Experience | Cost | Speed |
|--------|----------|-------------|-------------------|------|-------|
| **PIN Pad/ATM** | High | High | Self-service, instant | Low | Immediate |
| **PIN Mailer** | Very High | Low | Wait for delivery | Medium | 3-7 days |
| **Branch Issuance** | High | Medium | Face-to-face service | High | Same day |
| **Phone Delivery** | Medium | High | Instant notification | Low | Immediate |
| **Mobile App** | High | Very High | Digital experience | Low | Immediate |

#### Security Considerations per Method
```mermaid
graph TB
    subgraph "PIN Pad Method"
        A[Security Advantages] --> B[PIN never in plaintext]
        B --> C[Customer creates own PIN]
        C --> D[Immediate encryption]
        A --> E[Security Risks]
        E --> F[Shoulder surfing]
        F --> G[ATM tampering]
    end

    subgraph "PIN Mailer Method"
        H[Security Advantages] --> I[Physical security features]
        I --> J[Separation from card]
        J --> K[No digital transmission]
        H --> L[Security Risks]
        L --> M[Postal interception]
        M --> N[Customer doesn't destroy mailer]
    end

    subgraph "Branch Method"
        O[Security Advantages] --> P[Identity verification]
        P --> Q[Secure environment]
        Q --> R[Immediate processing]
        O --> S[Security Risks]
        S --> T[Staff collusion]
        T --> U[PIN pad tampering]
    end

    style A fill:#e8f5e8
    style H fill:#e3f2fd
    style O fill:#fff3e0
```

---

## 📖 Glossary

### Terms
- **PAN**: Primary Account Number - Nomor kartu (16 digit)
- **PIN**: Personal Identification Number - Nomor identifikasi pribadi
- **PIN Block**: Format terenkripsi untuk mengamankan PIN
- **ZMK**: Zone Master Key - Master key untuk zona tertentu
- **TMK**: Terminal Master Key - Master key untuk terminal
- **KCV**: Key Check Value - Nilai untuk verifikasi key
- **ISO-0/1/2/3**: Standar format PIN block
- **XOR**: Exclusive OR - Operasi logika untuk PIN block generation

### Acronyms
- **HSM**: Hardware Security Module
- **PIN**: Personal Identification Number
- **PAN**: Primary Account Number
- **ZMK**: Zone Master Key
- **TMK**: Terminal Master Key
- **KCV**: Key Check Value

---

## 🔗 Related Documentation

- [Test Scenarios](../test-scenario/pinblock.md) - Skenario pengujian lengkap
- [Key Ceremony Manual](key-ceremony.md) - Panduan Key Ceremony operations
- [HSM Simulator Overview](../README.md) - General overview HSM Simulator

---

*Manual ini adalah bagian dari HSM Simulator documentation untuk tujuan pembelajaran dan pendidikan.*
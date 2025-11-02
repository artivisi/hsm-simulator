# User Manual: PIN Block Operations HSM Simulator

Dokumentasi ini menjelaskan cara menggunakan fitur PIN Block Operations pada HSM Simulator untuk pembelajaran dan demonstrasi proses enkripsi PIN block yang digunakan dalam sistem perbankan.

## Overview

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

## 🎫 1. PIN Issuance and Generation

Proses pembuatan dan penerbitan PIN block adalah langkah pertama dalam siklus hidup PIN.

### 1.1 PIN Selection Methods

#### Method 1: PIN Pad/ATM First Time PIN Selection
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

#### Method 2: PIN Mailer Distribution
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

#### Method 3: Instant PIN Issuance (Branch Banking)
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

### 1.2 PIN Block Generation Process

#### Complete PIN Block Generation Flow
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

#### Detailed PIN Block Formation Process (ISO-0 Format)
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

### 1.3 Implementation Instructions

#### Mengakses PIN Block Generation
1. Login ke HSM Simulator
2. Navigasi ke menu **PIN Operations** → **Generate PIN Block**
3. System akan menampilkan form generation dengan educational explanations

#### Input Parameters
- **PAN**: 16 digit numeric (contoh: `1234567890123456`)
- **PIN**: 4-12 digit (contoh: `1234`)
- **PIN Block Format**: ISO-0, ISO-1, ISO-2, ISO-3
- **Encryption Key**: ZMK atau TMK

#### Proses Generation
1. **Step 1**: PIN formatting (`1234` → `041234F`)
2. **Step 2**: PAN selection (extract digits 5-15 untuk ISO-0)
3. **Step 3**: XOR operation antara formatted PIN dan PAN
4. **Step 4**: Encryption dengan master key

---

## 2. PIN Block Storage and Management

Penyimpanan dan manajemen PIN block dalam sistem perbankan. Ada dua metode utama untuk menyimpan data PIN:

### 2.1 Storage Methods Comparison

#### Method A: Encrypted PIN Block Storage
```mermaid
graph TB
    subgraph "Traditional Method"
        A[Customer enters PIN: 1234] --> B[Generate PIN Block]
        B --> C[Encrypt with LMK]
        C --> D[Store Encrypted PIN Block: 32+ hex chars]
        D --> E[Database Storage]
    end

    subgraph "Verification Process"
        E --> F[Retrieve Encrypted PIN Block]
        F --> G[Decrypt with LMK]
        G --> H[Compare with Entered PIN]
    end

    style D fill:#fff3e0
    style H fill:#e3f2fd
```

#### Method B: PVV (PIN Verification Value) Storage ⭐ Recommended
```mermaid
graph TB
    subgraph "Modern Method - ISO 9564"
        A[Customer enters PIN: 1234] --> B[Generate PIN Block]
        B --> C[Encrypt with LMK]
        C --> D[Calculate PVV: SHA-256 PIN+PAN]
        D --> E[Store PVV: 4 digits only]
        E --> F[Database Storage]
    end

    subgraph "Verification Process"
        F --> G[Customer enters PIN at ATM]
        G --> H[Calculate PVV from entered PIN]
        H --> I[Compare PVV: 4 digits]
        I --> J[Faster & More Secure]
    end

    style E fill:#e8f5e8
    style J fill:#e8f5e8
```

**Comparison Table:**

| Aspect | Method A: PIN Block | Method B: PVV ⭐ |
|--------|---------------------|-----------------|
| **Storage Size** | 32+ hex characters | 4 digits |
| **Security** | Reversible with key | One-way function |
| **Industry Usage** | Legacy/Educational | **ISO 9564 Standard** |
| **Database Size** | Larger | **Smaller (90% reduction)** |
| **Compliance** | N/A | **PCI-DSS, ISO 9564** |
| **Best For** | Educational demos | **Production systems** |

**PVV Calculation Method:**
```
Input: PIN (1234) + PAN (4111111111111111)
Process: SHA-256(PIN + PAN)
Extract: First 4 decimal digits from hash
Output: PVV (e.g., "5672")
```

**Why PVV is Preferred:**
- **More secure**: Cannot be reversed to obtain PIN
- **Smaller storage**: 4 digits vs 32+ characters (90% reduction)
- **Faster verification**: Simple 4-digit comparison
- **Industry standard**: ISO 9564 compliant
- **PCI-DSS compliant**: Meets payment card industry standards

### 2.2 Storage Architecture

#### PIN Block Storage di Core Banking System
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

#### Database Schema for Both Methods
```mermaid
erDiagram
    PIN_BLOCKS ||--o{ ACCOUNTS : "card_id"
    PIN_BLOCKS {
        string pin_block_id PK
        string card_id FK
        string encrypted_pin_block "Method A: Encrypted PIN block (32+ hex)"
        string pvv "Method B: PVV (4 digits) ⭐ Recommended"
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

**Storage Examples:**

**Method A (Legacy):**
```sql
INSERT INTO pin_blocks (pin_block_id, card_id, encrypted_pin_block, format_type)
VALUES ('pb001', 'card123', '8F4A2E1D9C7B5A3E6F8D2C4B7A9E5D3C', 'ISO-0');
```

**Method B (Recommended):**
```sql
INSERT INTO pin_blocks (pin_block_id, card_id, pvv, format_type)
VALUES ('pb001', 'card123', '5672', 'ISO-0');
-- or simpler, in accounts table:
INSERT INTO accounts (pan, pvv) VALUES ('4111111111111111', '5672');
```

### 2.2 PIN Mailer Activation Process

Proses aktivasi PIN mailer adalah mekanisme penting untuk menghubungkan PIN yang dihasilkan secara acak dengan kartu yang diterima nasabah. Sistem ini menjamin keamanan melalui beberapa lapisan proteksi.

### 2.2.1 Security Principles for PIN Mailer

#### Key Security Points:
1. **PIN Never in Plain Text**: PIN selalu terenkripsi selama proses
2. **HSM Control**: Semua operasi PIN melalui HSM untuk keamanan hardware
3. **Secure Printing**: Pencetakan di fasilitas aman dengan kontrol ketat
4. **Separation of Duties**: Pemisahan tugas antara generasi, pencetakan, dan distribusi
5. **Tamper-Evident Materials**: Materi pencetakan yang menunjukkan jika ada usaha pembongkaran

#### PIN Mailer Content Security:
- **Secure Decryption**: PIN hanya di-decrypt oleh HSM saat akan dicetak
- **Plain Text in Mailer**: PIN mailer berisi plain text karena akan dibaca customer
- **Tamper-Evident Envelope**: Amplop yang menunjukkan jika ada usaha pembongkaran
- **One-Time Use**: Data PIN hanya muncul di cetakan mailer, tidak disimpan di sistem
- **Secure Destruction**: Materi sisa pencetakan dan master file dimusnahkan aman
- **Audit Trail**: Semua proses tercatat untuk audit keamanan

#### Card-PIN Binding Process
```mermaid
graph TB
    subgraph "Batch Card Production"
        A[Generate 1000 card numbers] --> B[Generate 1000 random PINs]
        B --> C[Create PIN blocks for each card]
        C --> D[Store encrypted PIN blocks]
        D --> E[Link card-PIN in database]
    end

    subgraph "Database Linking Mechanism"
        E --> F[Cards Table]
        F --> G[card_id, pan, status]

        E --> H[PIN Blocks Table]
        H --> I[pin_block_id, card_id, encrypted_pin]

        F --> J[One-to-One Relationship]
        J --> K[Each card has exactly one PIN]

        subgraph "Linking Fields"
            G --> L[Primary Key: card_id]
            I --> L
            L --> M[Foreign Key: card_id references cards.card_id]
        end
    end

    subgraph "Physical Distribution"
        E --> N[Card Production]
        N --> O[Cards mailed to customers]

        E --> P[PIN Mailer Production]
        P --> Q[PIN mailers mailed separately]

        O --> R[Customer receives card]
        Q --> R
        R --> S[Customer links card + PIN mentally]
    end

    style E fill:#e8f5e8
    style J fill:#e3f2fd
    style R fill:#fff3e0
```

#### PIN Mailer Matching System
```mermaid
sequenceDiagram
    participant SYS as Banking System
    participant DB as Database
    participant HSM as HSM
    participant PP as Print Provider
    participant C as Customer
    participant ATM as ATM Terminal

    Note over SYS,HSM: Step 1: Card-PIN Pair Generation
    SYS->>SYS: Generate card number: 1234567890123456
    SYS->>HSM: Generate random PIN: 7890
    HSM->>HSM: Create PIN block: ABC123DEF456...
    HSM->>SYS: Return encrypted PIN block
    SYS->>DB: Store encrypted PIN block with card_id link

    Note over SYS,PP: Step 2: Secure PIN Mailer Production
    SYS->>HSM: Request PIN for mailer printing
    HSM->>HSM: Decrypt PIN block temporarily
    HSM->>SYS: Return PIN in plain text: 7890
    SYS->>PP: Send PIN data to secure printing facility
    PP->>PP: Print PIN mailer with plain text PIN: 7890
    PP->>PP: Use tamper-evident envelope
    PP->>PP: Quality control and secure packaging

    Note over SYS,C: Step 3: Physical Distribution
    SYS->>SYS: Print card with PAN: 1234567890123456
    PP->>C: Mail PIN mailer (plain text visible) to customer
    SYS->>C: Mail card separately (different time/channel)

    Note over C,ATM: Step 4: Customer Activation
    C->>C: Open PIN mailer in private location
    C->>C: Read plain text PIN: 7890
    C->>C: Memorize PIN and destroy mailer
    C->>ATM: Insert card: 1234567890123456
    ATM->>ATM: Read PAN from card
    ATM->>C: Display "Enter PIN"
    C->>ATM: Enter PIN: 7890 (from memory)
    ATM->>ATM: Generate PIN block with entered PIN
    ATM->>SYS: Send transaction request

    Note over SYS,HSM: Step 5: System Verification
    SYS->>DB: Find card_id for PAN: 1234567890123456
    DB->>SYS: Return card_id and stored encrypted PIN block
    SYS->>HSM: Verify received PIN block vs stored PIN block
    HSM->>HSM: Decrypt both and compare PIN values: 7890
    HSM->>SYS: Return verification success
    SYS->>DB: Update PIN block status to Active
    SYS->>ATM: Approve transaction
    ATM->>C: Display "Transaction Successful"

    Note over SYS: PIN Activation Complete
    SYS->>SYS: Card successfully activated with correct PIN
```

#### PIN Mailer Security Flow
```mermaid
graph TB
    subgraph "HSM Processing"
        A[Random PIN Generation] --> B[Create PIN Block]
        B --> C[Store in Database Encrypted]
        C --> D[Decrypt for Printing Only]
        D --> E[Secure Deletion of Temporary Data]
    end

    subgraph "Printing Facility"
        F[Receive Plain Text PIN] --> G[Secure Environment]
        G --> H[Tamper-Proof Printing]
        H --> I[Quality Control]
        I --> J[Secure Sealed Envelope]
        J --> K[Audit Trail]
    end

    subgraph "Customer Process"
        L[Receive Sealed Mailer] --> M[Open in Private]
        M --> N[Read Plain Text PIN]
        N --> O[Memorize PIN]
        O --> P[Destroy Mailer]
    end

    subgraph "Bank Security"
        Q[Access Control] --> R[Background Checks]
        R --> S[Surveillance]
        S --> T[Segregation of Duties]
    end

    style A fill:#e8f5e8
    style H fill:#e3f2fd
    style N fill:#fff3e0
    style Q fill:#f3e5f5
```

### 2.3 PIN Block Lifecycle Management

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

---

## 3. PIN Verification Methods

Sistem menyediakan dua metode verifikasi PIN dalam zona yang sama untuk memvalidasi kebenaran PIN.

### 3.1 Method Comparison

#### Method A: PIN Block Comparison (Educational)
Membandingkan encrypted PIN block dari terminal dengan yang tersimpan di database.

**Flow:** Terminal → Core Bank → HSM
- Terminal mengirim PIN block encrypted under TPK
- Core Bank mengambil PIN block tersimpan (encrypted under LMK)
- HSM mendekripsi kedua PIN block dan membandingkan

**Cocok untuk:** Educational purposes, demonstrasi

#### Method B: PVV Verification ⭐ Recommended (ISO 9564)
Membandingkan PVV yang dikalkulasi dengan PVV yang tersimpan.

**Flow:** Terminal → Core Bank → HSM
- Terminal mengirim PIN block encrypted under TPK
- Core Bank mengambil PVV yang tersimpan (4 digits, plaintext)
- HSM mendekripsi PIN block, kalkulasi PVV, bandingkan dengan stored PVV

**Cocok untuk:** Production systems, industry standard

### 3.2 Verification Process Flow

#### Basic Verification Flow
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

### 3.2 HSM Verification Operations

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

### 3.3 Implementation Instructions

#### Mengakses PIN Block Validation
1. Login ke HSM Simulator
2. Navigasi ke menu **PIN Operations** → **Validate PIN Block**
3. System menampilkan form validation

#### Input Parameters
- **Encrypted PIN Block**: Hexadecimal string (32 karakter)
- **PAN**: 16 digit numeric (harus sama dengan saat generation)
- **Expected PIN**: 4-12 digit untuk comparison
- **PIN Block Format**: Harus sama dengan format saat generation

#### Proses Validation
1. **Step 1**: Decryption encrypted PIN block
2. **Step 2**: PIN extraction dari clear PIN block
3. **Step 3**: Comparison antara extracted PIN dan expected PIN

---

## 4. Cross-Zone PIN Block Verification

Verifikasi PIN block antar zona yang berbeda dengan encryption keys yang berbeda.

### 4.1 Cross-Zone Verification Architecture

#### Cross-Zone Verification Flow
```mermaid
graph TB
    subgraph "Zone A (Acquirer)"
        A1[Terminal in Zone A] --> A2[Generate PIN Block with ZMK-A]
        A2 --> A3[Send to Switch A]
        A3 --> A4[Zone A HSM]
    end

    subgraph "Network Interconnection"
        A4 --> B1[Inter-Zone Gateway]
        B1 --> B2[Key Translation Request]
        B2 --> B3[Secure Channel]
    end

    subgraph "Zone B (Issuer)"
        B3 --> C1[Zone B Gateway]
        C1 --> C2[Zone B HSM]
        C2 --> C3[Retrieve Stored PIN Block]
        C3 --> C4[Verify with ZMK-B]
        C4 --> C5[Verification Result]
    end

    subgraph "Response Flow"
        C5 --> D1[Result to Zone A]
        D1 --> D2[Terminal Response]
        D2 --> D3[Customer Notification]
    end

    style A4 fill:#e3f2fd
    style C2 fill:#e8f5e8
    style B3 fill:#fff3e0
```

#### Key Translation Process
```mermaid
sequenceDiagram
    participant ZA as Zone A HSM
    participant GW as Gateway
    participant ZB as Zone B HSM
    participant DB as Database

    Note over ZA,GW: Step 1: Receive Cross-Zone Request
    ZA->>GW: Forward PIN Block + Request
    GW->>GW: Validate cross-zone access
    GW->>ZA: Request key translation

    Note over ZA,ZB: Step 2: Key Translation
    ZA->>ZA: Decrypt with ZMK-A
    ZA->>ZA: Extract clear PIN
    ZA->>ZB: Send clear PIN + ZMK-B request
    ZB->>ZB: Re-encrypt with ZMK-B
    ZB->>ZA: Return translated PIN block

    Note over ZB,DB: Step 3: Verification in Zone B
    ZB->>DB: Retrieve stored PIN block
    DB->>ZB: Return encrypted PIN block
    ZB->>ZB: Verify PIN match
    ZB->>ZA: Return verification result

    Note over ZA,GW: Step 4: Response Routing
    ZA->>GW: Forward result
    GW->>ZA: Route to origin
```

### 4.2 Implementation Details

#### Cross-Zone Verification Requirements
- **Zone Master Keys**: ZMK-A dan ZMK-B harus tersedia
- **Key Translation**: Proses translasi key antar zona
- **Secure Channel**: Komunikasi terenkripsi antar zona
- **Access Control**: Validasi akses cross-zone

#### Cross-Zone Verification Steps
1. **Step 1**: Receive PIN block dari Zone A
2. **Step 2**: Decrypt PIN block dengan ZMK-A
3. **Step 3**: Extract PIN dan re-encrypt dengan ZMK-B
4. **Step 4**: Verify dengan stored PIN block di Zone B
5. **Step 5**: Return verification result ke Zone A

---

## 📚 5. Educational Features

Fitur-fitur pembelajaran untuk memahami PIN block operations.

### 5.1 PIN Block Format Comparison
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

### 5.2 Step-by-Step Calculator
Akses dari: **Educational Tools** → **PIN Block Calculator**

1. Input PAN dan PIN
2. System menampilkan interactive calculation:
   - **Step 1**: PIN formatting dengan visual representation
   - **Step 2**: PAN selection dengan highlighting
   - **Step 3**: XOR operation dengan binary visualization
   - **Step 4**: Result dengan explanation
3. User dapat explore different inputs dan melihat real-time changes

### 5.3 Security Analysis
Akses dari: **Educational Tools** → **Security Analysis**

1. Input PAN dan PIN
2. System menampilkan security assessment:
   - **PIN Strength**: Entropy calculation, pattern detection
   - **Format Security**: Security level per format
   - **Vulnerability Analysis**: Potential weaknesses identification
   - **Recommendations**: Best practices

---

## 💡 6. Tips and Best Practices

### 6.1 Untuk Pembelajaran
1. **Mulai dengan ISO-0**: Format paling sederhana untuk pemahaman dasar
2. **Gunakan Educational Tools**: Manfaatkan semua educational features
3. **Eksperimen dengan Berbagai Input**: Test dengan berbagai PAN dan PIN combinations
4. **Review Error Messages**: Error messages mengandung educational content
5. **Gunakan Comparison Tools**: Bandingkan hasil antar format

### 6.2 Security Considerations
1. **PIN Strength**: Selalu gunakan PIN yang kuat (bukan sequential atau repeating)
2. **Format Selection**: Pilih format yang sesuai dengan kebutuhan
3. **Key Management**: Pahami perbedaan ZMK dan TMK
4. **Data Protection**: HSM Simulator secara otomatis mask sensitive data

### 6.3 Troubleshooting
1. **Check Input Format**: Pastikan PAN 16 digit dan PIN 4-12 digit
2. **Verify Format Consistency**: Gunakan format yang sama untuk generation dan validation
3. **Review Educational Content**: Manfaatkan educational explanations untuk troubleshooting
4. **Use Debug Tools**: Gunakan debug tools untuk analisis error

---

## 7. Error Handling

Penanganan error dan troubleshooting.

### 7.1 Common Errors dan Solutions

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

#### 5. Cross-Zone Verification Error
- **Error**: "Cross-zone key translation failed"
- **Solution**: Verify zone keys dan translation setup
- **Educational Info**: Cross-zone security considerations

---

## 🎓 8. Learning Path

Panduan pembelajaran bertahap.

### 8.1 Beginner Level
1. **Basic Generation**: Generate PIN block format ISO-0
2. **Basic Validation**: Validate PIN block yang valid
3. **Format Understanding**: Pelajari perbedaan format ISO-0, ISO-1, ISO-2, ISO-3
4. **Security Basics**: Pahami keamanan PIN block

### 8.2 Intermediate Level
1. **Advanced Formats**: Generate PIN block format ISO-2 dan ISO-3
2. **Cross-Zone Verification**: Implement verifikasi antar zona
3. **Error Analysis**: Debug invalid PIN blocks
4. **Security Analysis**: Analisis keamanan PIN dan PAN

### 8.3 Advanced Level
1. **Key Translation**: Implement key translation antar zona
2. **Batch Operations**: Generate dan validate multiple PIN blocks
3. **Custom Scenarios**: Create custom test cases
4. **Implementation Understanding**: Pahami implementation details

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
- [Zone Key Management](../test-scenario/zone-key.md) - Manajemen kunci antar zona
- [HSM Simulator Overview](../README.md) - General overview HSM Simulator

---

*Manual ini adalah bagian dari HSM Simulator documentation untuk tujuan pembelajaran dan pendidikan.*
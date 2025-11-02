# Zone Key Management Manual

## Overview

Dokumentasi ini menjelaskan konsep Zone Master Key (ZMK) dan Zone Session/PIN Key (ZPK) dalam sistem perbankan, serta implementasi PIN Translation pada HSM Simulator.

## Latar Belakang Konsep Zone

### Mengapa Diperlukan Zone?

Dalam ekosistem perbankan modern, terdapat banyak pihak yang terlibat dalam satu transaksi:

- **Acquirer**: Bank yang menyediakan infrastruktur (ATM, POS) untuk transaksi
- **Issuer**: Bank yang menerbitkan kartu kepada nasabah
- **Beneficiary**: Penerima dana transaksi
- **Switcher**: Pihak ketiga yang menghubungkan acquirer dan issuer
- **Network Provider**: Jaringan pembayaran (Visa, Mastercard, etc)

Setiap pihak ini memiliki keamanan dan isolasi data yang berbeda. Zone memungkinkan:
1. **Isolasi Keamanan**: Setiap entitas memiliki kunci enkripsi sendiri
2. **Skalabilitas**: Mudah menambah entitas baru tanpa mengganggu yang lain
3. **Kompliance**: Memenuhi regulasi keamanan data yang berbeda per wilayah
4. **Fault Isolation**: Masalah di satu zone tidak menyebar ke zone lain

### Arsitektur Zone dalam Sistem Pembayaran

```mermaid
graph TB
    subgraph "Zone Acquirer Bank A"
        A1[ATM Network] --> A2[Acquirer Host]
        A2 --> A3[ZMK-A]
        A3 --> A4[ZPK-A1, ZPK-A2, ZPK-A3]
    end

    subgraph "Zone Issuer Bank B"
        B1[Core Banking] --> B2[Issuer Host]
        B2 --> B3[ZMK-B]
        B3 --> B4[ZPK-B1, ZPK-B2]
    end

    subgraph "Zone Switcher Network"
        S1[Switch Gateway] --> S2[ZMK-S]
        S2 --> S3[ZPK-S1, ZPK-S2]
    end

    subgraph "Zone Network Provider"
        N1[Network Gateway] --> N2[ZMK-N]
        N2 --> N3[ZPK-N1, ZPK-N2]
    end

    A2 -.->|Encrypted Transaction| S1
    S1 -.->|Translated PIN| N1
    N1 -.->|Final PIN| B2

    style A2 fill:#90EE90,stroke:#333,stroke-width:2px
    style B2 fill:#87CEEB,stroke:#333,stroke-width:2px
    style S1 fill:#FFB6C1,stroke:#333,stroke-width:2px
    style N1 fill:#DDA0DD,stroke:#333,stroke-width:2px
```

### Alur Transaksi Lintas Zone

```mermaid
sequenceDiagram
    participant Customer as Nasabah
    participant ATM as ATM Bank A
    participant Acquirer as Acquirer Host
    participant Switcher as Switcher Network
    participant Network as Network Provider
    participant Issuer as Issuer Bank B

    Customer->>ATM: Masukkan Kartu + PIN
    ATM->>Acquirer: Enkripsi PIN dengan ZPK-A
    Acquirer->>Switcher: Kirim transaction + encrypted PIN
    Switcher->>Switcher: PIN Translation ZPK-A → ZPK-S
    Switcher->>Network: Forward ke Network Provider
    Network->>Network: PIN Translation ZPK-S → ZPK-N
    Network->>Issuer: Forward ke Issuer Bank
    Issuer->>Issuer: Decrypt PIN dengan ZPK-N
    Issuer-->>Network: Response (Approve/Reject)
    Network-->>Switcher: Response
    Switcher-->>Acquirer: Response
    Acquirer-->>ATM: Response
    ATM-->>Customer: Keluarkan Kartu + Uang (jika approved)
```

## Arsitektur Zone Key

```mermaid
graph TD
    A[Master Key] --> B[Zone Master Key - ZMK]
    B --> C[Zone PIN Key - ZPK]
    B --> D[Zone Session Key - ZSK]

    E[ATM/Device 1] --> F[ZPK 1]
    E --> G[ZSK 1]

    H[POS Terminal 2] --> I[ZPK 2]
    H --> J[ZSK 2]

    K[Host System] --> L[ZMK]
    L --> F
    L --> I
```

### Pihak-Pihak yang Terlibat dalam Zone Management

```mermaid
graph LR
    subgraph "Acquirer Zone"
        A1[Acquirer] --> A2[ATM/POS Devices]
        A2 --> A3[ZMK-Acquirer]
        A3 --> A4[ZPK per Device]
    end

    subgraph "Issuer Zone"
        I1[Issuer] --> I2[Core Banking]
        I2 --> I3[ZMK-Issuer]
        I3 --> I4[ZPK for Verification]
    end

    subgraph "Switcher Zone"
        S1[Switcher] --> S2[Gateway]
        S2 --> S3[ZMK-Switcher]
        S3 --> S4[ZPK for Translation]
    end

    A1 -.->|Encrypted PIN| S1
    S1 -.->|Translated PIN| I1

    style A1 fill:#90EE90,stroke:#333,stroke-width:2px
    style I1 fill:#87CEEB,stroke:#333,stroke-width:2px
    style S1 fill:#FFB6C1,stroke:#333,stroke-width:2px
```

## Zone Master Key (ZMK)

### Konsep
Zone Master Key adalah kunci utama yang digunakan untuk:
- Enkripsi/dekripsi kunci-kunci zone lainnya
- Mengamankan distribusi kunci ke perangkat-perangkat dalam zone
- Memastikan isolasi keamanan antar zone

### Karakteristik
- Panjang: 128-bit (16 byte) atau 192-bit (24 byte) tergantung algoritma
- Triple DES atau AES
- Disimpan dalam HSM dengan proteksi khusus
- Memiliki periode validitas yang panjang

### Cara Kerja
```mermaid
sequenceDiagram
    participant Host as Host System
    participant HSM as HSM
    participant Device as ATM/POS Device

    Host->>HSM: Generate ZMK
    HSM-->>Host: Encrypted ZMK
    Host->>Device: Distribute ZMK (encrypted)
    Device->>HSM: Derive ZPK from ZMK
    HSM-->>Device: Encrypted ZPK
```

## Zone PIN Key (ZPK)

### Konsep
Zone PIN Key adalah kunci yang digunakan khusus untuk:
- Enkripsi/dekripsi PIN block
- Validasi PIN saat transaksi
- Menyimpan PIN dengan aman dalam transmisi

### Karakteristik
- Panjang: 128-bit (16 byte)
- Triple DES
- Derived dari ZMK
- Memiliki periode validitas terbatas

### Cara Kerja
```mermaid
sequenceDiagram
    participant Customer as Customer
    participant Device as ATM
    participant HSM as HSM

    Customer->>Device: Input PIN
    Device->>HSM: Create PIN Block + ZPK
    HSM->>HSM: Encrypt PIN Block
    HSM-->>Device: Encrypted PIN Block
    Device->>Host: Send Encrypted PIN Block
```

## PIN Translation

### Prinsip Dasar
PIN Translation adalah proses mengubah enkripsi PIN dari satu ZPK ke ZPK lainnya, memungkinkan transaksi lintas zone atau perangkat tanpa mengekspos PIN dalam bentuk plaintext.

### Use Cases
1. **Acquirer to Issuer**: PIN dari ATM perlu dikirim ke bank penerbit kartu
2. **Interchange Networks**: Transaksi antar jaringan pembayaran
3. **Device Migration**: Pindah dari satu perangkat ke perangkat lain

### Proses PIN Translation
```mermaid
flowchart TD
    A[Encrypted PIN Block with ZPK A] --> B[Decrypt with ZPK A]
    B --> C[Plain PIN Block]
    C --> D[Encrypt with ZPK B]
    D --> E[Encrypted PIN Block with ZPK B]

    style C fill:#ff9999,stroke:#333,stroke-width:2px
```

### Format PIN Block
PIN Block biasanya menggunakan format ISO-0:
```mermaid
graph LR
    A[PIN Block] --> B[Control Field]
    A --> C[PIN Length]
    A --> D[PIN Digits]
    A --> E[Filler]

    B -->|4 nibble| F[0000]
    C -->|4 nibble| G[PIN length]
    D -->|PIN digits x 4 nibble| H[PIN values]
    E -->|Remaining| F[FFFF...]
```

## Simulasi dengan HSM Simulator

### Prerequisites
- HSM Simulator sudah terinstall dan dijalankan
- Browser web untuk mengakses interface web
- Akses ke aplikasi web HSM Simulator (biasanya http://localhost:8080)

### Langkah-langkah Simulasi via Web Interface

#### 1. Setup Zone Master Key
1. Buka browser dan akses http://localhost:8080
2. Login dengan credentials yang telah disediakan
3. Navigate ke menu **Zone Management** → **Zone Master Keys**
4. Klik tombol **Generate New ZMK**
5. Isi form dengan parameter:
   - **Key ID**: `ZMK_001`
   - **Algorithm**: Pilih `3DES` dari dropdown
   - **Key Length**: Pilih `128-bit`
   - **Zone ID**: `ZONE_ACQUIRER_A`
   - **Description**: `ZMK for Acquirer Bank A`
6. Klik **Generate**
7. Simpan ZMK yang dihasilkan (akan ditampilkan dalam bentuk encrypted)

#### 2. Create Zone PIN Key
1. Navigate ke menu **Zone Management** → **Zone PIN Keys**
2. Klik tombol **Generate New ZPK**
3. Isi form dengan parameter:
   - **Key ID**: `ZPK_001`
   - **ZMK Reference**: Pilih `ZMK_001` dari dropdown
   - **Zone ID**: `ZONE_ATM_001`
   - **Device Type**: Pilih `ATM`
   - **Validity Period**: Set 30 hari
4. Klik **Generate**
5. ZPK akan otomatis terencrypt dengan ZMK yang dipilih

#### 3. PIN Encryption Test
1. Navigate ke menu **PIN Operations** → **PIN Encryption**
2. Isi form encryption:
   - **PIN**: Masukkan `1234`
   - **PAN**: Masukkan `1234567890123456`
   - **ZPK ID**: Pilih `ZPK_001` dari dropdown
   - **PIN Block Format**: Pilih `ISO-0` dari dropdown
3. Klik **Encrypt PIN**
4. Hasil encrypted PIN block akan ditampilkan
5. Copy hasilnya untuk testing selanjutnya

#### 4. PIN Translation Test
1. Buat ZPK kedua dengan ID `ZPK_002` untuk target zone
2. Navigate ke menu **PIN Operations** → **PIN Translation**
3. Isi form translation:
   - **Encrypted PIN Block**: Paste hasil dari step sebelumnya
   - **Source ZPK ID**: Pilih `ZPK_001`
   - **Target ZPK ID**: Pilih `ZPK_002`
   - **PAN**: Masukkan `1234567890123456` (sama seperti sebelumnya)
4. Klik **Translate PIN**
5. Hasil PIN block yang terencrypt dengan ZPK target akan ditampilkan

#### 5. PIN Verification Test
1. Navigate ke menu **PIN Operations** → **PIN Verification**
2. Isi form verification:
   - **Encrypted PIN Block**: Gunakan hasil dari step encryption atau translation
   - **ZPK ID**: Pilih ZPK yang sesuai
   - **PAN**: Masukkan `1234567890123456`
   - **Expected PIN**: Masukkan `1234`
3. Klik **Verify PIN**
4. Hasil verification akan ditampilkan (Success/Failed)

### Web Interface Navigation Guide

```mermaid
graph TD
    A[Dashboard] --> B[Zone Management]
    A --> C[PIN Operations]
    A --> D[Key Management]
    A --> E[Audit Logs]

    B --> B1[Zone Master Keys]
    B --> B2[Zone PIN Keys]
    B --> B3[Zone Session Keys]

    C --> C1[PIN Encryption]
    C --> C2[PIN Translation]
    C --> C3[PIN Verification]

    D --> D1[Key Generation]
    D --> D2[Key Rotation]
    D --> D3[Key Status]

    E --> E1[Operation Logs]
    E --> E2[Security Events]
    E --> E3[Compliance Reports]
```

### Scenario Testing

#### Scenario 1: ATM Transaction Flow
```mermaid
sequenceDiagram
    participant Customer as Customer
    participant ATM as ATM Device
    participant HSM as HSM Simulator
    participant Acquirer as Acquirer Host
    participant Issuer as Issuer Host

    Customer->>ATM: Insert Card + Input PIN
    ATM->>HSM: Encrypt PIN with ZPK-ATM
    HSM-->>ATM: Encrypted PIN Block
    ATM->>Acquirer: Send Transaction + Encrypted PIN
    Acquirer->>HSM: Translate PIN to ZPK-Issuer
    HSM-->>Acquirer: Translated PIN Block
    Acquirer->>Issuer: Forward Transaction
    Issuer->>HSM: Decrypt PIN
    HSM-->>Issuer: PIN Verification Result
```

#### Scenario 2: Cross-Zone Transaction
```mermaid
graph TD
    A[ATM Zone A] -->|ZPK-A| B[Encrypted PIN]
    B --> C[HSM]
    C -->|ZPK-A| D[Decrypt]
    C -->|ZPK-B| E[Encrypt]
    E --> F[Host Zone B]

    style C fill:#90EE90,stroke:#333,stroke-width:2px
```

## Security Considerations

### Key Management
- ZMK harus dirotasi secara berkala
- ZPK memiliki validitas terbatas
- Key split mechanism untuk HSM cluster
- Audit log untuk semua operasi kunci

### Network Security
- TLS untuk semua komunikasi dengan HSM
- IP whitelisting untuk akses HSM
- Rate limiting untuk PIN verification attempts

### Monitoring
- Alert untuk PIN translation failures
- Monitoring key usage patterns
- Audit trail untuk semua operasi kunci

## Troubleshooting

### Common Issues
1. **PIN Translation Failed**: ZPK tidak valid atau expired
2. **Key Generation Error**: HSM tidak dapat menghasilkan kunci baru
3. **Network Timeout**: Koneksi ke HSM terputus
4. **Invalid PIN Format**: PAN atau PIN tidak sesuai format

### Debug Steps via Web Interface
1. **Check Key Status**: Navigate ke **Key Management** → **Key Status**
2. **HSM Health Check**: Dashboard → **System Health** → **HSM Status**
3. **Review Logs**: Navigate ke **Audit Logs** → **Operation Logs** atau **Security Events**
4. **Validate Configuration**: **Key Management** → **Configuration Validation**
5. **Performance Monitor**: Dashboard → **Performance Metrics** → **PIN Operations**
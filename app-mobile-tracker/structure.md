# Mobile Tracker: Architecture Overview

The application follows a clean architecture pattern with clear separation of concerns:

```mermaid
graph TB
    subgraph "UI Layer"
        A[Compose Screens] --> B[ViewModels]
    end
    
    subgraph "Domain Layer"
        B --> C[Repositories]
    end
    
    subgraph "Data Layer"
        C --> D[Services]
        C --> E[Room Database]
        D --> F[Supabase]
    end
    
    subgraph "Sensor Layer"
        G[BackgroundController] --> H[PhoneSensorDataService]
        H --> E
    end
```

## Key Components

| Layer            | Description                                    |
|------------------|------------------------------------------------|
| **UI**           | Jetpack Compose screens with Material 3 design |
| **ViewModels**   | State management using Kotlin StateFlow        |
| **Repositories** | Data abstraction layer with Result pattern     |
| **Services**     | Background processing, sync, and upload        |
| **Database**     | Room database with 22+ entities                |
| **DI**           | Koin-based dependency injection                |

## Package Structure

```
kaist.iclab.mobiletracker/
├── config/           # App configuration (AppConfig, Constants)
├── db/               # Room database, DAOs, entities
│   ├── dao/          # Data Access Objects
│   └── entity/       # Database entities
├── di/               # Koin dependency injection modules
├── helpers/          # Utility helpers (Language, Supabase)
├── repository/       # Repository interfaces and implementations
│   └── handlers/     # Sensor-specific data handlers
├── services/         # Background services
│   └── upload/       # Upload services and handlers
├── ui/               # Compose UI
│   ├── components/   # Reusable UI components
│   └── screens/      # App screens
├── utils/            # Utility classes
└── viewmodel/        # ViewModels
```

## Data Flow

### Sensor Data Collection

```mermaid
sequenceDiagram
    participant Sensor
    participant PhoneSensorDataService
    participant Channel
    participant Room
    
    Sensor->>PhoneSensorDataService: onDataReceived()
    PhoneSensorDataService->>Channel: trySend(entity)
    loop Batch Processing
        Channel->>PhoneSensorDataService: receive batch
        PhoneSensorDataService->>Room: insertBatch()
    end
```

### Data Synchronization

```mermaid
sequenceDiagram
    participant AutoSyncService
    participant UploadService
    participant Room
    participant Supabase
    
    AutoSyncService->>AutoSyncService: checkConditions()
    AutoSyncService->>UploadService: uploadSensorData()
    UploadService->>Room: getUnsyncedRecords()
    Room-->>UploadService: records
    UploadService->>Supabase: upsert()
    Supabase-->>UploadService: success
    UploadService->>Room: markAsSynced()
```

## Handler Pattern

Sensor data operations are abstracted using the Handler pattern:

```kotlin
interface SensorDataHandler {
    val sensorId: String
    val displayName: String
    suspend fun getRecordCount(): Int
    suspend fun getRecordsPaginated(...): List<SensorRecord>
    suspend fun deleteAll()
}
```

Each sensor type has its own handler implementation registered in `SensorDataHandlerRegistry`.

## Services

| Service                    | Purpose                                                   |
|----------------------------|-----------------------------------------------------------|
| `PhoneSensorDataService`   | Foreground service for receiving and batching sensor data |
| `AutoSyncService`          | Background service for automatic data synchronization     |
| `PhoneSensorUploadService` | Handles uploading phone sensor data to Supabase           |
| `WatchSensorUploadService` | Handles uploading watch sensor data to Supabase           |

## Database Schema

The app uses Room database with schema export enabled. Schema files are located in:

```
app-mobile-tracker/schemas/
```

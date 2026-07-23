# EnPULSE WebView 플랫폼 구현 계획

> 이 문서는 Claude Code가 `app-mobile-tracker` 레이어에서 작업을 진행하기 위한 구현 계획입니다.
> `tracker-library`는 원칙적으로 변경하지 않습니다 — 기존에 노출된 `Broadcast`/`Ema` 액션 패턴과
> `SurveySensor`/`SurveyScheduleStorage`의 기존 파이프라인을 재사용하는 것을 전제로 설계했습니다.

---

## Step 0 — 착수 전 필수 확인 사항 (Blocking)

구현을 시작하기 전에 아래를 코드베이스에서 확인해야 합니다. 이 결과에 따라 Step 1의 진입점 구현이 달라집니다.

**확인할 것**: `app-mobile-tracker`에 폰 자체의 `TriggerEngine`(`DefaultTriggerEngine`) 인스턴스가 Koin DI 모듈에 등록되어 있는가? (예: `di/phone/TriggerModule.kt` 같은 파일에서 `DefaultTriggerEngine`을 생성하고 `PhoneTriggerActionHandler` 같은 `TriggerActionHandler` 구현체를 주입하는 코드)

**근거**: 지금까지 확인된 코드에서는 `TriggerConfigPusher`(폰)가 트리거 설정을 조립해서 **워치로 BLE 전송**하고, 실제 `DefaultTriggerEngine` + `WatchTriggerActionHandler` 배선은 `app-wearable-tracker`에만 존재합니다. `Broadcast` 액션의 `context.sendBroadcast()`도 `WatchTriggerActionHandler` 안, 즉 **워치 프로세스**에서 실행됩니다.

**분기**:
- **Path A (폰이 자체 TriggerEngine을 갖고 있다면)**: 새 `Broadcast` 액션(`action = "kaist.iclab.mobiletracker.OPEN_WEBAPP"`)을 Supabase 트리거 설정에 등록하면, 폰의 `PhoneTriggerActionHandler.handleBroadcast()`가 로컬에서 바로 `context.sendBroadcast()`를 실행 → 아래 `WebAppTriggerReceiver`가 이걸 그대로 수신. **가장 단순한 경로.**
- **Path B (트리거 평가가 워치에서만 일어난다면)**: 워치의 `Broadcast` 액션은 워치 프로세스 안에서만 발사되므로 폰에 안 닿습니다. 이 경우 `Ema` 액션과 동일한 패턴(`bleChannel.send(KEY, surveyId)`)으로, **워치 쪽에 새 BLE 키 포워딩 로직 한 줄**이 필요합니다 (`app-wearable-tracker`의 `WatchTriggerActionHandler.handleBroadcast()`에서 `action.action`이 특정 prefix — 예: `"phone."` — 로 시작하면 로컬 브로드캐스트 대신 BLE로 폰에 전달). 이건 워치 쪽 최소 수정이라 "라이브러리는 안 건드리지만 앱 레이어(워치 앱)는 한 군데 건드린다"가 됩니다.

Path B로 판명되면 Phase 1 범위를 워치 쪽 1개 파일 수정까지 포함하도록 조정하세요. 아래 Step 1~6은 **Path A/B 어느 쪽이든 폰 쪽 구현은 동일**하도록 설계했습니다 — 진입점(`BroadcastReceiver` vs `BLEDataChannel` 리스너)만 다르고, 핵심 로직(`WebAppTriggerHandler.launch(...)`)은 공유합니다.

---

## 설계 원칙 (이번 대화에서 확정된 것)

1. **`tracker-library` 비수정 원칙** — 기존 `TriggerActionConfig.Broadcast`, `SurveySensor.RESULT_ACTION_NAME`, `SurveyScheduleStorage` 인터페이스를 그대로 재사용.
2. **`scheduleId`가 타이밍 메타데이터의 유일한 키** — `SurveyScheduleStorage.addSchedule()`로 발급, `Entity.triggerTime/actualTriggerTime/surveyStartTime`이 전부 여기서 조회됨. 새 진입점도 반드시 이 스토리지를 통해 스케줄을 발급해야 함.
3. **`setSurveyResponse`는 새 저장 로직이 필요 없음** — `SurveyActivity.pushSurveyResult()`와 동일하게 `SurveySensor.RESULT_ACTION_NAME` 브로드캐스트만 쏘면 기존 `surveyResultCallback`이 흡수.
4. **`WebMessageListener` 기반 브릿지** (`addJavascriptInterface` 아님) — origin 화이트리스트로 신뢰 경계 확보.
5. **API 표면 최소화** — 브릿지는 파생/집계 데이터만 노출. raw 센서 테이블 직접 접근 없음.
6. **백그라운드 작업은 네이티브 전담** — WebView는 포그라운드에서만 pull/구독하는 소비자. 실시간 개입 판단·알림은 트리거 엔진/서비스가 처리.
7. **로컬 저장소는 기존 패턴 따름** — ObjectBox NoSQL 사용. 아래에 couchbase 쓰라고 써있는데, 이부분은 무시하고 equivalent한 ObjectBox 코드로 구현 

---

## Phase 1 — `app-mobile-tracker` 구현

### 새 패키지 구조

```
app-mobile-tracker/src/main/java/kaist/iclab/mobiletracker/webapp/
├── WebAppTriggerHandler.kt       // 핵심 로직: scheduleId 발급 + 알림 표시
├── WebAppTriggerReceiver.kt      // Path A: BroadcastReceiver 진입점
├── WebAppBleListener.kt          // Path B: BLE 리스너 진입점 (필요시)
├── WebAppRegistry.kt             // 등록된 웹앱 목록 (Phase 2 전까지 로컬 스텁)
├── WebViewSurveyActivity.kt      // WebView 컨테이너 액티비티
├── bridge/
│   ├── EnPulseBridge.kt          // WebMessageListener 구현체, 액션 디스패치
│   ├── BridgeModels.kt           // BridgeRequest/BridgeResponse/BridgeAction
│   ├── SurveyBridgeHandler.kt    // getSurvey / setSurveyResponse
│   ├── SensorBridgeHandler.kt    // getSensorData
│   └── StorageBridgeHandler.kt   // getStorageData / setStorageData
└── storage/
    ├── WebAppStorageEntity.kt    // Couchbase 문서 모델
    └── CouchbaseWebAppStorage.kt
```

### Step 1 — `WebAppTriggerHandler` (핵심 로직, 진입점 무관)

기존 `SurveySensor.triggerSurveyNotification()`과 동일한 패턴을 따르되, `DefaultSurveyActivity` 대신 `WebViewSurveyActivity`를 실행합니다.

```kotlin
class WebAppTriggerHandler(
    private val context: Context,
    private val scheduleStorage: SurveyScheduleStorage,   // SurveySensorModule과 동일 인스턴스 주입
    private val webAppRegistry: WebAppRegistry,
    private val notificationHelper: NotificationHelper
) {
    fun launch(surveyId: String, webAppId: String) {
        val webApp = webAppRegistry.get(webAppId) ?: run {
            Log.e(TAG, "Unknown webAppId: $webAppId"); return
        }

        // 기존 triggerSurveyNotification과 동일 패턴 — triggerTime을 addSchedule 시점에 기록
        val scheduleId = scheduleStorage.addSchedule(
            SurveySchedule(surveyId = surveyId, triggerTime = System.currentTimeMillis())
        )

        val fullUrl = "${webApp.url}?survey_id=$surveyId&schedule_id=$scheduleId"

        val intent = Intent(context, WebViewSurveyActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("url", fullUrl)
            putExtra("webAppId", webAppId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, scheduleId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationHelper.showWebAppTriggerNotification(
            title = webApp.notificationTitle,
            text = webApp.notificationText,
            icon = webApp.notificationIcon,
            pendingIntent = pendingIntent
        )
        // 주의: triggerSurveyNotification 원본 코드는 여기서 별도로 setActualTriggerTime을 호출하지
        // 않습니다 (addSchedule 시점의 triggerTime을 그대로 씁니다). 동일하게 따르되, 알림 표시가
        // 실패하는 케이스(SecurityException)까지 고려한다면 try/catch 후 실패 시 스케줄을 롤백하는
        // 것도 고려하세요 (기존 코드에는 없는 보강 지점입니다).
    }

    companion object { private const val TAG = "WebAppTriggerHandler" }
}
```

### Step 2 — 진입점 연결

**Path A라면** (`WebAppTriggerReceiver.kt`):
```kotlin
class WebAppTriggerReceiver(private val handler: WebAppTriggerHandler) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val surveyId = intent.getStringExtra("survey_id") ?: return
        val webAppId = intent.getStringExtra("webapp_id") ?: return
        handler.launch(surveyId, webAppId)
    }
}
// 등록 (Application 또는 DI 모듈에서):
// context.registerReceiver(receiver, IntentFilter("kaist.iclab.mobiletracker.OPEN_WEBAPP"), RECEIVER_NOT_EXPORTED)
```
Dashboard의 트리거 액션 설정에서 이 스키마를 그대로 씁니다:
```json
{
  "kind": "broadcast",
  "action": "kaist.iclab.mobiletracker.OPEN_WEBAPP",
  "extras": [
    { "key": "survey_id", "value": "34", "valueType": "String" },
    { "key": "webapp_id", "value": "hrv_biofeedback", "valueType": "String" }
  ]
}
```

**Path B라면** (`WebAppBleListener.kt`) — 새 BLE 키 하나 추가 (`Constants.BLE.KEY_WEBAPP_TRIGGER` / `AppConfig.BLEKeys.WEBAPP_TRIGGER`, 폰·워치 양쪽 Constants 동기화 필요):
```kotlin
bleChannel.addOnReceivedListener(setOf(AppConfig.BLEKeys.WEBAPP_TRIGGER)) { _, json ->
    val obj = json.jsonObject
    val surveyId = obj["surveyId"]?.jsonPrimitive?.content ?: return@addOnReceivedListener
    val webAppId = obj["webAppId"]?.jsonPrimitive?.content ?: return@addOnReceivedListener
    handler.launch(surveyId, webAppId)
}
```
+ 워치 쪽 `WatchTriggerActionHandler.handleBroadcast()`에 `action.action == "phone.OPEN_WEBAPP"`일 때 `bleChannel.send(...)`로 포워딩하는 분기 추가.

### Step 3 — `WebAppRegistry` (Phase 2 이전 임시 스텁)

Dashboard에 웹앱 등록 UI가 아직 없으므로, 우선 로컬 JSON/코드 상수로 스텁을 둡니다. Phase 2에서 Supabase 테이블로 교체될 자리이므로 인터페이스를 분리해두세요.

```kotlin
interface WebAppRegistry {
    fun get(webAppId: String): WebAppConfig?
    fun list(): List<WebAppConfig>
}

data class WebAppConfig(
    val id: String,
    val url: String,
    val notificationTitle: String,
    val notificationText: String,
    val notificationIcon: String,
    val allowedOrigin: String   // WebMessageListener origin 화이트리스트에 사용
)

// Phase 1 임시 구현 — 하드코딩 또는 assets/webapps.json 로드
class StaticWebAppRegistry(private val configs: Map<String, WebAppConfig>) : WebAppRegistry {
    override fun get(webAppId: String) = configs[webAppId]
    override fun list() = configs.values.toList()
}
```

### Step 4 — `WebViewSurveyActivity` + 브릿지 스캐폴딩

```kotlin
class WebViewSurveyActivity : ComponentActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra("url")!!
        val webAppId = intent.getStringExtra("webAppId")!!
        val webApp = webAppRegistry.get(webAppId)!!

        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            webViewClient = RestrictedWebViewClient(allowedOrigin = webApp.allowedOrigin)
        }
        setContentView(webView)

        WebViewCompat.addWebMessageListener(
            webView,
            "EnPulseNative",
            setOf(webApp.allowedOrigin),   // origin 화이트리스트 — 등록된 웹앱 도메인만
            EnPulseBridge(
                surveyHandler = surveyBridgeHandler,
                sensorHandler = sensorBridgeHandler,
                storageHandler = storageBridgeHandler,
                callerWebAppId = webAppId
            )
        )

        webView.loadUrl(url)
    }
}

// RestrictedWebViewClient: shouldOverrideUrlLoading에서 webApp.allowedOrigin 외 도메인 이동 차단
```

### Step 5 — 브릿지 액션 디스패치

```kotlin
// bridge/BridgeModels.kt
@Serializable
data class BridgeRequest(
    val requestId: String,       // TS 라이브러리가 비동기 매칭에 씀 (Phase 3에서 필요)
    val action: String,
    val payload: JsonElement
)

@Serializable
data class BridgeResponse(
    val requestId: String,
    val status: String,          // "success" | "error"
    val data: JsonElement? = null,
    val errorMessage: String? = null
)

// bridge/EnPulseBridge.kt
class EnPulseBridge(
    private val surveyHandler: SurveyBridgeHandler,
    private val sensorHandler: SensorBridgeHandler,
    private val storageHandler: StorageBridgeHandler,
    private val callerWebAppId: String
) : WebViewCompat.WebMessageListener {
    override fun onPostMessage(
        view: WebView, message: WebMessageCompat, sourceOrigin: Uri,
        isMainFrame: Boolean, replyProxy: JavaScriptReplyProxy
    ) {
        val req = Json.decodeFromString<BridgeRequest>(message.data!!)
        val response = try {
            when (req.action) {
                "getSurvey" -> surveyHandler.getSurvey(req, callerWebAppId)
                "setSurveyResponse" -> surveyHandler.setSurveyResponse(req, callerWebAppId)
                "getSensorData" -> sensorHandler.getSensorData(req, callerWebAppId)
                "getStorageData" -> storageHandler.get(req, callerWebAppId)
                "setStorageData" -> storageHandler.set(req, callerWebAppId)
                else -> BridgeResponse(req.requestId, "error", errorMessage = "Unknown action: ${req.action}")
            }
        } catch (e: Exception) {
            BridgeResponse(req.requestId, "error", errorMessage = e.message)
        }
        replyProxy.postMessage(Json.encodeToString(response))
    }
}
```

### Step 6 — 개별 브릿지 핸들러

**`SurveyBridgeHandler.getSurvey`** — `SurveyActivity.initSurvey`와 동일 패턴 (`setSurveyStartTime` 호출 포함):
```kotlin
fun getSurvey(req: BridgeRequest, callerWebAppId: String): BridgeResponse {
    val params = req.payload.jsonObject
    val surveyId = params["survey_id"]!!.jsonPrimitive.content
    val scheduleId = params["schedule_id"]?.jsonPrimitive?.content

    val survey = surveyConfigStorage.get().survey[surveyId]
        ?: return BridgeResponse(req.requestId, "error", errorMessage = "Unknown survey_id")

    if (scheduleId != null) {
        scheduleStorage.setSurveyStartTime(scheduleId, System.currentTimeMillis())
    }

    return BridgeResponse(req.requestId, "success", data = Json.encodeToJsonElement(survey.toDto()))
}
```

**`SurveyBridgeHandler.setSurveyResponse`** — 기존 `SurveySensor.RESULT_ACTION_NAME` 브로드캐스트를 그대로 재현:
```kotlin
fun setSurveyResponse(req: BridgeRequest, callerWebAppId: String): BridgeResponse {
    val params = req.payload.jsonObject
    val scheduleId = params["schedule_id"]!!.jsonPrimitive.content
    val answers = params["answers"]!!

    val intent = Intent(SurveySensor.RESULT_ACTION_NAME).apply {
        putExtra("result", Json.encodeToString(answers))
        putExtra("responseTime", System.currentTimeMillis())
        putExtra("scheduleId", scheduleId)
    }
    context.sendBroadcast(intent)

    return BridgeResponse(req.requestId, "success")
}
```

**`SensorBridgeHandler.getSensorData`** — `sync_status` 필드 포함 (지난 논의에서 확정):
```kotlin
suspend fun getSensorData(req: BridgeRequest, callerWebAppId: String): BridgeResponse {
    val params = req.payload.jsonObject
    val sensorId = params["sensor_id"]!!.jsonPrimitive.content
    val startTime = params["start_time"]!!.jsonPrimitive.long
    val endTime = params["end_time"]!!.jsonPrimitive.long

    // 워치 센서라면 동기화 요청 후 타임아웃 내 대기, 실패 시 마지막 캐시값 + stale 표시
    val (data, syncStatus) = sensorDataRepository.queryWithFreshness(
        sensorId, startTime, endTime, syncTimeoutMs = 3000
    )

    return BridgeResponse(
        req.requestId, "success",
        data = buildJsonObject {
            put("records", data)
            put("sync_status", syncStatus.name.lowercase())  // "fresh" | "stale_fallback" | "timeout"
        }
    )
}
```

**`StorageBridgeHandler`** — 웹앱별 key-value, `Couchbase*Storage` 패턴 재사용:
```kotlin
// storage/WebAppStorageEntity.kt — 문서 키를 "{webAppId}:{key}"로 구성
// storage/CouchbaseWebAppStorage.kt — get/set을 Couchbase 컬렉션에 위임
fun get(req: BridgeRequest, callerWebAppId: String): BridgeResponse {
    val key = req.payload.jsonObject["key"]!!.jsonPrimitive.content
    val value = couchbaseWebAppStorage.get(callerWebAppId, key)
    return BridgeResponse(req.requestId, "success", data = value)
}
```
`callerWebAppId`로 키를 네임스페이스 분리하는 게 중요합니다 — 웹앱 A가 웹앱 B의 storage를 못 읽게.

### Step 7 — DI 배선 (Koin)

기존 `di/phone/SurveySensorModule.kt` 패턴을 따라 `di/phone/WebAppModule.kt`를 새로 만들고, `SurveyScheduleStorage`는 **`SurveySensorModule`에 등록된 것과 동일 인스턴스**를 주입받도록 하세요 (별도 인스턴스를 새로 만들면 `getScheduleByScheduleId`가 서로 다른 스토리지를 봐서 실패합니다).

---

## 테스트 체크리스트

- [ ] `WebAppTriggerHandler.launch()` 호출 → 알림 표시 → 탭 → `WebViewSurveyActivity` 실행 → URL에 `survey_id`/`schedule_id` 정상 포함
- [ ] `getSurvey` 호출 시 `scheduleStorage`에 `surveyStartTime`이 실제로 기록되는지 (`getScheduleByScheduleId`로 직접 확인)
- [ ] `setSurveyResponse` 호출 후, 기존 `SurveySensor`가 `Entity`를 정상 생성해서 로컬 버퍼에 쌓이는지 (별도 저장 로직 없이도 동작해야 함)
- [ ] 등록되지 않은 origin에서 로드된 페이지가 브릿지 호출 시 무시되는지 (`WebMessageListener` origin 화이트리스트 검증)
- [ ] `callerWebAppId` A로 저장한 storage 데이터를 B가 조회 못 하는지
- [ ] 워치 연결이 끊긴 상태에서 `getSensorData` 호출 시 `sync_status: "stale_fallback"` 또는 `"timeout"`이 정상 반환되는지
- [ ] (Path B라면) 워치→폰 BLE 포워딩이 정상 동작하는지, `min_interval_millis` 쓰로틀링이 여전히 워치 쪽에서 걸리는지

---

## Phase 2 — Dashboard 추가 사항 (나중에)

1. **`campaign_webapp` 테이블** (Supabase, `campaign_trigger`와 동일 컨벤션):
   ```sql
   create table campaign_webapp (
       id uuid primary key default gen_random_uuid(),
       campaign_id int references campaign(id),
       webapp_id text not null,          -- 위 WebAppConfig.id와 매칭
       name text not null,
       url text not null,
       notification_title text,
       notification_text text,
       notification_icon text,
       allowed_origin text not null,
       allowed_scopes jsonb default '"all"'::jsonb,  -- Gap4 자리만 예약, 지금은 항상 "all"
       created_at timestamptz default now()
   );
   ```
2. **웹앱 등록 관리 페이지** — CRUD UI (`src/app/.../webapps` 라우트), `useCampaignConfigEdit.ts`와 유사한 Zustand 스토어 패턴.
3. **트리거 액션 편집 UI 확장** — 기존 액션 kind 선택지(`watch_ema`, `ema`, `broadcast`)에 더해, "웹앱 알림" 프리셋을 추가하면 UX가 좋습니다. 내부적으로는 여전히 `kind: "broadcast"` + 고정 `action` 문자열 + `survey_id`/`webapp_id` extras를 자동 채워주는 폼 래퍼로 구현 — 백엔드 스키마 변경 없이 UI 단에서만 처리 가능.
4. **모바일 앱의 `WebAppRegistry`를 Supabase 동기화로 교체** — 기존 `SurveyRepositoryImpl.fetchAndPersistSurveys()`와 동일 패턴(원격 fetch → 로컬 persist → in-memory 적용)으로 `WebAppRepository` 추가.

---

## Phase 3 — TS 브릿지 라이브러리 (나중에)

**패키지 제안**: `@enpulse/webapp-bridge` (또는 신규 repo `EnPULSE-webapp-sdk`), ESM+CJS 듀얼 빌드 (tsup 권장), 서드파티 웹앱 개발자가 `npm install`로 사용.

**핵심 설계 — `requestId` 기반 비동기 멀티플렉싱**:
```typescript
type BridgeAction = "getSurvey" | "setSurveyResponse" | "getSensorData" | "getStorageData" | "setStorageData";

class EnPulseBridgeClient {
  private pending = new Map<string, { resolve: (v: any) => void; reject: (e: any) => void }>();

  constructor() {
    window.addEventListener("message", (e) => {
      const res: BridgeResponse = JSON.parse(e.data);
      const entry = this.pending.get(res.requestId);
      if (!entry) return;
      this.pending.delete(res.requestId);
      res.status === "success" ? entry.resolve(res.data) : entry.reject(new Error(res.errorMessage));
    });
  }

  private call<T>(action: BridgeAction, payload: object, timeoutMs = 5000): Promise<T> {
    const requestId = crypto.randomUUID();
    return new Promise<T>((resolve, reject) => {
      this.pending.set(requestId, { resolve, reject });
      setTimeout(() => {
        if (this.pending.delete(requestId)) reject(new Error(`Bridge call '${action}' timed out`));
      }, timeoutMs);
      (window as any).EnPulseNative.postMessage(JSON.stringify({ requestId, action, payload }));
    });
  }

  getSurvey(surveyId: string, scheduleId?: string) {
    return this.call<SurveyDto>("getSurvey", { survey_id: surveyId, schedule_id: scheduleId });
  }
  setSurveyResponse(scheduleId: string, answers: Record<string, unknown>) {
    return this.call<void>("setSurveyResponse", { schedule_id: scheduleId, answers });
  }
  getSensorData(sensorId: string, startTime: number, endTime: number) {
    return this.call<{ records: unknown[]; sync_status: "fresh" | "stale_fallback" | "timeout" }>(
      "getSensorData", { sensor_id: sensorId, start_time: startTime, end_time: endTime }
    );
  }
  getStorageData<T>(key: string) { return this.call<T>("getStorageData", { key }); }
  setStorageData(key: string, value: unknown) { return this.call<void>("setStorageData", { key, value }); }
}

export const enpulse = new EnPulseBridgeClient();
```

**URL 파라미터 헬퍼** (WebView가 넘겨준 `survey_id`/`schedule_id` 파싱용):
```typescript
export function getLaunchParams() {
  const p = new URLSearchParams(window.location.search);
  return { surveyId: p.get("survey_id"), scheduleId: p.get("schedule_id") };
}
```

**타입 정의**: `BridgeRequest`/`BridgeResponse`/`SurveyDto`는 Kotlin `BridgeModels.kt`와 **1:1로 동기화**되어야 합니다. 코드 중복을 줄이려면 나중에 Kotlin `@Serializable` 클래스에서 JSON Schema를 뽑아 TS 타입을 생성하는 것도 고려할 만하지만, 지금 단계에선 수동 동기화로 충분합니다.

**배포**: 연구 도구 성격상 공개 npm보다는 GitHub Packages(비공개) 또는 그냥 `EnPULSE-dashboard`/서드파티 웹앱 레포에 git submodule/직접 복사로 배포해도 무방합니다.

---

## 열린 질문 (구현 중 확인 필요)

1. Step 0의 Path A/B 판정 — 폰이 자체 `TriggerEngine`을 갖고 있는지
2. `WebAppTriggerHandler.launch()`에서 알림 표시 실패 시 이미 발급된 `scheduleId`를 롤백할지, 그냥 둘지 (기존 `triggerSurveyNotification`도 롤백 안 함 — 일관성 위해 안 하는 쪽으로 기본 설정했으나 확인 필요)
3. `getSensorData`의 워치 동기화 타임아웃 기본값 (위 예시는 3000ms로 임시 설정 — 실제 BLE 왕복 시간 실측 후 조정)
4. Couchbase vs Room 확정 여부 (설계 원칙 7번 참고)

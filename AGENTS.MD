# Архитектурный шаблон AlcoDbOptimizer (Clean Architecture + MVI)

Обязательный гайд для ВСЕХ фич в этом репозитории. Содержит описание слоев, паттернов, соглашений и полные примеры кода. **Любая новая фича или доработка существующей обязана следовать этому шаблону**, чтобы весь код был консистентным.

Базовый пакет: `com.alcodb.optimizer`

Проект — Kotlin Multiplatform с таргетами **Android** и **JVM (Desktop)**. iOS-таргета нет.

---

## 0. Железные правила (нарушения блокируют merge)

1. **Вся логика UI — в ViewModel.** Композитные экраны НЕ содержат бизнес-состояния и НЕ принимают решений. Любой диалог, выбор, фильтр, загрузка, флаг видимости живут в `State` и меняются только через `obtainEvent(Event)`.
2. **Запрещено** использовать `remember { mutableStateOf(...) }` / `var x by remember` для состояния, влияющего на бизнес-логику. Допустимо только для чисто декоративных вещей (например, `SnackbarHostState`, высота баннера).
3. **Запрещено** вызывать `Inject.instance()` из `@Composable` и из компонентов. ViewModel получает зависимости через DI-конструктор; compose получает ViewModel через `rememberViewModel()`/`rememberInstance()`, платформенные сервисы — через `rememberInstance()`.
4. **Компоненты stateless.** Все `@Composable` ниже уровня экрана принимают `state` + `onEvent: (XxxEvent) -> Unit` (или конкретные лямбды). Никакого доступа к ViewModel, никаких собственных корутин в компонентах.
5. **Одноразовые действия** — через `viewAction = XxxAction(...)` + `LaunchedEffect(action)` + `obtainEvent(ClearAction)`.
6. **Один файл — одна ответственность.** Экран разбивается на `XxxScreen.kt` (ViewModel + подписки) и `components/XxxScreenContent.kt` (контент) + отдельные компоненты; файлы не разрастаются.
7. **Вложенные фичи разрешены** (feature может содержать дочерние feature), если диалог/флоу относится только к родителю.
8. **Минимальная видимость (MANDATORY):** функции, свойства и классы — `private`, если используются только в одном файле; `internal`, если используются только в пределах своей фичи; `public` — только для реального кросс-фичевого публичного API. Никаких `public`-членов «на всякий случай».
9. **Дизайн-система — в Theme.kt.** Все цвета, шейпы и размеры, используемые в нескольких местах, ОБЯЗАНЫ быть объявлены в `Color.kt` / `Theme.kt` и использоваться через `MaterialTheme.colorScheme.xxx` / `MaterialTheme.shapes.xxx`. Запрещено:
   - `Color(0xFF...)` / `Color.Gray` / `Color.Black` / `Color.White` в composable-функциях (кроме vector path);
   - `RoundedCornerShape(N.dp)` в composable-функциях (кроме уникальных форм, используемых в одном месте);
   - захардкоженные `Spacer(Modifier.height(N.dp))` для размеров, повторяющихся ≥2 раз.
10. **@Composable — только в compose-пакете (MANDATORY).** ВСЕ `@Composable`-функции ОБЯЗАНЫ быть объявлены в пакете, оканчивающемся на `.compose` — `feature_<name>/compose/` (включая вложенные sub-features) или `common/compose/`. Запрещено объявлять `@Composable` в пакетах `presentation/`, `domain/`, `data/`; каталог `components/` внутри `presentation/` — грубое нарушение.
11. **Запрещены полностью квалифицированные обращения к типам и функциям (MANDATORY).** Все классы, функции, enum-значения и константы из других пакетов в коде вызываются ТОЛЬКО через `import` + короткое имя. Запрещено писать полный путь пакета внутри выражений, например `com.alcodb.optimizer.common.domain.utils.Result.Success` — нужно `import com.alcodb.optimizer.common.domain.utils.Result` и `Result.Success`. То же касается типов в сигнатурах, `is`-проверок, вызовов top-level функций. Исключение — строковые литералы, где полное имя встречается как текст.
12. **Язык — русский (MANDATORY).** Все ответы агента пользователю, планы, отчёты, комментарии и ВСЕ артефакты OpenSpec (`proposal.md`, `design.md`, `tasks.md`, delta-спеки и main-спеки `openspec/specs/**/spec.md`) пишутся на русском языке. Требования и сценарии формулируются по-русски (`### Requirement: <русский текст>`, `#### Scenario: <русский текст>`). На английском остаются только структурные маркеры OpenSpec (`## Requirements`, `## ADDED/MODIFIED/REMOVED Requirements`, `### Requirement:`, `#### Scenario:`, `**WHEN**`/`**THEN**`, `**Reason**:`, `**Migration**:`, `FROM:`/`TO:`), ключевые слова SHALL/MUST, код, идентификаторы, имена файлов/путей, API и библиотек.

Подробности и примеры — ниже.

---

## 1. Общая архитектура

Проект построен на **Clean Architecture** с **MVI (Model-View-Intent)** в слое презентации.

```
┌─────────────────────────────────────────────────────┐
│                  Presentation (UI)                   │
│  ViewModel / State / Event / Action / @Composable    │
├─────────────────────────────────────────────────────┤
│                    Domain (Бизнес-логика)             │
│  Repository (интерфейсы) / Model / UseCase / Error   │
├─────────────────────────────────────────────────────┤
│                   Data (Реализации)                  │
│  RepositoryImpl / DataSource / DTO / Mappers         │
├─────────────────────────────────────────────────────┤
│                Common / Core (Инфраструктура)        │
│  DI / Platform / Utils / Compose-компоненты          │
└─────────────────────────────────────────────────────┘
```

### Слои (сверху вниз)

**1. Presentation (UI)** — `feature_xxx/compose/` + `feature_xxx/presentation/`
- Компоненты: `BaseSharedViewModel<State, Action, Event>`, `State` (data class), `Event` (sealed interface), `Action` (sealed interface), `@Composable` функции.
- Содержит только логику UI. Взаимодействует с Domain через репозитории или Use Case'ы.
- Зависит только от Domain (через интерфейсы).

**2. Domain (Бизнес-логика)** — `feature_xxx/domain/`
- Компоненты: `Repository` (интерфейсы), `UseCase`, `Model`, `Error`, `Dispatchers`.
- Не зависит от Presentation и Data слоев. Не имеет зависимостей на Android/JVM-фреймворки/Ktor/Firebase.

**3. Data** — `feature_xxx/data/`
- Компоненты: `RepositoryImpl`, `DataSource`, `DTO`, мапперы.
- Реализует интерфейсы из Domain. Маппит DTO <-> Model. Содержит платформенные реализации.

**4. Common / Core** — `common/`
- `core/di/` (Inject), `core/platform/` (expect/actual), `presentation/utils/` (BaseSharedViewModel), `domain/utils/` (Result, Error, Dispatchers), `compose/` (общие UI).

---

## 2. Иерархия пакетов (Feature-first)

Каждая фича содержит все слои внутри одного пакета. `compose/`, `presentation/`, `domain/`, `data/` — на одном уровне. **ВСЕ `@Composable`-функции — только в `compose/`** (см. §0.10): в `presentation/` нет ни composable-функций, ни каталога `components/`.

```
feature_<name>/
├── compose/                    # @Composable экраны и UI-компоненты
│   ├── <Name>Screen.kt         # ТОЛЬКО ViewModel + подписки + диалоги
│   ├── components/             # <Name>ScreenContent.kt + отдельные компоненты
│   └── dialogs/                # диалоги экрана (если их несколько)
├── presentation/               # MVI компоненты
│   ├── <Name>ViewModel.kt
│   ├── coordinators/           # координаторы под-флоу (state + Result)
│   ├── di/                     # <Name>ViewModelModule
│   └── models/
│       ├── <Name>State.kt
│       ├── <Name>Event.kt
│       └── <Name>Action.kt (если требуется)
├── domain/                     # Domain слой
│   ├── di/                     # <Name>DomainModule
│   ├── repository/             # <Name>Repository.kt (композитный) + узкие контракты
│   ├── models/
│   ├── use_cases/
│   └── utils/                  # <Name>Error.kt
└── data/                       # Data слой
    ├── di/                     # <Name>DataModule
    ├── datasource/
    │   ├── <Name>DataSource.kt (композитный интерфейс)
    │   └── <Name>DataSourceImpl.kt
    ├── repository/             # <Name>RepositoryImpl (фасад) + роли
    └── models/                 # DTO фичи
```

### Вложенные фичи (sub-features)

Разрешены и поощряются, если дочерний флоу/диалог относится только к родителю:

- `feature_scan/feature_user_details/`
- `feature_scan/feature_lost_data_search/`

Каждая вложенная фича имеет собственную структуру `compose/presentation/domain/data` и собственные DI-модули.

Вложенная фича может не иметь UI: тогда в ней только `presentation/` (координатор машины состояний и его модели) и DI-модуль, а каталога `compose/` нет.

### Общий код (не привязанный к фичам)

```
common/
├── compose/
│   ├── components/              # общие UI-компоненты
│   ├── utils/                   # общие composable-утилиты
│   └── theme/                   # Color.kt, Theme.kt, дизайн-токены
├── core/
│   ├── di/Inject.kt
│   └── platform/                # PlatformConfiguration, PlatformSDK
├── data/
│   ├── datasource/              # FirestoreDataSource, LocalStorageDataSource
│   ├── models/                  # общие DTO
│   └── di/                      # CommonDataModule
├── domain/
│   ├── di/                      # CommonDomainModule
│   ├── models/
│   ├── repository/
│   └── utils/                   # Result.kt, Error.kt, Dispatchers.kt
├── presentation/utils/          # BaseSharedViewModel
└── utils/                       # FileUtils и прочие утилиты
```

---

## 3. Ключевые компоненты

### 3.1. Result и Error — система обработки ошибок

`common/domain/utils/`

```kotlin
interface Error

sealed interface Result<out D, out E : Error> {
    data class Success<out D>(val data: D) : Result<D, Nothing>
    data class Error<out E : DomainError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E>
inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E>
inline fun <T, E : Error> Result<T, E>.onError(action: (E) -> Unit): Result<T, E>
fun <T> Result<T, Error>.getOrNull(): T?

typealias DomainError = Error
val EmptySuccessResult = Result.Success(Unit)
typealias EmptyResult<E> = Result<Unit, E>
```

Ошибки фичи — `enum`:

```kotlin
package com.alcodb.optimizer.feature_settings.domain.utils

enum class SettingsError : Error {
    ClearDataError,
    SaveNameError,
    FetchSettingsError,
}
```

Общие ошибки data-слоя живут в `common/domain/utils/DataError.kt`:

```kotlin
enum class DataError : Error {
    NotInitialized,
    InitializationError,
    ReadError,
    WriteError,
    DeleteError,
}
```

**Правила:**
- Все методы Domain, Data и DataSource возвращают `Result<T, Error>` или `EmptyResult<Error>`.
- Исключение — синхронные методы без I/O и без бизнес-результата (`isInitialized()`, `getReadCount()`, `resetReadCount()`): они остаются прямыми.
- DataSource оборачивает платформенные исключения в `DataError`; фича маппит их в свой `<Name>Error`.
- В ViewModel/координаторе результат обрабатывается через `.onSuccess { }` / `.onError { }`, трансформируется через `.map { }`.
- Для игнорирования данных — `.asEmptyDataResult()`; для чтения с fallback — `.getOrNull()`.

### 3.2. Dispatchers

```kotlin
interface Dispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
```

- Внедряется в репозитории, датасурсы и Use Case'ы через конструктор.
- Все IO-операции оборачиваются в `withContext(dispatchers.io) { ... }`.
- Регистрируется как `bindSingleton` в `commonDomainModule`.

### 3.3. BaseSharedViewModel

`common/presentation/utils/BaseSharedViewModel.kt`:

```kotlin
abstract class BaseSharedViewModel<State : Any, Action, Event>(initialState: State) : ViewModel() {
    fun viewStates(): StateFlow<State>                 // постоянное состояние экрана
    fun viewActions(): SharedFlow<Action?>             // одноразовые действия (replay=1, DROP_OLDEST)

    protected var viewState: State                     // viewState = viewState.copy(...)
    protected var viewAction: Action?                  // viewAction = SomeAction(...)
    protected fun withViewModelScope(block: suspend CoroutineScope.() -> Unit): Job

    abstract fun obtainEvent(viewEvent: Event)          // точка входа для ВСЕХ событий UI
}
```

### 3.4. Inject — обертка для Kodein-DI

```kotlin
object Inject {
    val di: DirectDI
    fun createDependencies(tree: DirectDI)
    fun isInitialized(): Boolean
    inline fun <reified T> instance(tag: Any? = null): T = di.instance(tag)
}
```

DI инициализируется в `PlatformSDK.init(configuration)`. ViewModel-модули импортируются **до** Domain/Data модулей (работает благодаря ленивому разрешению Kodein).

---

## 4. MVI-паттерн (главный раздел!)

### 4.1. Компоненты

**State** — неизменяемый `data class`, описывающий ВСЁ состояние экрана и диалогов:

```kotlin
data class SettingsScreenState(
    val currentName: String = "",
    val currentEmail: String = "",
    val isLoading: Boolean = false,
    val openedShareDialog: Boolean = false,          // флаг видимости диалога
) : Any
```

**Event** — `sealed interface` для событий от UI, имена в прошедшем времени:

```kotlin
sealed interface SettingsScreenEvent {
    data object ClearAction : SettingsScreenEvent
    data object OnLogoutClick : SettingsScreenEvent
    data object OnShareClose : SettingsScreenEvent
    data class OnNameChanged(val name: String) : SettingsScreenEvent
    data class OnGoogleLoginResult(val idToken: String?) : SettingsScreenEvent
}
```

**Action** — `sealed interface` для одноразовых действий (снэкбар, навигация, открыть URI):

```kotlin
sealed interface SettingsScreenAction {
    data class ShowError(val error: Error) : SettingsScreenAction
    data object OpenAppSettings : SettingsScreenAction
}
```

### 4.2. Поток данных

```
UI ──(Event)──> ViewModel.obtainEvent()
                    │
                    ├── viewState = viewState.copy(...)   // обновление состояния
                    ├── withViewModelScope {               // асинхронная операция
                    │     repository.method()
                    │         .onSuccess { viewState = ... }
                    │         .onError { viewAction = ShowError(it) }
                    │}
                    │
                    ├── viewState ──(StateFlow)──> UI (рекомпозиция)
                    └── viewAction ──(SharedFlow)──> UI (LaunchedEffect)
```

### 4.3. Правила MVI (обязательные)

1. **Вся логика — внутри `obtainEvent()`** (через приватные методы-обработчики). Никакой логики в Compose.
2. **Изменение состояния** — только `viewState = viewState.copy(...)`. Иммутабельность.
3. **Одноразовые действия** — `viewAction = SomeAction(...)`.
4. После обработки `Action` в UI — вызвать `obtainEvent(ClearAction)`.
5. **Асинхронность** — только через `withViewModelScope { }`.
6. **Compose-функции не имеют доступа к ViewModel** — только `state` + `onEvent`.
7. **Диалоги и глобальные окна** (уведомления, подтверждения, детали) управляются флагами в `State` и событиями `Event`. НЕ локальным `remember` и НЕ синглтонами, доступными из compose.
8. Если одно и то же окно показывается с нескольких экранов, каждый экран держит свой флаг видимости в своём `State`, а сам диалог остается stateless-компонентом в фиче, которой он принадлежит.
9. **Нельзя** хранить бизнес-состояние в `remember`/`mutableStateOf`/глобальных синглтонах для чтения в compose; состояние приходит только из `viewStates()`.
10. **Никаких захардкоженных строк в UI.** Все тексты, видимые пользователю (заголовки, кнопки, подписи, описания, пустые состояния, snackbar, диалоги), берутся ТОЛЬКО из Compose Resources (`values/strings.xml` + `values-ru/strings.xml`) через `stringResource()`. Запрещено вставлять литералы в `Text(...)`, `title = "..."`, `captionText = "..."` и т.п. Строковые литералы в коде допустимы только для: логов, дебаг-сообщений, ключей/тэгов, динамических значений (числа, даты, email), имён классов/констант. Новый ключ добавляется в ОБА локаля (default + `values-ru`); ключ, оставшийся без перевода, — баг.
11. **Форматированные строки — только через formatArgs (MANDATORY).** Композитный код получает тексты с плейсхолдерами через `stringResource(Res.string.<key>, arg1, arg2, ...)`. Compose Multiplatform поддерживает ТОЛЬКО позиционные плейсхолдеры `%N$s` и `%N$d`; формы с точностью, шириной, флагами или float-конверсией (`%1$.0f`, `%1.2f`, `%1$020d`) запрещены — CMP не распознаёт их и выводит буквальным текстом. Целые числа передаются как `Int` через `%N$d`. Ручной `.replace(...)` плейсхолдеров допустим ТОЛЬКО в не-composable коде (ViewModel, domain, data), где `stringResource()` невызываем.

### 4.4. Пример ViewModel

```kotlin
class SettingsScreenViewModel(
    private val settingsRepository: SettingsScreenRepository,
    private val authRepository: AuthRepository,
    private val dispatchers: Dispatchers,
) : BaseSharedViewModel<SettingsScreenState, SettingsScreenAction, SettingsScreenEvent>(
    initialState = SettingsScreenState()
) {
    override fun obtainEvent(viewEvent: SettingsScreenEvent) {
        when (viewEvent) {
            SettingsScreenEvent.ClearAction -> viewAction = null
            SettingsScreenEvent.OnLogoutClick -> onLogout()
            SettingsScreenEvent.OnShareClose -> viewState = viewState.copy(openedShareDialog = false)
            is SettingsScreenEvent.OnNameChanged -> onNameChanged(viewEvent.name)
            is SettingsScreenEvent.OnGoogleLoginResult -> onLogin(viewEvent.idToken)
        }
    }

    private fun onLogout() {
        withViewModelScope {
            authRepository.logout()
                .onSuccess { viewAction = SettingsScreenAction.OpenAppSettings }
                .onError { viewAction = SettingsScreenAction.ShowError(it) }
        }
    }

    private fun onNameChanged(name: String) {
        viewState = viewState.copy(currentName = name)
        withViewModelScope {
            settingsRepository.saveName(name)
                .onError { viewAction = SettingsScreenAction.ShowError(it) }
        }
    }
}
```

---

## 5. Композиция экрана (Screen / Content / components)

### 5.1. `XxxScreen.kt` — ТОЛЬКО ViewModel + подписки

Файл экрана содержит: получение ViewModel, подписку на `state`/`action`, обработку `action` через `LaunchedEffect`, размещение диалогов. **Без UI-логики и без `remember` бизнес-состояния.**

```kotlin
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val viewModel: SettingsScreenViewModel by rememberViewModel()
    val state by viewModel.viewStates().collectAsStateWithLifecycle()
    val action by viewModel.viewActions().collectAsStateWithLifecycle()

    LaunchedEffect(action) {
        action?.let { action ->
            when (action) {
                is SettingsScreenAction.ShowError -> showError(snackbarHostState, action.error)
                SettingsScreenAction.OpenAppSettings -> openAppSettings(platformConfiguration)
            }
            viewModel.obtainEvent(SettingsScreenEvent.ClearAction)
        }
    }

    Scaffold(...) {
        SettingsScreenContent(
            state = state,
            onEvent = viewModel::obtainEvent,
        )
    }

    if (state.openedDeleteAccountDialog) { DeleteAccountDialog(...) }
}
```

### 5.2. `components/XxxScreenContent.kt` — контент экрана

Принимает `state`, `onEvent` и несколько UI-параметров. Рисует сетку/список и расставляет компоненты. **Не знает о ViewModel.**

```kotlin
@Composable
internal fun SettingsScreenContent(
    state: SettingsScreenState,
    onEvent: (SettingsScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(...) {
        item(key = "account") { AccountCard(state.loggedUser, onEvent) }
        item(key = "settings") { SettingsCard(state, onEvent) }
    }
}
```

### 5.3. Отдельные компоненты

Каждый компонент — отдельный файл (`components/AccountCard.kt`, `components/ThemeCard.kt`, ...). Диалоги — в `dialogs/`. Один файл = одна ответственность.

---

## 6. Репозитории и DataSource

- **Репозиторий (Domain):** контракт данных фичи. Большой контракт дробится на узкие интерфейсы по ответственности, а основной `<Name>Repository` становится композитом без собственных членов.
- **DataSource (Data):** интерфейс + реализация источника. Возвращает `Result`, не бросает исключения наружу; ошибки платформы маппятся в `DataError`.
- **Impl Репозитория (Data):** фасад, который держит роли и делегирует им вызовы. Собственной логики в фасаде нет.

### 6.1. Композитный контракт

```kotlin
interface ScanRepository :
    ScanSessionRepository,
    UserDeletionRepository,
    BackupRepository,
    RegistryCleanupRepository,
    AuthCleanupRepository,
    LostDataSearchRepository
```

Каждый узкий интерфейс объявляется в отдельном файле `domain/repository/` и содержит только методы своей ответственности. Потребитель (координатор/UseCase) принимает узкий интерфейс, а не весь композит, если ему нужна одна группа операций.

### 6.2. Фасад и роли реализации

```kotlin
class ScanRepositoryImpl(
    scanDataSource: ScanDataSource,
    firestoreDataSource: FirestoreDataSource,
    localStorageDataSource: LocalStorageDataSource,
    dispatchers: Dispatchers,
) : ScanRepository {
    private val state = ScanStateStore(localStorageDataSource)
    private val runner = ScanRunner(/* ... */)
    private val deletion = UserDeletionService(/* ... */)

    override suspend fun startScan(maxReads: Int): EmptyResult<Error> = runner.startScan(maxReads)
    override suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> =
        deletion.deleteUser(userId, isAnonymous)
}
```

Правила:
1. Фасад содержит только делегирующие однострочники и создание ролей; любая логика — в роли.
2. Роли живут в `data/repository/` и имеют `internal`-видимость. Одна роль — одна ответственность (сканирование, удаление, бэкапы, реестр, Auth, поиск).
3. Общее состояние (прогресс, списки, флаги) живёт в одном `internal`-классе состояния (`ScanStateStore`), роли не держат собственных копий.
4. Общие зависимости (провайдер коллекций, детектор, файловое хранилище бэкапов) — отдельные классы, а не приватные методы фасада.
5. Если роль превышает пороги detekt или начинает отвечать за два сценария — дробится дальше, а не подавляется baseline.

Мапперы DTO <-> Model — extension-функции в пакете DTO: `.toModel()`, `.toDto()`.

---

## 7. UseCase'ы и координаторы (опционально)

### 7.1. UseCase

Используется, если логика переиспользуется в нескольких ViewModel или слишком сложна для репозитория.

```kotlin
class GetContacts(private val repository: ShareRepository) {
    suspend operator fun invoke(mail: String): Result<ContactsResult, Error> = coroutineScope {
        // ...
    }
}
```

Правила: класс с `operator fun invoke()` (один публичный метод); зависит только от репозиториев; регистрируется в DI через `bindProvider`/`bindSingleton`.

### 7.2. Координатор под-флоу

Если ViewModel разрастается (диалог с собственной машиной состояний, отдельный сценарий восстановления/поиска/очистки), его под-флоу выносится в `presentation/coordinators/`.

```kotlin
internal class RestoreCoordinator(
    private val repository: BackupRepository,
) {
    fun show(state: ScanScreenState, entries: List<BackupEntry>): ScanScreenState =
        state.copy(isRestoreDialogVisible = true, backupEntries = entries)

    fun dismiss(state: ScanScreenState): ScanScreenState =
        state.copy(isRestoreDialogVisible = false)

    suspend fun restore(entry: BackupEntry): EmptyResult<Error> = repository.restoreUser(entry)
}
```

Правила:
1. Координатор **stateless**: не хранит `State`, не создаёт flow и не держит `viewModelScope`. Он принимает текущий `State` и возвращает новый.
2. Асинхронные методы координатора возвращают `Result`; ViewModel сам вызывает их во `withViewModelScope { }` и обновляет `viewState`.
3. ViewModel остаётся единственным владельцем состояния и единственным местом, где эмитятся `Action`; координатор может эмитить только `Result`/модели.
4. Координатор зависит только от узкого репозиторного контракта.
5. Всё, что относится к одному диалогу/сценарию, живёт в его координаторе; ViewModel содержит только диспетчеризацию `when (event)` и запуск координатора.

---

## 8. DI (Kodein)

### 8.1. Модули

```kotlin
// Data
val newFeatureDataModule = DI.Module("NewFeatureDataModule") {
    bindProvider<NewFeatureDataSource> {
        NewFeatureDataSourceImpl(firestore = instance())
    }
}

// Domain
val newFeatureDomainModule = DI.Module("NewFeatureDomainModule") {
    bindProvider<NewFeatureRepository> {
        NewFeatureRepositoryImpl(
            dataSource = instance(),
            dispatchers = instance(),
        )
    }
}

// ViewModel
val newFeatureViewModelModule = DI.Module("NewFeatureViewModelModule") {
    bindProvider {
        NewFeatureViewModel(
            repository = instance(),
            dispatchers = instance(),
        )
    }
}
```

### 8.2. Регистрация в PlatformSDK

ViewModel-модули импортируются до Domain/Data модулей:

```kotlin
importAll(
    umbrellaModule,                      // PlatformConfiguration
    platformModule,
    commonDomainModule,

    // ViewModel модули (загружаются ПЕРВЫМИ — работает за счет ленивости Kodein)
    newFeatureViewModelModule,

    // Domain модули
    newFeatureDomainModule,

    // Data модули
    commonDataModule,
    newFeatureDataModule,
)
```

### 8.3. Правила DI

- `bindSingleton` — для тяжелых объектов (Dispatchers, Firebase, HttpClient, `Settings`, `PlatformConfiguration`).
- `bindProvider` — для объектов с состоянием (ViewModel, Repository).
- `instance()` — получение зависимости из графа внутри модуля.
- `Inject.instance()` — только в статическом контексте (вне DI-модуля, например в `PlatformSDK`), НЕ в compose.
- Запрещены циклические зависимости.

---

## 9. Compose UI — правила

- **Пакет: все `@Composable`-функции объявляются ТОЛЬКО в `compose/`-пакете** — `feature_<name>/compose/` (включая sub-features) или `common/compose/`. Запрещено в `presentation/`, `domain/`, `data/` (см. §0.10).
- `collectAsStateWithLifecycle()` для `viewStates()` и `viewActions()`.
- `LaunchedEffect(action)` для обработки одноразовых действий + `obtainEvent(ClearAction)`.
- State Hoisting: компоненты получают `State` и лямбды, а не ViewModel.
- Ресурсы: `org.jetbrains.compose.resources.stringResource()` / `painterResource()`, не Android `R.` и не JVM-ресурсы. После добавления ключа в `strings.xml` (в оба локаля) достаточно добавить импорт сгенерированного ресурса: `import alcodboptimizer.composeapp.generated.resources.Res`, `import alcodboptimizer.composeapp.generated.resources.<key_name>` и `import org.jetbrains.compose.resources.stringResource`. Если после добавления ресурса не компилируется — причина почти всегда в отсутствующем импорте.
- **Форматированные строки:** в composable тексты с плейсхолдерами резолвятся только через `stringResource(Res.string.<key>, args)` для `%N$s` / `%N$d`; `.replace(...)` внутри composable не используется. Другие формы (`%1$.0f`, `%1.2f`) CMP не поддерживает и выводит как есть; числа — через `%N$d` + `Int`. Подробно: §4.3.11.
- **Владение ресурсами и контекстом — только compose-слой (MANDATORY).** Запрещено в `domain/`, `presentation/`, `data/` и в некомпозитных файлах `commonMain`: `Res.*`, `StringResource`, `DrawableResource`, `ImageVector`, ссылки на Material Icons (`androidx.compose.material.icons.*`), а также `android.content.Context` / `ComponentActivity` / `LocalContext`. Домен хранит только типизированные ключи, а соответствие «ключ → ресурс/вектор» выражается композитными мапперами в `compose/`-пакете фичи. Платформенный `Context` доступен только платформенным `actual`-реализациям и композитному коду конкретной платформы (`androidMain/.../compose/`); `expect class PlatformConfiguration` объявляет только платформенно-независимое поведение, а Android-специфичные члены (`androidContext`, `getActivity`) видны лишь в `androidMain`.
- Общий компонент для нескольких экранов — в `common/compose/components/`; общие composable-утилиты (например, `SharedFlow.collectAsStateWithLifecycle`, clipboard) — в `common/compose/utils/`.
- UI-эффекты без ViewModel: `SnackbarHostState`, высота баннера (`onHeightChanged`) — допустимы в `remember`, т.к. они декоративные, но они не принимают бизнес-решений.
- **Диалоги управляются state/event.** Новый диалог — stateless-компонент в `compose/dialogs/`, видимость — флагом в `State`, закрытие — `Event`. Не открывать диалог из `remember`-флага.
- **Контраст и консистентность UI в обеих темах (MANDATORY).** Текст, лежащий на кастомной цветной подложке, ОБЯЗАН иметь явный `color` из токенов (`MaterialTheme.colorScheme...`). Нельзя полагаться на дефолтный `LocalContentColor` (в тёмной теме он может сделать текст невидимым). Общие визуальные компоненты используются с их дефолтными темизированными цветами; запрещены локальные перебивки `containerColor`/`contentColor`, ломающие консистентность.

---

## 10. Платформенная интеграция (expect/actual)

```
composeApp/src/
├── commonMain/kotlin/.../common/core/platform/   PlatformConfiguration.kt (expect)
├── androidMain/kotlin/.../                       PlatformConfiguration.android.kt
└── jvmMain/kotlin/.../                           PlatformConfiguration.jvm.kt
```

- `expect` объявляет минимум — только то, что реально различается между платформами.
- `actual`-файлы именуются по шаблону `ИмяФайла.платформа.kt`.
- Для каждого `expect` ровно 2 `actual` (androidMain, jvmMain).
- Если тела `actual` на Android и JVM идентичны, пара НЕ создаётся: тело переносится в `commonMain` как обычное объявление без `expect`/`actual`.
- `PlatformConfiguration` — единая точка входа платформенных зависимостей, регистрируется как `bindSingleton` в `PlatformSDK.init()`.

---

## 11. Полный пример новой фичи

### Структура

```
feature_new_feature/
├── compose/
│   ├── NewFeatureScreen.kt
│   ├── components/
│   │   ├── NewFeatureScreenContent.kt
│   │   └── NewFeatureItem.kt
│   └── dialogs/
├── presentation/
│   ├── di/NewFeatureViewModelModule.kt
│   ├── models/
│   │   ├── NewFeatureState.kt
│   │   └── NewFeatureEvent.kt
│   └── NewFeatureViewModel.kt
├── domain/
│   ├── di/NewFeatureDomainModule.kt
│   ├── repository/NewFeatureRepository.kt
│   ├── models/
│   └── utils/NewFeatureError.kt
└── data/
    ├── di/NewFeatureDataModule.kt
    ├── datasource/NewFeatureDataSource.kt (+ Impl)
    └── models/
```

### Код

```kotlin
// State
data class NewFeatureState(
    val items: List<NewFeatureData> = emptyList(),
    val isLoading: Boolean = false,
) : Any

// Event
sealed interface NewFeatureEvent {
    data object ClearAction : NewFeatureEvent
    data object OnRefresh : NewFeatureEvent
    data class OnItemClicked(val id: String) : NewFeatureEvent
}

// Action
sealed interface NewFeatureAction {
    data class ShowError(val error: Error) : NewFeatureAction
}

// Error
enum class NewFeatureError : Error { FetchError, SaveError }

// ViewModel
class NewFeatureViewModel(
    private val repository: NewFeatureRepository,
    private val dispatchers: Dispatchers,
) : BaseSharedViewModel<NewFeatureState, NewFeatureAction, NewFeatureEvent>(
    initialState = NewFeatureState()
) {
    init { loadItems() }

    override fun obtainEvent(viewEvent: NewFeatureEvent) {
        when (viewEvent) {
            NewFeatureEvent.ClearAction -> viewAction = null
            NewFeatureEvent.OnRefresh -> loadItems()
            is NewFeatureEvent.OnItemClicked -> handleItemClick(viewEvent.id)
        }
    }

    private fun loadItems() {
        viewState = viewState.copy(isLoading = true)
        withViewModelScope {
            repository.getItems()
                .onSuccess { viewState = viewState.copy(items = it, isLoading = false) }
                .onError {
                    viewState = viewState.copy(isLoading = false)
                    viewAction = NewFeatureAction.ShowError(it)
                }
        }
    }

    private fun handleItemClick(id: String) { /* ... */ }
}

// Экран
@Composable
fun NewFeatureScreen(modifier: Modifier = Modifier) {
    val viewModel: NewFeatureViewModel by rememberViewModel()
    val state by viewModel.viewStates().collectAsStateWithLifecycle()
    val action by viewModel.viewActions().collectAsStateWithLifecycle()

    LaunchedEffect(action) {
        action?.let {
            when (it) { is NewFeatureAction.ShowError -> { /* показать ошибку */ } }
            viewModel.obtainEvent(NewFeatureEvent.ClearAction)
        }
    }

    NewFeatureScreenContent(state = state, onEvent = viewModel::obtainEvent)
}
```

---

## 12. Чек-лист новой фичи

- [ ] Пакет `feature_<name>` с подпакетами `compose`, `presentation`, `domain`, `data`.
- [ ] `sealed interface <Name>Event` (события UI, имена в прошедшем времени).
- [ ] `data class <Name>State : Any` — ВСЁ состояние экрана и диалогов.
- [ ] `sealed interface <Name>Action` (одноразовые действия, опционально).
- [ ] `enum <Name>Error : Error`.
- [ ] `<Name>Repository` — композитный интерфейс из узких контрактов в `domain/repository/`.
- [ ] `<Name>DataSource` (+ impl) в data; при разрастании — композитный интерфейс из узких контрактов, все методы возвращают `Result` (кроме чистых геттеров).
- [ ] `class <Name>RepositoryImpl` в data — фасад с делегированием ролям (`internal`-классы в `data/repository/`).
- [ ] `class <Name>ViewModel : BaseSharedViewModel<...>` — только диспетчеризация событий и оркестрация.
- [ ] Под-флоу с собственной машиной состояний вынесены в `presentation/coordinators/` (stateless, возвращают `State`/`Result`).
- [ ] DI-модули data/domain/presentation + регистрация в `PlatformSDK` (ViewModel-модули первыми).
- [ ] `@Composable` экран с подписками state/action + `LaunchedEffect(action)` + `ClearAction`.
- [ ] `components/<Name>ScreenContent.kt` + отдельные компоненты; диалоги — через state/event.
- [ ] Нет `remember` для бизнес-состояния; нет `Inject.instance()` в compose.
- [ ] Все IO-операции в `withContext(dispatchers.io)`.
- [ ] Все методы возвращают `Result<T, Error>`.
- [ ] Мапперы DTO <-> Model.
- [ ] Видимость: `private` для одного файла, `internal` в пределах фичи, `public` только для кросс-фичевого API.
- [ ] Все `@Composable`-функции — в `compose/`-пакете (фичи или `common/compose/`); ни одной в `presentation/domain/data`.
- [ ] Нет захардкоженных пользовательских строк: все тексты — в `values/strings.xml` и `values-ru/strings.xml`.
- [ ] Нет полных имён пакетов в выражениях — только `import` + короткое имя.
- [ ] Цвета, шейпы и повторяющиеся размеры — из `Theme.kt`/`Color.kt`.

---

## 13. Проверки перед PR

- [ ] `./gradlew build` — полная сборка всех модулей и таргетов.
- [ ] `./gradlew :androidApp:assembleDebug` — Android-приложение собирается.
- [ ] `./gradlew :composeApp:compileKotlinJvm` — JVM-таргет компилируется без предупреждений.
- [ ] `./gradlew detekt` — линт всех модулей. В `composeApp` агрегатная задача `detekt` зависит от source-set задач, поэтому проверяется и `commonMain`; запускать именно её, а не задачу одного source set'а.
- [ ] `./gradlew tasks --all` — модель задач Gradle создаётся без ошибок (проверка синхронизации IDE).
- [ ] `openspec validate --all` — если менялись спеки или change.
- [ ] Изменения не коммитятся и не пушутся без явного запроса пользователя.

### Пороги сложности и baseline (detekt)

`config/detekt/detekt.yml` содержит секцию `complexity` с двумя правилами (detekt 2.x):

- `LargeClass`: `allowedLines: 400`;
- `TooManyFunctions`: `allowedFunctionsPerClass: 20`, `allowedFunctionsPerFile: 30`, `allowedFunctionsPerInterface: 15`, `allowedFunctionsPerObject: 20`, `ignoreOverridden: true`, `ignoreDeprecated: true`.
- Тестовые source set'ы (`commonTest`, `jvmTest`, `androidHostTest`) исключены из обоих правил.

`config/detekt/detekt-baseline.xml` сейчас пуст и подключён ко всем задачам `Detekt` и `DetektCreateBaselineTask` в `composeApp/build.gradle.kts`. Пока в нём нет записей — любой новый класс/интерфейс свыше порогов нужно дробить, а не добавлять в baseline.

**Baseline только сокращается.** Если запись всё же появилась как осознанный долг с обоснованием, change, который режет класс из baseline, обязан удалить его запись в том же PR. Добавление записи ради зелёной сборки запрещено.

`LongMethod` и `CyclomaticComplexMethod` намеренно не включены: они дают десятки срабатываний на `when`-диспетчерах и composable'ах и требуют отдельной калибровки по реальному отчёту.

---

## 14. Стиль кода — сводка

| Элемент | Соглашение |
|---------|-----------|
| **State** | `data class` с default-значениями, implements `Any` |
| **Event** | `sealed interface`, имена в прошедшем времени (`Clicked`, `Loaded`, `Selected`) |
| **Action** | `sealed interface`, имена-существительные (`ShowError`, `NavigateTo`) |
| **ViewModel** | extends `BaseSharedViewModel<State, Action, Event>` |
| **Repository** | композитный интерфейс в domain + узкие контракты; impl-фасад в data |
| **DataSource** | интерфейс + impl в data; методы возвращают `Result` |
| **Роль data-слоя** | `internal`-класс в `data/repository/`, одна ответственность, общее состояние — в store |
| **UseCase** | класс с `operator fun invoke()` |
| **Координатор** | stateless `internal`-класс в `presentation/coordinators/`; `State` → новый `State`, async → `Result` |
| **Error** | enum implements `Error` interface |
| **DI модуль** | `val moduleName = DI.Module("ModuleName") { }` |
| **Мапперы** | extension-функции: `.toModel()`, `.toDto()` |
| **Пакеты** | snake_case: `feature_my_calendar`, `common_domain_utils` |
| **Layout-папки** | Во feature: `compose/`, `domain/`, `data/` (не `ui/`, не `business/`) |
| **Видимость** | `private` (файл) → `internal` (фича) → `public` (кросс-фичевый API) |
| **Composable** | только в `compose/`-пакетах, stateless, `state` + `onEvent` |
| **Строки** | только Compose Resources, оба локаля, `%N$s`/`%N$d` |

# Module_6

Задания 1–7 модуля 6 доступны из каталога приложения. Карточки 4 и 5 открывают команды для запуска соответствующего JVM-сервера на компьютере. Остальные карточки открывают Android-экраны.

## Запуск Android

Распакуйте ZIP. В Android Studio выберите File → Open и откройте папку `Module_6` с `settings.gradle.kts`. Для Gradle JDK используйте JBR Android Studio или JDK 17+. Установите Android SDK Platform 36, дождитесь Sync, выберите конфигурацию `app` и устройство с Android 10 (API 29) или новее. Нажмите Run. В меню приложения выберите нужное задание.

Сборка из PowerShell в корне проекта

```powershell
.\gradlew.bat :app:assembleDebug
```

Стек проекта включает Kotlin 2.0.21, Gradle Wrapper 8.13, AGP 8.13.2 и Jetpack Compose. Исходники приложений и тестов написаны на Kotlin. Для запуска JVM-тестов на Windows при сбое ClassNotFoundException из каталога с кириллицей используйте путь с латинскими буквами, например `C:\AndroidTasks\Module_6`.

## Задания

| Номер | Запуск |
|---|---|
| 1 | Фотокаталог Picsum |
| 2 | Публичный Nobel |
| 3 | Вход через DummyJSON |
| 4 | `:serverLocal:run`, порт 8081 |
| 5 | `:serverNeon:run`, порт 8080 |
| 6 | Собственный Nobel API |
| 7 | Пульс по Bluetooth |

## Локальный Nobel API

```powershell
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
.\gradlew.bat :serverLocal:run
```

Команда генерации секрета рассчитана на PowerShell 7. В Android Studio задачу можно запустить через Gradle → serverLocal → application → run с заданным `JWT_SECRET`. Из ресурса `prizes.json` загружаются пять премий 1903 года и семь лауреатов. Учебный вход `learner` / `practice`. Пароль можно задать через `DEMO_PASSWORD`. Стандартный порт 8081. Если он занят, задайте свободный, например `$env:PORT = '18112'`, и используйте его в HTTP-запросах.

Запросите `POST /auth/login` с JSON `username` и `password`. Для защищённых запросов передайте JWT в `Authorization: Bearer ...`. Доступны `GET /prizes`, `GET /prizes/1903/phy` и `GET /prizes/1903/phy/laureates` на `http://localhost:8081`.

## Сервер с Neon и Android-клиент

Подготовьте учебную ветку/базу Neon. В окне Connect выберите ветку, базу и роль, отключите Connection pooling для прямого подключения, скопируйте PostgreSQL connection string. На Windows запустите PowerShell 7 из корня проекта.

```powershell
.\neon.ps1 -Action Configure
.\neon.ps1 -Action Run
```

Configure принимает строку в закрытом запросе и сохраняет её в `%LOCALAPPDATA%\Module6-Neon\connection.dpapi`, зашифрованной для текущей учётной записи Windows. Run формирует JDBC_URL с TLS, передаёт параметры дочернему Gradle и генерирует отдельный JWT_SECRET. Напрямую `:serverNeon:run` принимает `JDBC_URL` вида `jdbc:postgresql://HOST/DB?sslmode=require`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` длиной от 32 символов и необязательный `PORT`. [Документация Neon](https://neon.com/docs/connect/connect-from-any-app).

При первом старте создаются таблицы и импортируется учебный набор премий. Учебные учётные записи API `learner` / `practice` и `second` / `practice2`. Для пустой базы можно задать собственные `DEMO_PASSWORD` и `SECOND_DEMO_PASSWORD` до первого запуска. Они отдельны от роли и пароля PostgreSQL.

После старта откройте `app` → «Собственный Nobel API». На AVD оставьте `http://10.0.2.2:8080`, войдите как `learner`, отфильтруйте лауреатов по 1903 году или категории и откройте карточку. На физическом устройстве укажите доступный HTTPS-адрес сервера. Подробный контракт доступен по `http://localhost:8080/openapi.json` и через браузер по `http://localhost:8080/docs`. Для отрисовки страницы документации её клиентский скрипт загружается из сети.

Избранное проверяется через `GET /users/me/prizes`, `POST /users/me/prizes/{prizeId}` и `DELETE /users/me/prizes/{prizeId}`. Например, идентификатор премии по физике `1903-phy`. Сервер проверяет JWT и определяет пользователя из токена. Сохранены `/auth/login` и защищённые маршруты премий из задания 4. Список и детали задания 6 читаются только с собственного сервера. Экраны публичного Nobel и DummyJSON работают отдельно.

Для BLE включите Bluetooth, выдайте доступ к ближайшим устройствам и используйте датчик с профилем Heart Rate Service (0x180D). Измерения поступают через уведомления характеристики 0x2A37.

```powershell
.\gradlew.bat :app:testDebugUnitTest :serverLocal:test :serverNeon:test
```

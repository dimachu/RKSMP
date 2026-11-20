# Module_4

Задания 1–14 модуля 4 доступны из каталога приложения. Карточки 1 и 2 открывают команды запуска для JVM-подмодуля `:console` на компьютере. Карточки 3–14 открывают Android-экраны.

## Запуск Android

Распакуйте ZIP. В Android Studio выберите File → Open и откройте папку `Module_4` с `settings.gradle.kts`. Для Gradle JDK используйте JBR Android Studio или JDK 17+. Установите Android SDK Platform 36, дождитесь Sync, выберите конфигурацию `app` и устройство с Android 10 (API 29) или новее. Нажмите Run. В меню приложения выберите нужное задание.

Сборка из PowerShell в корне проекта

```powershell
.\gradlew.bat :app:assembleDebug
```

Стек проекта включает Kotlin 2.0.21, Gradle Wrapper 8.13, AGP 8.13.2 и Jetpack Compose. Исходники приложений и тестов написаны на Kotlin. Для запуска JVM-тестов на Windows при сбое ClassNotFoundException из каталога с кириллицей используйте путь с латинскими буквами, например `C:\AndroidTasks\Module_4`.

## Задания

| Номер | Запуск |
|---|---|
| 1 | `:console:sources` |
| 2 | `:console:duplicates` |
| 3–4 | Поиск репозиториев, Социальная лента |
| 5–7 | Foreground Service, Background Service, Bound Service |
| 8–9 | Обработка фотографии, Отчёт о погоде |
| 10–11 | Местоположение, Напоминание в 20:00 |
| 12–14 | Факты о животных, Курс валюты, Компас |

```powershell
.\gradlew.bat :console:sources -Psuccess
.\gradlew.bat :console:sources -Pfail
.\gradlew.bat :console:duplicates -Pdirectory=C:\AndroidTasks\samples -Pseconds=10
.\gradlew.bat :console:test :app:testDebugUnitTest
```

Для поиска дубликатов положите два JSON-файла с одинаковыми байтами и один отличающийся в указанный каталог. Параметр `-Pseconds` принимает целое количество секунд. В режиме `-Pfail` продажи возвращают ошибку, а погода и пользователи по-прежнему попадают в отчёт.

Запускайте Background Service из открытого приложения. Длительная работа после ухода в фон ограничивается Android. Разрешение уведомлений запрашивается при запуске счётчика, таймера, прогноза или напоминания. Для задания 11 также разрешите точные сигналы в настройках. Для задания 10 используйте эмулятор с Google APIs или устройство с Google Play Services. Направление компаса проверяется на устройстве с датчиками или через виртуальные датчики AVD.

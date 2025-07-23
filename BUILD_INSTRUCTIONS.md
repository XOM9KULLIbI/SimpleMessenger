# Инструкции по сборке SimpleMessenger

## Требования

### Для разработки
- **Android Studio** Arctic Fox (2020.3.1) или новее
- **JDK 11** или выше
- **Android SDK** с API Level 21-36
- **Gradle 8.13** (автоматически устанавливается)

### Для запуска
- **Android устройство** с API Level 21+ (Android 5.0+)
- Или **Android эмулятор** с теми же требованиями

## Настройка проекта

### 1. Клонирование и открытие
```bash
# Открыть проект в Android Studio
# File -> Open -> выбрать папку проекта
```

### 2. Настройка Android SDK
В Android Studio:
1. File → Project Structure → SDK Location
2. Указать путь к Android SDK
3. Убедиться, что установлены:
   - Android SDK Platform API 21-36
   - Android SDK Build-Tools 34.0.0+
   - Android SDK Platform-Tools

### 3. Синхронизация Gradle
```bash
# В терминале Android Studio или командной строке
./gradlew clean build
```

## Сборка APK

### Debug версия
```bash
./gradlew assembleDebug
```
APK будет в: `app/build/outputs/apk/debug/app-debug.apk`

### Release версия
```bash
./gradlew assembleRelease
```
APK будет в: `app/build/outputs/apk/release/app-release.apk`

## Запуск сервера

### Python сервер
```bash
# Установить зависимости
pip install flask

# Запустить сервер
python "server (5).py"
```

Сервер будет доступен на `http://localhost:5000`

### Важно для тестирования
- При тестировании на реальном устройстве, замените `localhost` в коде приложения на IP-адрес компьютера
- Убедитесь, что устройство и компьютер находятся в одной сети

## Конфигурация сервера в приложении

В файле `MainActivity.kt` найдите и измените URL сервера:
```kotlin
private val serverUrl = "http://YOUR_COMPUTER_IP:5000"
```

## Возможные проблемы

### 1. SDK location not found
Создайте файл `local.properties` в корне проекта:
```properties
sdk.dir=/path/to/your/Android/Sdk
```

### 2. Ошибки сети на эмуляторе
Используйте `10.0.2.2` вместо `localhost` для доступа к серверу с эмулятора:
```kotlin
private val serverUrl = "http://10.0.2.2:5000"
```

### 3. Проблемы с разрешениями
Убедитесь, что предоставлены разрешения:
- Доступ к интернету (автоматически)
- Доступ к хранилищу (для загрузки файлов)
- Доступ к медиафайлам (для отправки фото/видео)

## Структура проекта

```
app/
├── src/main/
│   ├── java/com/example/simplemessenger/
│   │   ├── MainActivity.kt          # Главная активность
│   │   ├── ChatActivity.kt          # Активность чата
│   │   ├── FullscreenMediaActivity.kt # Полноэкранный просмотр медиа
│   │   ├── MediaDownloadHelper.kt   # Помощник загрузки медиа
│   │   └── ui/
│   │       ├── MessageAdapter.kt    # Адаптер сообщений
│   │       ├── Message.kt           # Модель сообщения
│   │       └── ChatListAdapter.kt   # Адаптер списка чатов
│   ├── res/
│   │   ├── layout/                  # XML layouts
│   │   ├── drawable/                # Drawable ресурсы
│   │   ├── values/                  # Значения (цвета, размеры и т.д.)
│   │   └── xml/                     # XML конфигурации
│   └── AndroidManifest.xml          # Манифест приложения
└── build.gradle.kts                 # Конфигурация сборки
```

## Тестирование

### 1. Unit тесты
```bash
./gradlew test
```

### 2. Инструментальные тесты
```bash
./gradlew connectedAndroidTest
```

### 3. Ручное тестирование
1. Установите APK на устройство
2. Запустите сервер
3. Создайте пользователей и протестируйте функции:
   - Отправка текстовых сообщений
   - Отправка изображений
   - Отправка видео
   - Загрузка медиафайлов
   - Полноэкранный просмотр

## Дополнительные команды

### Очистка проекта
```bash
./gradlew clean
```

### Проверка зависимостей
```bash
./gradlew dependencies
```

### Генерация отчёта о размере APK
```bash
./gradlew assembleRelease
# Результат в build/reports/
```
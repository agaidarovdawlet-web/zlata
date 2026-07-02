# Краткая инструкция

## Назначение

Приложение имитирует мобильный сервис для сотрудников «ЭнергосбыТ Плюс»: поиск клиентов и лицевых счетов, просмотр счетчиков, внесение показаний, контроль поверок и оплат.

## Главная БД

```text
app/src/main/assets/energosbyt_plus.db
```

Таблицы:

- `customers`
- `addresses`
- `personal_accounts`
- `meters`
- `meter_readings`
- `payments`

## Проверить БД

```bash
sqlite3 app/src/main/assets/energosbyt_plus.db ".tables"
sqlite3 app/src/main/assets/energosbyt_plus.db "PRAGMA foreign_key_check;"
```

## Собрать

```bash
./gradlew :app:assembleDebug
```

## Установить и запустить

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.example.zlata -c android.intent.category.LAUNCHER 1
```

## Что показать в отчете

- локальная SQLite-БД;
- копирование БД из `assets`;
- связи через внешние ключи;
- поиск по ФИО, адресу, счету и счетчику;
- внесение показаний без интернета;
- контроль ближайших поверок;
- Material 3 интерфейс в стиле энергосбытовой компании.

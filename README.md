# Аукционная площадка

Консольная программа для управления аукционами. Программа хранит данные в PostgreSQL и позволяет вести лоты, продавцов и покупателей, принимать ставки и отслеживать баланс.

## Возможности

- Управление продавцами, покупателями и аукционными лотами.
- Поиск, фильтрация и сортировка лотов.
- Создание ставок и пополнение баланса покупателей.
- Статистика по аукционам и экспорт лотов в CSV или Excel.
- Работа со временем по московскому часовому поясу (UTC+3).

## Технологии

Java 17 · Maven · PostgreSQL

## Запуск

### 1. Подготовка окружения

Должны быть установлены: JDK 17, Maven и PostgreSQL.

### 2. Создание и заполнение БД

Требуется выполнить следующие команды из корня проекта:

```powershell
psql -U postgres -d postgres -c "CREATE DATABASE auction_db;"
psql -U postgres -d auction_db -f sql/schema.sql
psql -U postgres -d auction_db -f sql/seed.sql
```

Скрипт `seed.sql` добавляет демонстрационные категории, участников, лоты и ставки.

### 3. Настройка подключения

Необходимо открыть `src/main/resources/db.properties` и указать имя пользователя и пароль PostgreSQL:

```properties
db.url=jdbc:postgresql://localhost:5432/auction_db
db.user=postgres
db.password=ваш_пароль
```

### 4. Сборка & Запуск

```powershell
mvn clean package
java -jar target/auction-platform-1.0.0.jar
```

## Структура проекта

- `src/main/java` — приложение, модели, репозитории и бизнес-логика.
- `src/main/resources/db.properties` — параметры подключения к БД.
- `sql/schema.sql` — схема базы данных.
- `sql/seed.sql` — демонстрационные данные.
- `export/` — файлы экспорта. 

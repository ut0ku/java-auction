package ru.mirea.project.util;

import ru.mirea.project.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseManager {
    private static final Properties PROPS = new Properties();
    private static String url;
    private static String user;
    private static String password;

    static {
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new DatabaseException("Файл db.properties не найден.", null);
            }
            PROPS.load(in);
            url = PROPS.getProperty("db.url");
            user = PROPS.getProperty("db.user");
            password = PROPS.getProperty("db.password");
        } catch (IOException ex) {
            throw new DatabaseException("Ошибка чтения db.properties.", ex);
        }
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подключения к базе данных: " + ex.getMessage(), ex);
        }
    }

    public static void executeSqlFile(Path path) {
        try {
            String sql = Files.readString(path, StandardCharsets.UTF_8);
            String[] statements = sql.split(";");
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                for (String part : statements) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        statement.execute(trimmed);
                    }
                }
            }
        } catch (IOException | SQLException ex) {
            throw new DatabaseException("Ошибка выполнения SQL-скрипта: " + path.getFileName(), ex);
        }
    }

    public static void printTables() {
        String sql = """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name
                """;
        try (Connection connection = getConnection();
             var statement = connection.createStatement();
             var rs = statement.executeQuery(sql)) {
            System.out.println("\n--- Таблицы базы данных ---");
            while (rs.next()) {
                String table = rs.getString("table_name");
                System.out.println("Таблица: " + table);
                printTableRows(connection, table);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка вывода таблиц.", ex);
        }
    }

    private static void printTableRows(Connection connection, String table) throws SQLException {
        String query = "SELECT * FROM " + table + " LIMIT 20";
        try (var statement = connection.createStatement(); var rs = statement.executeQuery(query)) {
            int columns = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder row = new StringBuilder("  ");
                for (int i = 1; i <= columns; i++) {
                    row.append(rs.getMetaData().getColumnName(i)).append("=")
                            .append(rs.getString(i)).append("; ");
                }
                System.out.println(row);
            }
        }
    }
}

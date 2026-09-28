package ru.mirea.project.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class InputReader {
    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return "";
        }
        return scanner.nextLine().trim();
    }

    public String readLineWithDefault(String prompt, String defaultValue) {
        String value = readLine(prompt);
        return value.isBlank() ? defaultValue : value;
    }

    public int readIntWithDefault(String prompt, int defaultValue) {
        String value = readLine(prompt);
        if (value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            System.out.println("Ошибка: ID должен быть целым числом. Использовано значение по умолчанию.");
            return defaultValue;
        }
    }

    public int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isEmpty() && !scanner.hasNextLine()) {
                return -1;
            }
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                System.out.println("Ошибка: ID должен быть целым числом.");
            }
        }
    }

    public BigDecimal readBigDecimal(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return new BigDecimal(value.replace(',', '.'));
            } catch (NumberFormatException ex) {
                System.out.println("Ошибка: введите корректное число.");
            }
        }
    }

    public BigDecimal readBigDecimalWithDefault(String prompt, BigDecimal defaultValue) {
        String value = readLine(prompt);
        if (value.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.replace(',', '.'));
        } catch (NumberFormatException ex) {
            System.out.println("Ошибка: введите корректное число. Использовано значение по умолчанию.");
            return defaultValue;
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (yyyy-MM-dd HH:mm, МСК UTC+3): ");
            try {
                return LocalDateTime.parse(value.replace(' ', 'T'));
            } catch (DateTimeParseException ex) {
                try {
                    return LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                } catch (DateTimeParseException ex2) {
                    System.out.println("Ошибка: используйте формат yyyy-MM-dd HH:mm (время по МСК, UTC+3)");
                }
            }
        }
    }

    public void pause() {
        if (scanner.hasNextLine()) {
            readLine("\nНажмите Enter для продолжения...");
        }
    }
}

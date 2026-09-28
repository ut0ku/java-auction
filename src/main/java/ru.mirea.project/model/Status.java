package ru.mirea.project.model;

public enum Status {
    DRAFT,
    ACTIVE,
    SOLD,
    CANCELLED;

    public static Status fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Статус не может быть пустым.");
        }
        try {
            return Status.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Некорректный статус: " + value);
        }
    }
}

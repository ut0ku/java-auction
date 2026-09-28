package ru.mirea.project.util;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.MainEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public final class CsvExporter {
    private CsvExporter() {
    }

    public static void exportLots(List<MainEntity> lots, Path filePath) {
        if (Files.isDirectory(filePath)) {
            filePath = filePath.resolve("lots.csv");
        }
        if (filePath.getFileName() == null || filePath.getFileName().toString().lastIndexOf('.') == -1) {
            filePath = filePath.resolve("lots.csv");
        }
        List<String> lines = lots.stream()
                .map(lot -> String.join(";",
                        String.valueOf(lot.getId()),
                        escape(lot.getTitle()),
                        escape(lot.getCategoryName()),
                        escape(lot.getSellerName()),
                        lot.getStatus().name(),
                        lot.getStartingPrice().toPlainString(),
                        lot.getCurrentPrice().toPlainString(),
                        lot.getCreatedAt().toString(),
                        lot.getEndsAt().toString()))
                .collect(Collectors.toList());

        lines.add(0, "id;title;category;seller;status;starting_price;current_price;created_at;ends_at");
        try {
            Files.createDirectories(filePath.getParent() == null ? Path.of(".") : filePath.getParent());
            Files.write(filePath, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new DatabaseException("Ошибка экспорта в CSV.", ex);
        }
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\n", " ");
    }
}

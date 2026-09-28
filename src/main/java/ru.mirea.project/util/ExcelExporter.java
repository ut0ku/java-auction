package ru.mirea.project.util;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.MainEntity;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ExcelExporter {
    private ExcelExporter() {
    }

    public static void exportLots(List<MainEntity> lots, Path filePath) {
        if (Files.isDirectory(filePath)) {
            filePath = filePath.resolve("lots.xlsx");
        }
        if (filePath.getFileName() == null || filePath.getFileName().toString().lastIndexOf('.') == -1) {
            filePath = filePath.resolve("lots.xlsx");
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Аукционные лоты");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Название", "Категория", "Продавец", "Статус",
                    "Стартовая цена", "Текущая цена", "Создан", "Окончание"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowNum = 1;
            for (MainEntity lot : lots) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(lot.getId());
                row.createCell(1).setCellValue(lot.getTitle());
                row.createCell(2).setCellValue(lot.getCategoryName());
                row.createCell(3).setCellValue(lot.getSellerName());
                row.createCell(4).setCellValue(lot.getStatus().name());
                row.createCell(5).setCellValue(lot.getStartingPrice().doubleValue());
                row.createCell(6).setCellValue(lot.getCurrentPrice().doubleValue());
                row.createCell(7).setCellValue(lot.getCreatedAt().toString());
                row.createCell(8).setCellValue(lot.getEndsAt().toString());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            Files.createDirectories(filePath.getParent() == null ? Path.of(".") : filePath.getParent());
            try (OutputStream out = Files.newOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (IOException ex) {
            throw new DatabaseException("Ошибка экспорта в Excel.", ex);
        }
    }
}

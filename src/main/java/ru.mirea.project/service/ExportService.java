package ru.mirea.project.service;

import ru.mirea.project.model.MainEntity;
import ru.mirea.project.util.CsvExporter;
import ru.mirea.project.util.ExcelExporter;

import java.nio.file.Path;
import java.util.List;

public class ExportService {
    private final MainEntityService mainEntityService = new MainEntityService();

    public void exportToExcel(Path path) {
        List<MainEntity> lots = mainEntityService.findAll();
        ExcelExporter.exportLots(lots, path);
    }

    public void exportToCsv(Path path) {
        List<MainEntity> lots = mainEntityService.findAll();
        CsvExporter.exportLots(lots, path);
    }
}

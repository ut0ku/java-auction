package ru.mirea.project;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.*;
import ru.mirea.project.repository.CategoryRepository;
import ru.mirea.project.service.*;
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.util.InputReader;
import ru.mirea.project.util.MoneyFormatter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ConsoleApp {
    private final InputReader input;
    private final SellerService sellerService = new SellerService();
    private final UserService userService = new UserService();
    private final MainEntityService mainEntityService = new MainEntityService();
    private final BidService bidService = new BidService();
    private final BalanceService balanceService = new BalanceService();
    private final StatisticsService statisticsService = new StatisticsService();
    private final ExportService exportService = new ExportService();
    private final CategoryRepository categoryRepository = new CategoryRepository();

    public ConsoleApp(Scanner scanner) {
        this.input = new InputReader(scanner);
        this.scanner = scanner;
    }

    private final Scanner scanner;

    public void run() {
        mainEntityService.synchronizeExpiredStatuses();
        while (true) {
            printMainMenu();
            String choice = input.readLine("Выберите действие: ");
            if (choice.isEmpty() && !hasInput()) {
                System.out.println("Выход из системы.");
                return;
            }
            try {
                switch (choice) {
                    case "1" -> sellersMenu();
                    case "2" -> buyersMenu();
                    case "3" -> lotsMenu();
                    case "4" -> searchMenu();
                    case "5" -> filterMenu();
                    case "6" -> sortMenu();
                    case "7" -> showStatistics();
                    case "8" -> exportMenu();
                    case "9" -> DatabaseManager.printTables();
                    case "10" -> bidsMenu();
                    case "11" -> balanceMenu();
                    case "0" -> {
                        System.out.println("Выход из системы.");
                        return;
                    }
                    default -> System.out.println("Неверный пункт меню.");
                }
            } catch (EntityNotFoundException | BusinessException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            } catch (DatabaseException ex) {
                System.out.println("Ошибка базы данных: " + ex.getMessage());
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n======================================== АУКЦИОННАЯ ПЛОЩАДКА ========================================\n(все даты и время в системе — по МСК, UTC+3)\n1. Продавцы\n2. Покупатели\n3. Аукционные лоты\n4. Поиск\n5. Фильтрация\n6. Сортировка\n7. Статистика\n8. Экспорт данных\n9. Вывести таблицы базы данных\n10. Ставки\n11. Баланс\n0. Выход");
    }

    private void sellersMenu() {
        while (true) {
            System.out.println("\n--- Продавцы ---");
            System.out.println("1. Создать  2. Список  3. По ID  4. Изменить  5. Удалить  6. Поиск по имени  0. Назад");
            switch (input.readLine("Выберите действие: ")) {
                case "1" -> {
                    Seller seller = sellerService.create(
                            input.readLine("ФИО: "),
                            input.readLine("Email: "),
                            input.readLine("Телефон: "));
                    System.out.println("Создан: " + seller);
                }
                case "2" -> printList(sellerService.findAll());
                case "3" -> System.out.println(sellerService.getById(input.readInt("ID: ")));
                case "4" -> {
                    int id = input.readInt("ID: ");
                    Seller existing = sellerService.getById(id);
                    sellerService.update(id,
                            input.readLineWithDefault("ФИО [" + existing.getFullName() + "]: ", existing.getFullName()),
                            input.readLineWithDefault("Email [" + existing.getEmail() + "]: ", existing.getEmail()),
                            input.readLineWithDefault("Телефон [" + existing.getPhone() + "]: ", existing.getPhone()));
                    System.out.println("Продавец обновлён.");
                }
                case "5" -> {
                    sellerService.delete(input.readInt("ID: "));
                    System.out.println("Продавец удалён.");
                }
                case "6" -> printList(sellerService.searchByName(input.readLine("Часть имени: ")));
                case "0" -> { return; }
                default -> System.out.println("Неверный пункт меню.");
            }
        }
    }

    private void buyersMenu() {
        while (true) {
            System.out.println("\n--- Покупатели ---");
            System.out.println("1. Создать  2. Список  3. По ID  4. Изменить  5. Удалить  6. Поиск по имени");
            System.out.println("7. Баланс покупателя  8. Пополнить баланс  0. Назад");
            switch (input.readLine("Выберите действие: ")) {
                case "1" -> {
                    User user = userService.create(
                            input.readLine("ФИО: "),
                            input.readLine("Email: "),
                            input.readLine("Телефон: "));
                    System.out.println("Создан: " + user);
                }
                case "2" -> printList(userService.findAll());
                case "3" -> System.out.println(userService.getById(input.readInt("ID: ")));
                case "4" -> {
                    int id = input.readInt("ID: ");
                    User existing = userService.getById(id);
                    userService.update(id,
                            input.readLineWithDefault("ФИО [" + existing.getFullName() + "]: ", existing.getFullName()),
                            input.readLineWithDefault("Email [" + existing.getEmail() + "]: ", existing.getEmail()),
                            input.readLineWithDefault("Телефон [" + existing.getPhone() + "]: ", existing.getPhone()));
                    System.out.println("Покупатель обновлён.");
                }
                case "5" -> {
                    userService.delete(input.readInt("ID: "));
                    System.out.println("Покупатель удалён.");
                }
                case "6" -> printList(userService.searchByName(input.readLine("Часть имени: ")));
                case "7" -> {
                    User user = balanceService.getUserWithBalance(input.readInt("ID покупателя: "));
                    System.out.printf("Покупатель: %s%nБаланс: %s руб.%n",
                            user.getFullName(), MoneyFormatter.format(user.getBalance()));
                }
                case "8" -> {
                    int userId = input.readInt("ID покупателя: ");
                    User user = balanceService.getUserWithBalance(userId);
                    System.out.println("Текущий баланс: " + MoneyFormatter.format(user.getBalance()) + " руб.");
                    BalanceTopup topup = balanceService.topUp(userId, input.readBigDecimal("Сумма пополнения: "));
                    System.out.println("Пополнение выполнено: " + topup);
                }
                case "0" -> { return; }
                default -> System.out.println("Неверный пункт меню.");
            }
        }
    }

    private void lotsMenu() {
        while (true) {
            System.out.println("\n--- Аукционные лоты ---");
            System.out.println("1. Создать  2. Весь список  3. Список по категориям  4. По ID  5. Изменить  6. Удалить  0. Назад");
            switch (input.readLine("Выберите действие: ")) {
                case "1" -> createLot();
                case "2" -> printList(mainEntityService.findAll());
                case "3" -> {
                    printCategories();
                    printList(mainEntityService.filterByCategory(input.readInt("ID категории: ")));
                }
                case "4" -> System.out.println(mainEntityService.getById(input.readInt("ID: ")));
                case "5" -> updateLot();
                case "6" -> {
                    mainEntityService.delete(input.readInt("ID: "));
                    System.out.println("Лот удалён.");
                }
                case "0" -> { return; }
                default -> System.out.println("Неверный пункт меню.");
            }
        }
    }

    private void createLot() {
        printCategories();
        int categoryId = input.readInt("ID категории: ");
        int sellerId = input.readInt("ID продавца: ");
        String criminalRecord = readCriminalRecord(categoryId);
        MainEntity lot = mainEntityService.create(
                sellerId,
                categoryId,
                input.readLine("Название: "),
                input.readLine("Описание: "),
                input.readBigDecimal("Стартовая цена: "),
                readStatus(),
                input.readDateTime("Дата окончания"),
                criminalRecord);
            System.out.println("Создан: " + mainEntityService.getById(lot.getId()));
    }

    private String readCriminalRecord(int categoryId) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null && "Недвижимость".equals(category.getName())) {
            String hasRecord = input.readLine("Есть ли судимость? (да/нет): ");
            if ("да".equalsIgnoreCase(hasRecord)) {
                return input.readLine("Какая судимость: ");
            }
        }
        return null;
    }

    private void updateLot() {
        int id = input.readInt("ID: ");
        MainEntity existing = mainEntityService.getById(id);
        int sellerId = input.readIntWithDefault("ID продавца [" + existing.getSellerId() + "]: ", existing.getSellerId());
        int categoryId = input.readIntWithDefault("ID категории [" + existing.getCategoryId() + "]: ", existing.getCategoryId());
        String title = input.readLineWithDefault("Название [" + existing.getTitle() + "]: ", existing.getTitle());
        String description = input.readLineWithDefault("Описание [" + existing.getDescription() + "]: ", existing.getDescription());
        java.math.BigDecimal startingPrice = input.readBigDecimalWithDefault("Стартовая цена [" + existing.getStartingPrice() + "]: ", existing.getStartingPrice());
        java.math.BigDecimal currentPrice = input.readBigDecimalWithDefault("Текущая цена [" + existing.getCurrentPrice() + "]: ", existing.getCurrentPrice());
        Status status = readStatus();
        java.time.LocalDateTime endsAt = input.readDateTime("Дата окончания [" + existing.getEndsAt() + "]");
        String criminalRecord = updateCriminalRecord(categoryId, existing.getCriminalRecord());
        mainEntityService.update(
                id, sellerId, categoryId, title, description, startingPrice, currentPrice, status, endsAt, criminalRecord);
        System.out.println("Лот обновлён.");
    }

    private String updateCriminalRecord(int categoryId, String currentRecord) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category != null && "Недвижимость".equals(category.getName())) {
            String defaultVal = currentRecord != null ? "да" : "нет";
            String hasRecord = input.readLineWithDefault("Есть ли судимость? (да/нет) [" + defaultVal + "]: ", defaultVal);
            if ("да".equalsIgnoreCase(hasRecord)) {
                return input.readLineWithDefault("Какая судимость: ", currentRecord != null ? currentRecord : "");
            }
            return null;
        }
        return currentRecord;
    }

    private void searchMenu() {
        System.out.println("\n--- Поиск ---");
        System.out.println("1. По названию лота  2. По описанию лота  3. По продавцу (ID)  0. Назад");
        switch (input.readLine("Выберите действие: ")) {
            case "1" -> printList(mainEntityService.searchByTitle(input.readLine("Название: ")));
            case "2" -> printList(mainEntityService.searchByDescription(input.readLine("Описание: ")));
            case "3" -> printList(mainEntityService.findBySeller(input.readInt("ID продавца: ")));
            case "0" -> { return; }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private void filterMenu() {
        System.out.println("\n--- Фильтрация ---");
        System.out.println("1. По статусу  2. По категории  3. По диапазону цен  4. По дате окончания  0. Назад");
        switch (input.readLine("Выберите действие: ")) {
            case "1" -> printList(mainEntityService.filterByStatus(readStatus()));
            case "2" -> {
                printCategories();
                printList(mainEntityService.filterByCategory(input.readInt("ID категории: ")));
            }
            case "3" -> printList(mainEntityService.filterByPriceRange(
                    input.readBigDecimal("Мин. цена: "),
                    input.readBigDecimal("Макс. цена: ")));
            case "4" -> printList(mainEntityService.filterByEndDateRange(
                    input.readDateTime("Дата от"),
                    input.readDateTime("Дата до")));
            case "0" -> { return; }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private void sortMenu() {
        System.out.println("\n--- Сортировка ---");
        System.out.println("1. По дате создания (возр.)  2. По дате создания (убыв.)");
        System.out.println("3. По текущей цене (возр.)   4. По текущей цене (убыв.)  0. Назад");
        switch (input.readLine("Выберите действие: ")) {
            case "1" -> printList(mainEntityService.sortByCreatedAt(true));
            case "2" -> printList(mainEntityService.sortByCreatedAt(false));
            case "3" -> printList(mainEntityService.sortByCurrentPrice(true));
            case "4" -> printList(mainEntityService.sortByCurrentPrice(false));
            case "0" -> { return; }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private void bidsMenu() {
        while (true) {
            System.out.println("\n--- Ставки ---");
            System.out.println("1. Сделать ставку  2. Все ставки  3. По лоту  4. По покупателю  0. Назад");
            switch (input.readLine("Выберите действие: ")) {
                case "1" -> {
                    int lotId = input.readInt("ID лота: ");
                    int userId = input.readInt("ID покупателя: ");
                    MainEntity lot = mainEntityService.getById(lotId);
                    System.out.println("Лот: " + lot.getTitle());
                    System.out.println("Текущая цена: " + MoneyFormatter.format(lot.getCurrentPrice()) + " руб.");
                    User user = balanceService.getUserWithBalance(userId);
                    System.out.println("Баланс покупателя: " + MoneyFormatter.format(user.getBalance()) + " руб.");
                    Bid bid = bidService.placeBid(lotId, userId, input.readBigDecimal("Сумма ставки: "));
                    System.out.println("Ставка принята: " + bid);
                }
                case "2" -> printList(bidService.findAll());
                case "3" -> printList(bidService.findByLotId(input.readInt("ID лота: ")));
                case "4" -> printList(bidService.findByUserId(input.readInt("ID покупателя: ")));
                case "0" -> { return; }
                default -> System.out.println("Неверный пункт меню.");
            }
        }
    }

    private void balanceMenu() {
        while (true) {
            System.out.println("\n--- Баланс ---");
            System.out.println("1. Пополнить баланс  2. Баланс покупателя  3. История пополнений  4. Все пополнения  0. Назад");
            switch (input.readLine("Выберите действие: ")) {
                case "1" -> {
                    int userId = input.readInt("ID покупателя: ");
                    User user = balanceService.getUserWithBalance(userId);
                    System.out.println("Текущий баланс: " + MoneyFormatter.format(user.getBalance()) + " руб.");
                    BalanceTopup topup = balanceService.topUp(userId, input.readBigDecimal("Сумма пополнения: "));
                    System.out.println("Пополнение выполнено: " + topup);
                }
                case "2" -> {
                    User user = balanceService.getUserWithBalance(input.readInt("ID покупателя: "));
                    System.out.printf("Покупатель: %s%nБаланс: %s руб.%n",
                            user.getFullName(), MoneyFormatter.format(user.getBalance()));
                }
                case "3" -> printList(balanceService.findTopupsByUserId(input.readInt("ID покупателя: ")));
                case "4" -> printList(balanceService.findAllTopups());
                case "0" -> { return; }
                default -> System.out.println("Неверный пункт меню.");
            }
        }
    }

    private void showStatistics() {
        System.out.println("\n--- Статистика системы ---");
        Map<String, String> stats = statisticsService.collectStatistics();
        stats.forEach((key, value) -> System.out.println(key + ": " + value));
    }

    private void exportMenu() {
        System.out.println("\n--- Экспорт ---");
        System.out.println("1. Excel (.xlsx)  2. CSV (.csv)  0. Назад");
        switch (input.readLine("Выберите действие: ")) {
            case "1" -> {
                String path = input.readLine("Путь к файлу [export/lots.xlsx]: ");
                if (path.isBlank()) {
                    path = "export/lots.xlsx";
                }
                exportService.exportToExcel(Path.of(path));
                System.out.println("Экспортировано в " + path);
            }
            case "2" -> {
                String path = input.readLine("Путь к файлу [export/lots.csv]: ");
                if (path.isBlank()) {
                    path = "export/lots.csv";
                }
                exportService.exportToCsv(Path.of(path));
                System.out.println("Экспортировано в " + path);
            }
            case "0" -> { return; }
            default -> System.out.println("Неверный пункт меню.");
        }
    }

    private Status readStatus() {
        while (true) {
            String value = input.readLine("Статус (DRAFT/ACTIVE/SOLD/CANCELLED): ");
            try {
                return Status.fromString(value);
            } catch (IllegalArgumentException ex) {
                System.out.println("Ошибка: " + ex.getMessage());
            }
        }
    }

    private void printCategories() {
        System.out.println("Категории:");
        categoryRepository.findAll().forEach(System.out::println);
    }

    private void printList(List<?> items) {
        if (items.isEmpty()) {
            System.out.println("Записи не найдены.");
            return;
        }
        items.forEach(System.out::println);
        System.out.println("Всего: " + items.size());
    }

    private boolean hasInput() {
        return scanner.hasNextLine();
    }
}

package ru.mirea.project.service;

import ru.mirea.project.model.Status;
import ru.mirea.project.repository.MainEntityRepository;
import ru.mirea.project.repository.BidRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.CategoryRepository;
import ru.mirea.project.repository.SellerRepository;
import ru.mirea.project.util.MoneyFormatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatisticsService {
    private final SellerRepository sellerRepository = new SellerRepository();
    private final UserRepository userRepository = new UserRepository();
    private final MainEntityRepository mainEntityRepository = new MainEntityRepository();
    private final MainEntityService mainEntityService = new MainEntityService();
    private final BidRepository bidRepository = new BidRepository();
    private final BalanceService balanceService = new BalanceService();
    private final CategoryRepository categoryRepository = new CategoryRepository();

    public Map<String, String> collectStatistics() {
        mainEntityService.synchronizeExpiredStatuses();
        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Всего продавцов", String.valueOf(sellerRepository.countAll()));
        stats.put("Всего покупателей", String.valueOf(userRepository.countAll()));
        stats.put("Всего лотов", String.valueOf(mainEntityRepository.countAll()));
        stats.put("Активных лотов", String.valueOf(mainEntityRepository.countByStatus(Status.ACTIVE)));
        stats.put("Проданных лотов", String.valueOf(mainEntityRepository.countByStatus(Status.SOLD)));
        stats.put("Отменённых лотов", String.valueOf(mainEntityRepository.countByStatus(Status.CANCELLED)));
        stats.put("Черновиков", String.valueOf(mainEntityRepository.countByStatus(Status.DRAFT)));
        stats.put("Всего ставок", String.valueOf(bidRepository.countAll()));
        stats.put("Всего пополнений", String.valueOf(balanceService.countTopups()));
        stats.put("Сумма балансов покупателей", MoneyFormatter.format(balanceService.totalBalances()) + " руб.");
        BigDecimal avg = mainEntityRepository.averageCurrentPrice().setScale(2, RoundingMode.HALF_UP);
        stats.put("Средняя текущая цена", MoneyFormatter.format(avg) + " руб.");

        categoryRepository.findAll().forEach(category -> stats.put(
                        "Лотов в категории «" + category.getName() + "»",
                        String.valueOf(mainEntityRepository.countByCategoryId(category.getId()))
                ));

        return stats;
    }
}

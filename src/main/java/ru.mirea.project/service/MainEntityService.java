package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.MainEntity;
import ru.mirea.project.model.Status;
import ru.mirea.project.repository.MainEntityRepository;
import ru.mirea.project.repository.CategoryRepository;
import ru.mirea.project.repository.SellerRepository;

import ru.mirea.project.util.MoscowTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainEntityService {
    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            Status.DRAFT, EnumSet.of(Status.ACTIVE, Status.CANCELLED),
            Status.ACTIVE, EnumSet.of(Status.DRAFT, Status.SOLD, Status.CANCELLED),
            Status.SOLD, EnumSet.noneOf(Status.class),
            Status.CANCELLED, EnumSet.noneOf(Status.class)
    );

    private final MainEntityRepository mainEntityRepository = new MainEntityRepository();
    private final SellerRepository sellerRepository = new SellerRepository();
    private final CategoryRepository categoryRepository = new CategoryRepository();

    public MainEntity create(int sellerId, int categoryId, String title, String description,
                              BigDecimal startingPrice, Status status, LocalDateTime endsAt, String criminalRecord) {
        MainEntity lot = buildLot(null, sellerId, categoryId, title, description, startingPrice, status, endsAt, criminalRecord);
        validateLot(lot, null);
        lot.setCurrentPrice(startingPrice);
        return mainEntityRepository.save(lot);
    }

    public List<MainEntity> findAll() {
        synchronizeExpiredStatuses();
        return mainEntityRepository.findAll();
    }

    public MainEntity getById(int id) {
        synchronizeExpiredStatuses();
        return mainEntityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Лот с ID " + id + " не найден."));
    }

    public void update(int id, int sellerId, int categoryId, String title, String description,
                       BigDecimal startingPrice, BigDecimal currentPrice, Status status, LocalDateTime endsAt, String criminalRecord) {
        MainEntity existing = getById(id);
        validateStatusTransition(existing.getStatus(), status);
        MainEntity lot = buildLot(id, sellerId, categoryId, title, description, startingPrice, status, endsAt, criminalRecord);
        lot.setCurrentPrice(currentPrice);
        lot.setCreatedAt(existing.getCreatedAt());
        validateLot(lot, id);
        mainEntityRepository.update(lot);
    }

    public void delete(int id) {
        MainEntity lot = getById(id);
        if (lot.getStatus() == Status.ACTIVE) {
            throw new BusinessException("Нельзя удалить активный лот. Сначала отмените или завершите торги.");
        }
        mainEntityRepository.deleteById(id);
    }

    public List<MainEntity> searchByTitle(String part) {
        synchronizeExpiredStatuses();
        return search(part, mainEntityRepository::findByTitlePart, "названию");
    }

    public List<MainEntity> searchByDescription(String part) {
        synchronizeExpiredStatuses();
        return search(part, mainEntityRepository::findByDescriptionPart, "описанию");
    }

    public List<MainEntity> filterByStatus(Status status) {
        synchronizeExpiredStatuses();
        return mainEntityRepository.filterByStatus(status);
    }

    public List<MainEntity> filterByCategory(int categoryId) {
        synchronizeExpiredStatuses();
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Категория с ID " + categoryId + " не найдена."));
        return mainEntityRepository.filterByCategoryId(categoryId);
    }

    public List<MainEntity> findBySeller(int sellerId) {
        synchronizeExpiredStatuses();
        sellerRepository.findById(sellerId)
                .orElseThrow(() -> new EntityNotFoundException("Продавец с ID " + sellerId + " не найден."));
        return mainEntityRepository.findBySellerId(sellerId);
    }

    public List<MainEntity> filterByPriceRange(BigDecimal min, BigDecimal max) {
        synchronizeExpiredStatuses();
        if (min.compareTo(max) > 0) {
            throw new BusinessException("Минимальная цена не может превышать максимальную.");
        }
        return mainEntityRepository.filterByPriceRange(min, max);
    }

    public List<MainEntity> filterByEndDateRange(LocalDateTime from, LocalDateTime to) {
        synchronizeExpiredStatuses();
        if (from.isAfter(to)) {
            throw new BusinessException("Дата начала диапазона не может быть позже даты окончания.");
        }
        return mainEntityRepository.filterByEndDateRange(from, to);
    }

    public List<MainEntity> sortByCreatedAt(boolean ascending) {
        synchronizeExpiredStatuses();
        return mainEntityRepository.sortByCreatedAt(ascending);
    }

    public List<MainEntity> sortByCurrentPrice(boolean ascending) {
        synchronizeExpiredStatuses();
        return mainEntityRepository.sortByCurrentPrice(ascending);
    }

    public void synchronizeExpiredStatuses() {
        mainEntityRepository.synchronizeExpiredStatuses();
    }

    private MainEntity buildLot(Integer id, int sellerId, int categoryId, String title, String description,
                                 BigDecimal startingPrice, Status status, LocalDateTime endsAt, String criminalRecord) {
        MainEntity lot = new MainEntity();
        lot.setId(id);
        lot.setSellerId(sellerId);
        lot.setCategoryId(categoryId);
        lot.setTitle(title == null ? "" : title.trim());
        lot.setDescription(description);
        lot.setStartingPrice(startingPrice);
        lot.setStatus(status);
        lot.setEndsAt(endsAt);
        lot.setCriminalRecord(criminalRecord);
        return lot;
    }

    private void validateLot(MainEntity lot, Integer excludeId) {
        if (lot.getTitle().isBlank()) {
            throw new BusinessException("Название лота обязательно.");
        }
        sellerRepository.findById(lot.getSellerId())
                .orElseThrow(() -> new BusinessException("Указан несуществующий продавец."));
        categoryRepository.findById(lot.getCategoryId())
                .orElseThrow(() -> new BusinessException("Указана несуществующая категория."));
        if (lot.getStartingPrice() == null || lot.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Стартовая цена должна быть больше нуля.");
        }
        if (lot.getCurrentPrice() != null && lot.getCurrentPrice().compareTo(lot.getStartingPrice()) < 0) {
            throw new BusinessException("Текущая цена не может быть ниже стартовой.");
        }
        if (lot.getEndsAt() == null) {
            throw new BusinessException("Дата окончания аукциона обязательна.");
        }
        if (lot.getStatus() == Status.ACTIVE && !lot.getEndsAt().isAfter(MoscowTime.now())) {
            throw new BusinessException("Активный лот должен завершаться в будущем (время по МСК, UTC+3).");
        }
        if (excludeId == null && lot.getStatus() == Status.SOLD) {
            throw new BusinessException("Новый лот нельзя сразу создать со статусом SOLD.");
        }
    }

    private void validateStatusTransition(Status from, Status to) {
        if (from == to) {
            return;
        }
        Set<Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new BusinessException("Запрещённый переход статуса: " + from + " -> " + to);
        }
    }

    private List<MainEntity> search(String part, java.util.function.Function<String, List<MainEntity>> fn, String field) {
        if (part == null || part.isBlank()) {
            throw new BusinessException("Строка поиска по " + field + " не может быть пустой.");
        }
        return fn.apply(part.trim());
    }
}

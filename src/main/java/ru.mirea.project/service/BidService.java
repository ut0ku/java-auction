package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.MainEntity;
import ru.mirea.project.model.Bid;
import ru.mirea.project.model.Status;
import ru.mirea.project.repository.BidRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.util.MoneyFormatter;
import ru.mirea.project.util.MoscowTime;

import java.math.BigDecimal;
import java.util.List;

/**
 * Бизнес-правила для ставок:
 * 1. Нельзя сделать ставку без указания суммы.
 * 2. Нельзя указать несуществующего покупателя или лот.
 * 3. Ставка допускается только на лот со статусом ACTIVE.
 * 4. Сумма ставки должна быть выше текущей цены лота.
 * 5. Нельзя сделать ставку после окончания торгов (время сравнивается по МСК, UTC+3).
 * 6. На балансе покупателя должно быть достаточно средств для ставки.
 */
public class BidService {
    private final BidRepository bidRepository = new BidRepository();
    private final UserRepository userRepository = new UserRepository();
    private final MainEntityService mainEntityService = new MainEntityService();

    public Bid placeBid(int lotId, int userId, BigDecimal amount) {
        mainEntityService.synchronizeExpiredStatuses();
        MainEntity lot = mainEntityService.getById(lotId);
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Указан несуществующий покупатель."));

        validateBid(lot, amount);
        validateBalance(lotId, userId, amount);

        try {
            return bidRepository.placeBid(lotId, userId, amount, MoscowTime.now());
        } catch (DatabaseException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("Недостаточно средств")) {
                throw new BusinessException(ex.getMessage());
            }
            throw ex;
        }
    }

    public List<Bid> findAll() {
        return bidRepository.findAll();
    }

    public List<Bid> findByLotId(int lotId) {
        mainEntityService.getById(lotId);
        return bidRepository.findByLotId(lotId);
    }

    public List<Bid> findByUserId(int userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Покупатель с ID " + userId + " не найден."));
        return bidRepository.findByUserId(userId);
    }

    public long countAll() {
        return bidRepository.countAll();
    }

    private void validateBid(MainEntity lot, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Сумма ставки должна быть больше нуля.");
        }
        if (lot.getStatus() != Status.ACTIVE) {
            throw new BusinessException("Ставки принимаются только на активные лоты (статус ACTIVE).");
        }
        if (!lot.getEndsAt().isAfter(MoscowTime.now())) {
            throw new BusinessException("Торги по лоту уже завершены (время проверяется по МСК, UTC+3).");
        }
        if (amount.compareTo(lot.getCurrentPrice()) <= 0) {
                throw new BusinessException("Ставка должна быть выше текущей цены: "
                    + MoneyFormatter.format(lot.getCurrentPrice()) + " руб.");
        }
    }

    private void validateBalance(int lotId, int userId, BigDecimal amount) {
        BigDecimal required = bidRepository.computeRequiredFunds(lotId, userId, amount);
        if (required.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal balance = userRepository.getBalance(userId);
        if (balance.compareTo(required) < 0) {
            throw new BusinessException(String.format(
                    "Недостаточно средств на балансе. Требуется: %s руб., доступно: %s руб.",
                    MoneyFormatter.format(required), MoneyFormatter.format(balance)));
        }
    }
}

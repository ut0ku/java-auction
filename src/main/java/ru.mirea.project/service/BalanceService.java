package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.BalanceTopup;
import ru.mirea.project.model.User;
import ru.mirea.project.repository.BalanceTopupRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.util.MoscowTime;

import java.math.BigDecimal;
import java.util.List;

/**
 * Бизнес-правила для баланса покупателя:
 * 1. Пополнение возможно только для существующего покупателя.
 * 2. Сумма пополнения должна быть больше нуля.
 * 3. Баланс не может стать отрицательным.
 * 4. Для ставки на лот требуется достаточный баланс (проверка в {@link BidService}).
 */
public class BalanceService {
    private final BalanceTopupRepository topupRepository = new BalanceTopupRepository();
    private final UserRepository userRepository = new UserRepository();

    public BalanceTopup topUp(int userId, BigDecimal amount) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Покупатель с ID " + userId + " не найден."));
        validateTopUpAmount(amount);
        return topupRepository.topUp(userId, amount, MoscowTime.now());
    }

    public BigDecimal getBalance(int userId) {
        return userRepository.getBalance(userId);
    }

    public List<BalanceTopup> findAllTopups() {
        return topupRepository.findAll();
    }

    public List<BalanceTopup> findTopupsByUserId(int userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Покупатель с ID " + userId + " не найден."));
        return topupRepository.findByUserId(userId);
    }

    public User getUserWithBalance(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Покупатель с ID " + userId + " не найден."));
    }

    public long countTopups() {
        return topupRepository.countAll();
    }

    public BigDecimal totalBalances() {
        return userRepository.sumAllBalances();
    }

    private void validateTopUpAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Сумма пополнения должна быть больше нуля.");
        }
    }
}

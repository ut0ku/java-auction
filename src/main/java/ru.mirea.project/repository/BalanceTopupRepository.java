package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.BalanceTopup;
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.util.MoscowTime;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BalanceTopupRepository {
    private static final String BASE_SELECT = """
            SELECT t.id, t.buyer_id, b.full_name AS buyer_name, t.amount, t.topup_time, t.balance_after
            FROM balance_topups t
            JOIN buyers b ON b.id = t.buyer_id
            """;

    public BalanceTopup topUp(int userId, BigDecimal amount, LocalDateTime topupTime) {
        String updateSql = """
                UPDATE buyers SET balance = balance + ?
                WHERE id = ?
                RETURNING balance
                """;
        String insertSql = """
                INSERT INTO balance_topups (buyer_id, amount, topup_time, balance_after)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal balanceAfter;
                try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                    update.setBigDecimal(1, amount);
                    update.setInt(2, userId);
                    try (ResultSet rs = update.executeQuery()) {
                        if (!rs.next()) {
                            throw new DatabaseException("Покупатель не найден при пополнении баланса.", null);
                        }
                        balanceAfter = rs.getBigDecimal("balance");
                    }
                }

                int topupId;
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setInt(1, userId);
                    insert.setBigDecimal(2, amount);
                    insert.setTimestamp(3, Timestamp.valueOf(topupTime));
                    insert.setBigDecimal(4, balanceAfter);
                    try (ResultSet rs = insert.executeQuery()) {
                        rs.next();
                        topupId = rs.getInt("id");
                    }
                }

                conn.commit();
                return findById(topupId).orElseThrow(() ->
                        new DatabaseException("Пополнение сохранено, но не найдено при чтении.", null));
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка пополнения баланса.", ex);
        }
    }

    public Optional<BalanceTopup> findById(int id) {
        return query(BASE_SELECT + " WHERE t.id = ?", ps -> ps.setInt(1, id)).stream().findFirst();
    }

    public List<BalanceTopup> findAll() {
        return query(BASE_SELECT + " ORDER BY t.topup_time DESC", null);
    }

    public List<BalanceTopup> findByUserId(int userId) {
        return query(BASE_SELECT + " WHERE t.buyer_id = ? ORDER BY t.topup_time DESC",
                ps -> ps.setInt(1, userId));
    }

    public long countAll() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM balance_topups");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта пополнений.", ex);
        }
    }

    public BigDecimal sumAllTopups() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM balance_topups");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта суммы пополнений.", ex);
        }
    }

    static void adjustBalance(Connection conn, int userId, BigDecimal delta) throws SQLException {
        String sql = """
                UPDATE buyers SET balance = balance + ?
                WHERE id = ? AND balance + ? >= 0
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, userId);
            ps.setBigDecimal(3, delta);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new SQLException("Недостаточно средств или покупатель не найден.");
            }
        }
    }

    static BigDecimal getBalance(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM buyers WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Покупатель не найден.");
                }
                return rs.getBigDecimal("balance");
            }
        }
    }

    @FunctionalInterface
    private interface ParameterSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<BalanceTopup> query(String sql, ParameterSetter setter) {
        List<BalanceTopup> topups = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.set(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    topups.add(mapRow(rs));
                }
            }
            return topups;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка чтения истории пополнений.", ex);
        }
    }

    private BalanceTopup mapRow(ResultSet rs) throws SQLException {
        BalanceTopup topup = new BalanceTopup();
        topup.setId(rs.getInt("id"));
        topup.setUserId(rs.getInt("buyer_id"));
        topup.setUserName(rs.getString("buyer_name"));
        topup.setAmount(rs.getBigDecimal("amount"));
        Timestamp ts = rs.getTimestamp("topup_time");
        topup.setTopupTime(ts == null ? MoscowTime.now() : ts.toLocalDateTime());
        topup.setBalanceAfter(rs.getBigDecimal("balance_after"));
        return topup;
    }
}

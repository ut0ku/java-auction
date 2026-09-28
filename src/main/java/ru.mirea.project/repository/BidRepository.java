package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.Bid;
import ru.mirea.project.util.DatabaseManager;
import ru.mirea.project.util.MoscowTime;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BidRepository {
    private static final String BASE_SELECT = """
            SELECT b.id, b.lot_id, l.title AS lot_title, b.buyer_id, bu.full_name AS buyer_name,
                   b.amount, b.bid_time
            FROM bids b
            JOIN auction_lots l ON l.id = b.lot_id
            JOIN buyers bu ON bu.id = b.buyer_id
            """;

    public Bid placeBid(int lotId, int userId, BigDecimal amount, LocalDateTime bidTime) {
        String insertSql = """
                INSERT INTO bids (lot_id, buyer_id, amount, bid_time)
                VALUES (?, ?, ?, ?)
                RETURNING id
                """;
        String updateLotSql = "UPDATE auction_lots SET current_price = ? WHERE id = ?";
        String highestBidSql = """
                SELECT buyer_id, amount FROM bids
                WHERE lot_id = ?
                ORDER BY amount DESC, bid_time DESC
                LIMIT 1
                """;

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer previousUserId = null;
                BigDecimal previousAmount = null;
                try (PreparedStatement highest = conn.prepareStatement(highestBidSql)) {
                    highest.setInt(1, lotId);
                    try (ResultSet rs = highest.executeQuery()) {
                        if (rs.next()) {
                            previousUserId = rs.getInt("buyer_id");
                            previousAmount = rs.getBigDecimal("amount");
                        }
                    }
                }

                BigDecimal deductAmount;
                if (previousUserId != null && previousUserId == userId) {
                    deductAmount = amount.subtract(previousAmount);
                } else {
                    deductAmount = amount;
                }

                if (deductAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BalanceTopupRepository.adjustBalance(conn, userId, deductAmount.negate());
                }

                if (previousUserId != null && !previousUserId.equals(userId)) {
                    BalanceTopupRepository.adjustBalance(conn, previousUserId, previousAmount);
                }

                int bidId;
                try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                    insert.setInt(1, lotId);
                    insert.setInt(2, userId);
                    insert.setBigDecimal(3, amount);
                    insert.setTimestamp(4, Timestamp.valueOf(bidTime));
                    try (ResultSet rs = insert.executeQuery()) {
                        rs.next();
                        bidId = rs.getInt("id");
                    }
                }

                try (PreparedStatement update = conn.prepareStatement(updateLotSql)) {
                    update.setBigDecimal(1, amount);
                    update.setInt(2, lotId);
                    update.executeUpdate();
                }

                conn.commit();
                return findById(bidId).orElseThrow(() ->
                        new DatabaseException("Ставка сохранена, но не найдена при чтении.", null));
            } catch (SQLException ex) {
                conn.rollback();
                if ("Недостаточно средств или покупатель не найден.".equals(ex.getMessage())) {
                    throw new DatabaseException("Недостаточно средств на балансе для ставки.", ex);
                }
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (DatabaseException ex) {
            throw ex;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка сохранения ставки.", ex);
        }
    }

    public Optional<Bid> findHighestBidOnLot(int lotId) {
        return query(BASE_SELECT + " WHERE b.lot_id = ? ORDER BY b.amount DESC, b.bid_time DESC LIMIT 1",
                ps -> ps.setInt(1, lotId)).stream().findFirst();
    }

    public BigDecimal computeRequiredFunds(int lotId, int userId, BigDecimal amount) {
        return findHighestBidOnLot(lotId)
                .filter(bid -> bid.getUserId() == userId)
                .map(bid -> amount.subtract(bid.getAmount()))
                .orElse(amount);
    }

    public java.util.Optional<Bid> findById(int id) {
        return query(BASE_SELECT + " WHERE b.id = ?", ps -> ps.setInt(1, id)).stream().findFirst();
    }

    public List<Bid> findAll() {
        return query(BASE_SELECT + " ORDER BY b.bid_time DESC", null);
    }

    public List<Bid> findByLotId(int lotId) {
        return query(BASE_SELECT + " WHERE b.lot_id = ? ORDER BY b.amount DESC, b.bid_time DESC",
                ps -> ps.setInt(1, lotId));
    }

    public List<Bid> findByUserId(int userId) {
        return query(BASE_SELECT + " WHERE b.buyer_id = ? ORDER BY b.bid_time DESC",
                ps -> ps.setInt(1, userId));
    }

    public long countAll() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM bids");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта ставок.", ex);
        }
    }

    @FunctionalInterface
    private interface ParameterSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Bid> query(String sql, ParameterSetter setter) {
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.set(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bids.add(mapRow(rs));
                }
            }
            return bids;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка чтения ставок.", ex);
        }
    }

    private Bid mapRow(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getInt("id"));
        bid.setLotId(rs.getInt("lot_id"));
        bid.setLotTitle(rs.getString("lot_title"));
        bid.setUserId(rs.getInt("buyer_id"));
        bid.setUserName(rs.getString("buyer_name"));
        bid.setAmount(rs.getBigDecimal("amount"));
        Timestamp ts = rs.getTimestamp("bid_time");
        bid.setBidTime(ts == null ? MoscowTime.now() : ts.toLocalDateTime());
        return bid;
    }
}

package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.MainEntity;
import ru.mirea.project.model.Status;
import ru.mirea.project.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MainEntityRepository implements CrudRepository<MainEntity, Integer> {
    private static final String BASE_SELECT = """
            SELECT l.id, l.seller_id, s.full_name AS seller_name, l.category_id, c.name AS category_name,
                   l.title, l.description, l.starting_price, l.current_price, l.status,
                   l.criminal_record, l.created_at, l.ends_at
            FROM auction_lots l
            JOIN sellers s ON s.id = l.seller_id
            JOIN categories c ON c.id = l.category_id
            """;

    @Override
    public MainEntity save(MainEntity lot) {
        String sql = """
                INSERT INTO auction_lots (seller_id, category_id, title, description,
                                          starting_price, current_price, status, criminal_record, ends_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id, created_at
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            fillLotParams(ps, lot, true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lot.setId(rs.getInt("id"));
                    lot.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
            return lot;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка сохранения лота.", ex);
        }
    }

    public void synchronizeExpiredStatuses() {
        String sql = """
                UPDATE auction_lots l
                SET status = CASE
                    WHEN EXISTS (SELECT 1 FROM bids b WHERE b.lot_id = l.id) THEN 'SOLD'
                    ELSE 'CANCELLED'
                END
                WHERE l.status = 'ACTIVE'
                  AND l.ends_at <= (CURRENT_TIMESTAMP AT TIME ZONE 'Europe/Moscow')
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка обновления статусов завершённых лотов.", ex);
        }
    }

    @Override
    public Optional<MainEntity> findById(Integer id) {
        String sql = BASE_SELECT + " WHERE l.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка поиска лота.", ex);
        }
    }

    @Override
    public List<MainEntity> findAll() {
        return query(BASE_SELECT + " ORDER BY l.id");
    }

    public List<MainEntity> findByTitlePart(String part) {
        return query(BASE_SELECT + " WHERE LOWER(l.title) LIKE ? ORDER BY l.title",
                ps -> ps.setString(1, "%" + part.toLowerCase() + "%"));
    }

    public List<MainEntity> findByDescriptionPart(String part) {
        return query(BASE_SELECT + " WHERE LOWER(l.description) LIKE ? ORDER BY l.title",
                ps -> ps.setString(1, "%" + part.toLowerCase() + "%"));
    }

    public List<MainEntity> findBySellerId(Integer sellerId) {
        return query(BASE_SELECT + " WHERE l.seller_id = ? ORDER BY l.created_at DESC",
                ps -> ps.setInt(1, sellerId));
    }

    public List<MainEntity> filterByStatus(Status status) {
        return query(BASE_SELECT + " WHERE l.status = ? ORDER BY l.created_at DESC",
                ps -> ps.setString(1, status.name()));
    }

    public List<MainEntity> filterByCategoryId(Integer categoryId) {
        return query(BASE_SELECT + " WHERE l.category_id = ? ORDER BY l.current_price DESC",
                ps -> ps.setInt(1, categoryId));
    }

    public List<MainEntity> filterByPriceRange(BigDecimal min, BigDecimal max) {
        return query(BASE_SELECT + " WHERE l.current_price BETWEEN ? AND ? ORDER BY l.current_price",
                ps -> {
                    ps.setBigDecimal(1, min);
                    ps.setBigDecimal(2, max);
                });
    }

    public List<MainEntity> filterByEndDateRange(LocalDateTime from, LocalDateTime to) {
        return query(BASE_SELECT + " WHERE l.ends_at BETWEEN ? AND ? ORDER BY l.ends_at",
                ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(from));
                    ps.setTimestamp(2, Timestamp.valueOf(to));
                });
    }

    public List<MainEntity> sortByCreatedAt(boolean ascending) {
        return query(BASE_SELECT + " ORDER BY l.created_at " + (ascending ? "ASC" : "DESC"));
    }

    public List<MainEntity> sortByCurrentPrice(boolean ascending) {
        return query(BASE_SELECT + " ORDER BY l.current_price " + (ascending ? "ASC" : "DESC"));
    }

    public long countAll() {
        return count("SELECT COUNT(*) FROM auction_lots");
    }

    public long countByStatus(Status status) {
        return count("SELECT COUNT(*) FROM auction_lots WHERE status = ?", ps -> ps.setString(1, status.name()));
    }

    public long countByCategoryId(Integer categoryId) {
        return count("SELECT COUNT(*) FROM auction_lots WHERE category_id = ?", ps -> ps.setInt(1, categoryId));
    }

    public BigDecimal averageCurrentPrice() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT AVG(current_price) FROM auction_lots");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            BigDecimal avg = rs.getBigDecimal(1);
            return avg == null ? BigDecimal.ZERO : avg;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка расчёта средней цены.", ex);
        }
    }

    @Override
    public void update(MainEntity lot) {
        String lockSql = "SELECT status FROM auction_lots WHERE id = ? FOR UPDATE";
        String highestBidSql = """
            SELECT buyer_id, amount FROM bids
            WHERE lot_id = ?
            ORDER BY amount DESC, bid_time DESC
            LIMIT 1
            """;
        String sql = """
                UPDATE auction_lots
                SET seller_id = ?, category_id = ?, title = ?, description = ?,
                    starting_price = ?, current_price = ?, status = ?, criminal_record = ?, ends_at = ?
                WHERE id = ?
                """;
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Status oldStatus;
                try (PreparedStatement lock = conn.prepareStatement(lockSql)) {
                    lock.setInt(1, lot.getId());
                    try (ResultSet rs = lock.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Лот не найден при обновлении.");
                        }
                        oldStatus = Status.fromString(rs.getString("status"));
                    }
                }

                if (oldStatus == Status.ACTIVE
                        && (lot.getStatus() == Status.DRAFT || lot.getStatus() == Status.CANCELLED)) {
                    try (PreparedStatement highest = conn.prepareStatement(highestBidSql)) {
                        highest.setInt(1, lot.getId());
                        try (ResultSet rs = highest.executeQuery()) {
                            if (rs.next()) {
                                BalanceTopupRepository.adjustBalance(
                                        conn, rs.getInt("buyer_id"), rs.getBigDecimal("amount"));
                            }
                        }
                    }
                    if (lot.getStatus() == Status.DRAFT) {
                        try (PreparedStatement deleteBids = conn.prepareStatement(
                                "DELETE FROM bids WHERE lot_id = ?")) {
                            deleteBids.setInt(1, lot.getId());
                            deleteBids.executeUpdate();
                        }
                        lot.setCurrentPrice(lot.getStartingPrice());
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    fillLotParams(ps, lot, false);
                    ps.setInt(10, lot.getId());
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка обновления лота.", ex);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM auction_lots WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка удаления лота.", ex);
        }
    }

    @FunctionalInterface
    private interface ParameterSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<MainEntity> query(String sql) {
        return query(sql, null);
    }

    private List<MainEntity> query(String sql, ParameterSetter setter) {
        List<MainEntity> lots = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.set(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapRow(rs));
                }
            }
            return lots;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка выполнения запроса.", ex);
        }
    }

    private long count(String sql, ParameterSetter setter) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) {
                setter.set(ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта лотов.", ex);
        }
    }

    private long count(String sql) {
        return count(sql, null);
    }

    private void fillLotParams(PreparedStatement ps, MainEntity lot, boolean isInsert) throws SQLException {
        ps.setInt(1, lot.getSellerId());
        ps.setInt(2, lot.getCategoryId());
        ps.setString(3, lot.getTitle());
        ps.setString(4, lot.getDescription());
        ps.setBigDecimal(5, lot.getStartingPrice());
        ps.setBigDecimal(6, lot.getCurrentPrice());
        ps.setString(7, lot.getStatus().name());
        ps.setString(8, lot.getCriminalRecord());
        ps.setTimestamp(9, Timestamp.valueOf(lot.getEndsAt()));
    }

    private MainEntity mapRow(ResultSet rs) throws SQLException {
        MainEntity lot = new MainEntity();
        lot.setId(rs.getInt("id"));
        lot.setSellerId(rs.getInt("seller_id"));
        lot.setSellerName(rs.getString("seller_name"));
        lot.setCategoryId(rs.getInt("category_id"));
        lot.setCategoryName(rs.getString("category_name"));
        lot.setTitle(rs.getString("title"));
        lot.setDescription(rs.getString("description"));
        lot.setStartingPrice(rs.getBigDecimal("starting_price"));
        lot.setCurrentPrice(rs.getBigDecimal("current_price"));
        lot.setStatus(Status.fromString(rs.getString("status")));
        lot.setCriminalRecord(rs.getString("criminal_record"));
        lot.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        lot.setEndsAt(rs.getTimestamp("ends_at").toLocalDateTime());
        return lot;
    }
}

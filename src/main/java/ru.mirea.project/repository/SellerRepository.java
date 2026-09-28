package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.Seller;
import ru.mirea.project.util.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SellerRepository implements CrudRepository<Seller, Integer> {
    @Override
    public Seller save(Seller seller) {
        String sql = """
                INSERT INTO sellers (full_name, email, phone)
                VALUES (?, ?, ?)
                RETURNING id, registered_at
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seller.getFullName());
            ps.setString(2, seller.getEmail());
            ps.setString(3, seller.getPhone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seller.setId(rs.getInt("id"));
                    seller.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
                }
            }
            return seller;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка сохранения продавца.", ex);
        }
    }

    @Override
    public Optional<Seller> findById(Integer id) {
        String sql = "SELECT * FROM sellers WHERE id = ?";
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
            throw new DatabaseException("Ошибка поиска продавца.", ex);
        }
    }

    @Override
    public List<Seller> findAll() {
        String sql = "SELECT * FROM sellers ORDER BY id";
        List<Seller> sellers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sellers.add(mapRow(rs));
            }
            return sellers;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка получения списка продавцов.", ex);
        }
    }

    public List<Seller> findByNamePart(String part) {
        String sql = "SELECT * FROM sellers WHERE LOWER(full_name) LIKE ? ORDER BY full_name";
        List<Seller> sellers = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + part.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sellers.add(mapRow(rs));
                }
            }
            return sellers;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка поиска продавцов.", ex);
        }
    }

    public boolean existsByEmail(String email, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT 1 FROM sellers WHERE LOWER(email) = LOWER(?)"
                : "SELECT 1 FROM sellers WHERE LOWER(email) = LOWER(?) AND id <> ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            if (excludeId != null) {
                ps.setInt(2, excludeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка проверки email продавца.", ex);
        }
    }

    public long countActiveLots(Integer sellerId) {
        String sql = "SELECT COUNT(*) FROM auction_lots WHERE seller_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта лотов продавца.", ex);
        }
    }

    @Override
    public void update(Seller seller) {
        String sql = "UPDATE sellers SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seller.getFullName());
            ps.setString(2, seller.getEmail());
            ps.setString(3, seller.getPhone());
            ps.setInt(4, seller.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка обновления продавца.", ex);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM sellers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка удаления продавца.", ex);
        }
    }

    public long countAll() {
        return count("SELECT COUNT(*) FROM sellers");
    }

    private long count(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта.", ex);
        }
    }

    private Seller mapRow(ResultSet rs) throws SQLException {
        Seller seller = new Seller();
        seller.setId(rs.getInt("id"));
        seller.setFullName(rs.getString("full_name"));
        seller.setEmail(rs.getString("email"));
        seller.setPhone(rs.getString("phone"));
        Timestamp ts = rs.getTimestamp("registered_at");
        seller.setRegisteredAt(ts == null ? null : ts.toLocalDateTime());
        return seller;
    }
}

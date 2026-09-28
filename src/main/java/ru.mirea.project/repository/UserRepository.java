package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.User;
import ru.mirea.project.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository implements CrudRepository<User, Integer> {
    @Override
    public User save(User user) {
        String sql = """
                INSERT INTO buyers (full_name, email, phone)
                VALUES (?, ?, ?)
                RETURNING id, registered_at
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getInt("id"));
                    user.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
                }
            }
            return user;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка сохранения покупателя.", ex);
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM buyers WHERE id = ?";
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
            throw new DatabaseException("Ошибка поиска покупателя.", ex);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM buyers ORDER BY id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            return users;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка получения списка покупателей.", ex);
        }
    }

    public List<User> findByNamePart(String part) {
        String sql = "SELECT * FROM buyers WHERE LOWER(full_name) LIKE ? ORDER BY full_name";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + part.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
            return users;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка поиска покупателей.", ex);
        }
    }

    public boolean existsByEmail(String email, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT 1 FROM buyers WHERE LOWER(email) = LOWER(?)"
                : "SELECT 1 FROM buyers WHERE LOWER(email) = LOWER(?) AND id <> ?";
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
            throw new DatabaseException("Ошибка проверки email покупателя.", ex);
        }
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE buyers SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setInt(4, user.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка обновления покупателя.", ex);
        }
    }

    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM buyers WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка удаления покупателя.", ex);
        }
    }

    public long countAll() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM buyers");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта покупателей.", ex);
        }
    }

    public BigDecimal getBalance(int id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT balance FROM buyers WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DatabaseException("Покупатель не найден.", null);
                }
                return rs.getBigDecimal("balance");
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка получения баланса.", ex);
        }
    }

    public BigDecimal sumAllBalances() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COALESCE(SUM(balance), 0) FROM buyers");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getBigDecimal(1);
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка подсчёта общего баланса.", ex);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setBalance(rs.getBigDecimal("balance"));
        Timestamp ts = rs.getTimestamp("registered_at");
        user.setRegisteredAt(ts == null ? null : ts.toLocalDateTime());
        return user;
    }
}

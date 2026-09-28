package ru.mirea.project.repository;

import ru.mirea.project.exception.DatabaseException;
import ru.mirea.project.model.Category;
import ru.mirea.project.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryRepository {
    public List<Category> findAll() {
        String sql = "SELECT id, name FROM categories ORDER BY id";
        List<Category> categories = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
            return categories;
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка получения категорий.", ex);
        }
    }

    public Optional<Category> findById(Integer id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Category(rs.getInt("id"), rs.getString("name")));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new DatabaseException("Ошибка поиска категории.", ex);
        }
    }
}

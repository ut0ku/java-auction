package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.User;
import ru.mirea.project.repository.UserRepository;

import java.util.List;

/**
 * Бизнес-правила для покупателей:
 * 1. ФИО обязательно.
 * 2. Email обязателен и уникален.
 */
public class UserService {
    private final UserRepository repository = new UserRepository();

    public User create(String fullName, String email, String phone) {
        validate(fullName, email, phone, null);
        return repository.save(new User(fullName.trim(), email.trim(), phone));
    }

    public List<User> findAll() {
        return repository.findAll();
    }

    public User getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Покупатель с ID " + id + " не найден."));
    }

    public void update(int id, String fullName, String email, String phone) {
        User user = getById(id);
        validate(fullName, email, phone, id);
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPhone(phone);
        repository.update(user);
    }

    public void delete(int id) {
        getById(id);
        repository.deleteById(id);
    }

    public List<User> searchByName(String part) {
        if (part == null || part.isBlank()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return repository.findByNamePart(part.trim());
    }

    private void validate(String fullName, String email, String phone, Integer excludeId) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО покупателя обязательно.");
        }
        if (!fullName.matches("[А-ЯЁа-яё]+(\\s+[А-ЯЁа-яё]+)+")) {
            throw new BusinessException("ФИО покупателя должно содержать не менее 2 слов (имя и фамилия), только кириллица.");
        }
        if (email == null || email.isBlank() || !email.matches("[\\w._-]+@[\\w.-]+\\.[\\w]{2,}")) {
            throw new BusinessException("Укажите корректный email покупателя.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^\\+?[0-9\\s\\-()]{7,15}$")) {
            throw new BusinessException("Укажите корректный номер телефона покупателя.");
        }
        if (repository.existsByEmail(email, excludeId)) {
            throw new BusinessException("Покупатель с таким email уже существует.");
        }
    }
}

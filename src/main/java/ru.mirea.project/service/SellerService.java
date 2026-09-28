package ru.mirea.project.service;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Seller;
import ru.mirea.project.repository.SellerRepository;

import java.util.List;

/**
 * Бизнес-правила для продавцов:
 * 1. ФИО обязательно.
 * 2. Email обязателен и уникален.
 * 3. Нельзя удалить продавца с активными лотами.
 */
public class SellerService {
    private final SellerRepository repository = new SellerRepository();

    public Seller create(String fullName, String email, String phone) {
        validate(fullName, email, phone, null);
        return repository.save(new Seller(fullName.trim(), email.trim(), phone));
    }

    public List<Seller> findAll() {
        return repository.findAll();
    }

    public Seller getById(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Продавец с ID " + id + " не найден."));
    }

    public void update(int id, String fullName, String email, String phone) {
        Seller seller = getById(id);
        validate(fullName, email, phone, id);
        seller.setFullName(fullName.trim());
        seller.setEmail(email.trim());
        seller.setPhone(phone);
        repository.update(seller);
    }

    public void delete(int id) {
        getById(id);
        if (repository.countActiveLots(id) > 0) {
            throw new BusinessException("Нельзя удалить продавца с активными лотами.");
        }
        repository.deleteById(id);
    }

    public List<Seller> searchByName(String part) {
        if (part == null || part.isBlank()) {
            throw new BusinessException("Строка поиска не может быть пустой.");
        }
        return repository.findByNamePart(part.trim());
    }

    private void validate(String fullName, String email, String phone, Integer excludeId) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessException("ФИО продавца обязательно.");
        }
        if (!fullName.matches("[А-ЯЁа-яё]+(\\s+[А-ЯЁа-яё]+)+")) {
            throw new BusinessException("ФИО продавца должно содержать не менее 2 слов (имя и фамилия), только кириллица.");
        }
        if (email == null || email.isBlank() || !email.matches("[\\w._-]+@[\\w.-]+\\.[\\w]{2,}")) {
            throw new BusinessException("Укажите корректный email продавца.");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^\\+?[0-9\\s\\-()]{7,15}$")) {
            throw new BusinessException("Укажите корректный номер телефона продавца.");
        }
        if (repository.existsByEmail(email, excludeId)) {
            throw new BusinessException("Продавец с таким email уже существует.");
        }
    }
}

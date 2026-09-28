-- Тестовые данные (5+ продавцов, 5+ покупателей, 10+ лотов, 3+ статуса)

INSERT INTO categories (name) VALUES
    ('Живопись'),
    ('Антиквариат'),
    ('Ювелирные изделия'),
    ('Монеты'),
    ('Книги'),
    ('Недвижимость')
ON CONFLICT (name) DO NOTHING;

INSERT INTO sellers (full_name, email, phone, registered_at) VALUES
    ('Иванов Петр Сергеевич', 'ivanov.seller@auction.ru', '+7-900-111-01-01', '2025-01-10 10:00:00'),
    ('Сидорова Анна Михайловна', 'sidorova@auction.ru', '+7-900-111-02-02', '2025-01-15 11:30:00'),
    ('Кузнецов Дмитрий', 'kuznetsov@auction.ru', '+7-900-111-03-03', '2025-02-01 09:00:00'),
    ('Морозова Елена', 'morozova@auction.ru', '+7-900-111-04-04', '2025-02-20 14:00:00'),
    ('Волков Алексей', 'volkov@auction.ru', '+7-900-111-05-05', '2025-03-05 16:45:00'),
    ('Новикова Ольга', 'novikova@auction.ru', '+7-900-111-06-06', '2025-03-12 08:20:00')
ON CONFLICT (email) DO NOTHING;

INSERT INTO buyers (full_name, email, phone, balance, registered_at) VALUES
    ('Петров Иван', 'petrov.buyer@mail.ru', '+7-901-222-01-01', 500000, '2025-01-12 12:00:00'),
    ('Смирнова Мария', 'smirnova@mail.ru', '+7-901-222-02-02', 750000, '2025-01-18 13:30:00'),
    ('Федоров Николай', 'fedorov@mail.ru', '+7-901-222-03-03', 300000, '2025-02-05 10:15:00'),
    ('Козлова Дарья', 'kozlova@mail.ru', '+7-901-222-04-04', 400000, '2025-02-22 17:00:00'),
    ('Лебедев Артём', 'lebedev@mail.ru', '+7-901-222-05-05', 1000000, '2025-03-01 11:45:00'),
    ('Орлова Виктория', 'orlova@mail.ru', '+7-901-222-06-06', 600000, '2025-03-15 09:30:00')
ON CONFLICT (email) DO NOTHING;

INSERT INTO auction_lots (seller_id, category_id, title, description, starting_price, current_price, status, created_at, ends_at)
SELECT s.id, c.id, v.title, v.description, v.starting_price, v.current_price, v.status, v.created_at::timestamp, v.ends_at::timestamp
FROM (VALUES
    ('ivanov.seller@auction.ru', 'Живопись', 'Картина «Закат над рекой»', 'Масло, холст, XX век', 50000, 50000, 'ACTIVE', '2025-04-01 10:00:00', '2026-12-31 18:00:00'),
    ('ivanov.seller@auction.ru', 'Живопись', 'Портрет неизвестного', 'Классическая живопись', 120000, 135000, 'ACTIVE', '2025-04-05 11:00:00', '2026-11-30 18:00:00'),
    ('sidorova@auction.ru', 'Антиквариат', 'Фарфоровая ваза XVIII века', 'Мейсен, реставрация', 80000, 80000, 'DRAFT', '2025-04-10 09:00:00', '2026-10-15 18:00:00'),
    ('sidorova@auction.ru', 'Антиквариат', 'Бронзовая статуэтка', 'Франция, XIX век', 45000, 52000, 'ACTIVE', '2025-04-12 14:00:00', '2026-09-20 18:00:00'),
    ('kuznetsov@auction.ru', 'Ювелирные изделия', 'Золотое кольцо с сапфиром', '585 проба', 95000, 95000, 'ACTIVE', '2025-04-15 10:30:00', '2026-08-25 18:00:00'),
    ('kuznetsov@auction.ru', 'Ювелирные изделия', 'Серебряный браслет', 'Ручная работа', 15000, 18500, 'SOLD', '2025-03-01 12:00:00', '2025-06-01 18:00:00'),
    ('morozova@auction.ru', 'Монеты', 'Золотой червонец 1923', 'Сохранность UNC', 200000, 215000, 'ACTIVE', '2025-04-20 08:00:00', '2026-07-10 18:00:00'),
    ('morozova@auction.ru', 'Монеты', 'Серебряный рубль 1898', 'Редкий выпуск', 35000, 35000, 'CANCELLED', '2025-03-10 09:00:00', '2025-05-10 18:00:00'),
    ('volkov@auction.ru', 'Книги', 'Первое издание «Мастера и Маргариты»', '1937 год', 500000, 520000, 'ACTIVE', '2025-05-01 10:00:00', '2026-12-01 18:00:00'),
    ('volkov@auction.ru', 'Книги', 'Атлас мира 1850 года', 'Коллекционное издание', 75000, 75000, 'DRAFT', '2025-05-05 15:00:00', '2026-11-01 18:00:00'),
    ('novikova@auction.ru', 'Живопись', 'Пейзаж «Осень»', 'Акварель', 25000, 28000, 'SOLD', '2025-02-01 11:00:00', '2025-04-01 18:00:00'),
    ('novikova@auction.ru', 'Антиквариат', 'Старинные часы карманные', 'Швейцария', 60000, 60000, 'ACTIVE', '2025-05-10 13:00:00', '2026-10-01 18:00:00')
) AS v(seller_email, category_name, title, description, starting_price, current_price, status, created_at, ends_at)
JOIN sellers s ON s.email = v.seller_email
JOIN categories c ON c.name = v.category_name
WHERE NOT EXISTS (SELECT 1 FROM auction_lots al WHERE al.title = v.title);

INSERT INTO bids (lot_id, buyer_id, amount, bid_time)
SELECT al.id, b.id, v.amount, v.bid_time::timestamp
FROM (VALUES
    ('Картина «Закат над рекой»', 'petrov.buyer@mail.ru', 52000, '2025-04-02 10:00:00'),
    ('Портрет неизвестного', 'smirnova@mail.ru', 135000, '2025-04-06 12:00:00'),
    ('Бронзовая статуэтка', 'fedorov@mail.ru', 52000, '2025-04-13 09:00:00'),
    ('Серебряный браслет', 'kozlova@mail.ru', 18500, '2025-03-15 14:00:00'),
    ('Золотой червонец 1923', 'lebedev@mail.ru', 215000, '2025-04-21 11:00:00'),
    ('Первое издание «Мастера и Маргариты»', 'orlova@mail.ru', 520000, '2025-05-02 16:00:00'),
    ('Пейзаж «Осень»', 'petrov.buyer@mail.ru', 28000, '2025-02-15 10:00:00')
) AS v(lot_title, buyer_email, amount, bid_time)
JOIN auction_lots al ON al.title = v.lot_title
JOIN buyers b ON b.email = v.buyer_email
WHERE NOT EXISTS (
    SELECT 1 FROM bids bd
    WHERE bd.lot_id = al.id AND bd.buyer_id = b.id AND bd.amount = v.amount
);

-- LastCall Eats Seed Data
-- 密码均为 111111
-- BCrypt hash 由 Spring Security BCryptPasswordEncoder(strength=10) 生成
-- 执行前请确保已运行 schema.sql

USE lastcall_eats;

-- =====================
-- 清空所有数据（按外键顺序）
-- =====================
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE review;
TRUNCATE TABLE post;
TRUNCATE TABLE pickup_code;
TRUNCATE TABLE merchant_notification;
TRUNCATE TABLE processed_event;
TRUNCATE TABLE outbox_event;
TRUNCATE TABLE orders;
TRUNCATE TABLE product_listing;
TRUNCATE TABLE product_template;
TRUNCATE TABLE user_favorite;
TRUNCATE TABLE merchant;
TRUNCATE TABLE `user`;
SET FOREIGN_KEY_CHECKS = 1;

-- =====================
-- 用户 (密码: 111111)
-- =====================
INSERT INTO `user` (email, password_hash, nickname) VALUES
('alice@example.com',    '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Alice'),
('bob@example.com',      '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Bob'),
('charlie@example.com',  '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Charlie');

-- =====================
-- 商家 (密码: 111111)
-- =====================
INSERT INTO `merchant` (email, password_hash, name, address, business_hours) VALUES
('bakery@example.com',   '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Golden Bakery',    '123 Main St, Boston, MA',      '08:00-20:00'),
('sushi@example.com',    '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Sakura Sushi',     '456 Elm St, Boston, MA',       '11:00-22:00'),
('cafe@example.com',     '$2a$10$lwTs9A3cEkFqSItPrHiPmeN3zcEg2zzs7VNxXUgrdKklpJs2gVc4.', 'Brew & Bite Cafe', '789 Oak Ave, Cambridge, MA',   '07:00-18:00');

-- =====================
-- 商品模板 (merchant_id 1=Bakery, 2=Sushi, 3=Cafe)
-- =====================
INSERT INTO `product_template` (merchant_id, name, description, original_price) VALUES
-- Golden Bakery
(1, 'Sourdough Bread',      'Freshly baked sourdough loaf',         8.00),
(1, 'Croissant Box',        'Assorted croissants x6',               15.00),
(1, 'Muffin Pack',          'Blueberry and chocolate muffins x4',   10.00),
-- Sakura Sushi
(2, 'Salmon Roll Set',      '8-piece salmon roll',                  18.00),
(2, 'Bento Box',            'Mixed sushi bento with miso soup',     22.00),
(2, 'Sashimi Platter',      'Chef selection sashimi x12',           28.00),
-- Brew & Bite Cafe
(3, 'Coffee & Sandwich',    'Latte + club sandwich combo',          14.00),
(3, 'Pastry Bundle',        'Danish pastry x3 + drip coffee',       12.00),
(3, 'Salad Bowl',           'Caesar salad with grilled chicken',    13.00);

-- =====================
-- 限时商品 (今天及未来几天，pickup window 傍晚)
-- template_id 对应上面的模板顺序 1-9
-- =====================
INSERT INTO `product_listing` (merchant_id, template_id, discount_price, quantity, remaining_quantity, pickup_start, pickup_end, date) VALUES
-- Golden Bakery 今天
(1, 1, 4.00,  5, 5, '17:00', '19:00', CURDATE()),
(1, 2, 8.00,  3, 3, '17:00', '19:00', CURDATE()),
-- Golden Bakery 明天
(1, 3, 5.00,  4, 4, '17:00', '19:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY)),

-- Sakura Sushi 今天
(2, 4, 10.00, 4, 4, '20:00', '21:30', CURDATE()),
(2, 5, 12.00, 3, 3, '20:00', '21:30', CURDATE()),
-- Sakura Sushi 明天
(2, 6, 15.00, 2, 2, '20:00', '21:30', DATE_ADD(CURDATE(), INTERVAL 1 DAY)),

-- Brew & Bite Cafe 今天
(3, 7, 7.00,  6, 6, '16:00', '18:00', CURDATE()),
(3, 8, 6.00,  5, 5, '16:00', '18:00', CURDATE()),
-- Brew & Bite Cafe 明天
(3, 9, 7.00,  4, 4, '16:00', '18:00', DATE_ADD(CURDATE(), INTERVAL 1 DAY));

-- =====================
-- 已完成订单 (for review seed)
-- listing_id 1=Sourdough, 4=Salmon Roll, 7=Coffee&Sandwich
-- =====================
INSERT INTO `orders` (user_id, listing_id, merchant_id, price, status, created_at, updated_at) VALUES
(1, 1, 1, 4.00,  'COMPLETED', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 1, 1, 4.00,  'COMPLETED', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 4, 2, 10.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 4, 2, 10.00, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 7, 3, 7.00,  'COMPLETED', DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(3, 7, 3, 7.00,  'COMPLETED', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================
-- 取货码 (每个订单一条)
-- =====================
INSERT INTO `pickup_code` (order_id, numeric_code, qr_code, used) VALUES
(1, '123401', 'ORDER:1', 1),
(2, '123402', 'ORDER:2', 1),
(3, '123403', 'ORDER:3', 1),
(4, '123404', 'ORDER:4', 1),
(5, '123405', 'ORDER:5', 1),
(6, '123406', 'ORDER:6', 1);

-- =====================
-- 评价 (order unique, template_id 对应模板)
-- template 1=Sourdough, 4=Salmon Roll, 7=Coffee&Sandwich
-- =====================
INSERT INTO `review` (order_id, user_id, merchant_id, template_id, rating, content, is_visible, created_at, updated_at) VALUES
(1, 1, 1, 1, 5, 'Amazing sourdough, perfectly crusty outside and soft inside. Great value!', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 2, 1, 1, 4, 'Really good bread for the price. Will definitely pick up again.', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, 3, 2, 4, 5, 'Super fresh salmon rolls. Could not believe this was a discount deal!', 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY)),
(4, 1, 2, 4, 4, 'Solid sushi, fresh fish and good portion size.', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(5, 2, 3, 7, 5, 'Best coffee and sandwich combo in Cambridge. The latte was perfect.', 1, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
(6, 3, 3, 7, 3, 'Sandwich was okay, a bit dry. Coffee was good though.', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================
-- 社区帖子 (user_id 1=Alice, 2=Bob, 3=Charlie)
-- =====================
INSERT INTO `post` (user_id, merchant_id, content, image_urls, like_count, comment_count, is_visible, created_at, updated_at) VALUES
(1, 1, 'Just picked up the sourdough bread from Golden Bakery — absolutely worth it at half price!', NULL, 3, 1, 1, NOW(), NOW()),
(2, 2, 'Sakura Sushi never disappoints. Got the salmon roll set for $10, fresh and delicious 🍣', NULL, 5, 2, 1, NOW(), NOW()),
(3, 3, 'Brew & Bite Cafe has the best morning deal. Coffee + sandwich for $7, can\'t beat that.', NULL, 2, 0, 1, NOW(), NOW()),
(1, NULL, 'Anyone else been saving so much money using LastCall? Picked up 3 meals this week under $20 total!', NULL, 8, 3, 1, NOW(), NOW()),
(2, 1, 'Golden Bakery croissant box is a steal. Got 6 croissants for $8, froze half of them.', NULL, 4, 1, 1, NOW(), NOW()),
(3, 2, 'The bento box from Sakura Sushi was huge. Definitely coming back tomorrow if they list again.', NULL, 6, 2, 1, NOW(), NOW());

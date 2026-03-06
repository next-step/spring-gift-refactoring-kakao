-- 카테고리
INSERT INTO category (id, name, color, image_url, description) VALUES (1, '간식', '#FF9800', 'http://img.com/snack.png', '간식 카테고리');
INSERT INTO category (id, name, color, image_url, description) VALUES (2, '음료', '#2196F3', 'http://img.com/drink.png', '음료 카테고리');

-- 상품
INSERT INTO product (id, name, price, image_url, category_id) VALUES (1, '초콜릿', 5000, 'http://img.com/choco.png', 1);
INSERT INTO product (id, name, price, image_url, category_id) VALUES (2, '커피', 3000, 'http://img.com/coffee.png', 2);

-- 옵션 (재고 관리 대상)
INSERT INTO options (id, name, quantity, product_id) VALUES (1, '초콜릿 기본', 10, 1);
INSERT INTO options (id, name, quantity, product_id) VALUES (2, '커피 기본', 1, 2);

-- 회원
INSERT INTO member (id, email, password, point) VALUES (1, 'sender@test.com', 'password', 100000);
INSERT INTO member (id, email, password, point) VALUES (2, 'receiver@test.com', 'password', 0);
INSERT INTO member (id, email, password, point) VALUES (3, 'poor@test.com', 'password', 0);

-- 위시
INSERT INTO wish (id, member_id, product_id) VALUES (1, 1, 1);

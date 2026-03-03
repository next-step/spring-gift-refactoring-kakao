INSERT INTO category (id, name, color, image_url, description)
VALUES (1, '테스트 카테고리', '#000000', 'http://img.test/c.png', '설명');

INSERT INTO product (id, name, price, image_url, category_id)
VALUES (1, '테스트 상품', 1000, 'http://img.test/p.png', 1);

INSERT INTO member (id, email, password, point)
VALUES (1, 'test@test.com', 'password', 100000);
INSERT INTO member (id, email, password, point)
VALUES (2, 'other@test.com', 'password', 50000);

INSERT INTO options (id, product_id, name, quantity)
VALUES (1, 1, '기본 옵션', 100);
INSERT INTO options (id, product_id, name, quantity)
VALUES (2, 1, '두번째 옵션', 50);

INSERT INTO wish (id, member_id, product_id)
VALUES (1, 1, 1);

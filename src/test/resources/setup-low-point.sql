INSERT INTO category (id, name, color, image_url, description)
VALUES (1, '테스트 카테고리', '#000000', 'http://img.test/c.png', '설명');

INSERT INTO product (id, name, price, image_url, category_id)
VALUES (1, '테스트 상품', 1000, 'http://img.test/p.png', 1);

INSERT INTO member (id, email, password, point)
VALUES (1, 'lowpoint@test.com', 'password', 500);

INSERT INTO options (id, product_id, name, quantity)
VALUES (1, 1, '기본 옵션', 100);

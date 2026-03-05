ALTER TABLE wish ADD CONSTRAINT uk_wish_member_product UNIQUE (member_id, product_id);

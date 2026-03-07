ALTER TABLE options ADD CONSTRAINT chk_option_quantity_non_negative CHECK (quantity >= 0);
ALTER TABLE member ADD CONSTRAINT chk_member_point_non_negative CHECK (point >= 0);

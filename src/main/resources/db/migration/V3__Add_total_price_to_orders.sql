ALTER TABLE orders ADD COLUMN total_price INT NOT NULL DEFAULT 0;

UPDATE orders o
    JOIN options opt ON o.option_id = opt.id
    JOIN product p ON opt.product_id = p.id
SET o.total_price = p.price * o.quantity;

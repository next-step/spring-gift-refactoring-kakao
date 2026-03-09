ALTER TABLE member ADD COLUMN role VARCHAR(10) NOT NULL DEFAULT 'USER';
UPDATE member SET role = 'ADMIN' WHERE email = 'admin@example.com';

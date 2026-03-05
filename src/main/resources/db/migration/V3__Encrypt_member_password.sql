alter table member add column password_hash varchar(255);
alter table member add column password_salt varchar(255);
alter table member drop column password;

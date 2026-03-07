alter table wish
    add constraint uk_wish_member_product unique (member_id, product_id);

alter table options
    add constraint uk_options_product_name unique (product_id, name);

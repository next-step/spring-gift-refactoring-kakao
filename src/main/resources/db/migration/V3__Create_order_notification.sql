create table order_notification
(
    id            bigint auto_increment primary key,
    order_id      bigint        not null,
    member_id     bigint        not null,
    message       varchar(2000) not null,  -- 주문 시점의 알림 문구(template_object)
    status        varchar(20)   not null,  -- PENDING(보낼 것), SENT(보냄), FAILED(실패)
    attempt_count int           not null default 0,
    last_error    varchar(1000),
    created_at    timestamp     not null,
    sent_at       timestamp,
    foreign key (order_id) references orders (id),
    foreign key (member_id) references member (id)
);

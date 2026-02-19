alter table accounts
add column initial_balance numeric(14,2) not null default 0;

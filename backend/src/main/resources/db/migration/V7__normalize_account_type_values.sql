-- Normalize legacy account type values to enum-compatible values.
update accounts
set type = case lower(type)
    when 'bank' then 'BANK'
    when 'banco' then 'BANK'
    when 'corrente' then 'BANK'
    when 'conta_corrente' then 'BANK'
    when 'checking' then 'BANK'

    when 'cash' then 'CASH'
    when 'dinheiro' then 'CASH'

    when 'card' then 'CARD'
    when 'cartao' then 'CARD'
    when 'cartão' then 'CARD'
    when 'credit_card' then 'CARD'

    when 'other' then 'OTHER'
    when 'outro' then 'OTHER'
    else 'OTHER'
end;

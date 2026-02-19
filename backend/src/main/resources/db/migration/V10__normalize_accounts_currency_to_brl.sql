UPDATE accounts
SET currency = 'BRL'
WHERE currency IS DISTINCT FROM 'BRL';

UPDATE card_invoices ci
SET due_date = (
    date_trunc(
        'month',
        make_date(ci.period_year, ci.period_month, 1)
        + CASE
            WHEN pm.billing_due_day > pm.billing_closing_day THEN INTERVAL '0 month'
            ELSE INTERVAL '1 month'
          END
    )::date
    + (
        LEAST(
            pm.billing_due_day,
            EXTRACT(
                DAY FROM (
                    date_trunc(
                        'month',
                        make_date(ci.period_year, ci.period_month, 1)
                        + CASE
                            WHEN pm.billing_due_day > pm.billing_closing_day THEN INTERVAL '0 month'
                            ELSE INTERVAL '1 month'
                          END
                    )
                    + INTERVAL '1 month - 1 day'
                )
            )::int
        ) - 1
    )
)
FROM payment_methods pm
WHERE ci.payment_method_id = pm.id
  AND pm.type = 'CARD'
  AND pm.billing_closing_day IS NOT NULL
  AND pm.billing_due_day IS NOT NULL;

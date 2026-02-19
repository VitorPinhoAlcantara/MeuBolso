-- Rule realignment:
-- invoice period = month of purchases (competência da compra)
-- closing date = period + 1 month (billing closing day)
-- due date = same month as closing when due_day > closing_day, otherwise next month

-- Step 1: move year to a temporary range to avoid unique conflicts during month shift.
UPDATE card_invoices ci
SET period_year = ci.period_year + 100
FROM payment_methods pm
WHERE ci.payment_method_id = pm.id
  AND pm.type = 'CARD'
  AND pm.billing_closing_day IS NOT NULL
  AND pm.billing_due_day IS NOT NULL;

-- Step 2: shift period one month back and recalculate closing/due dates.
UPDATE card_invoices ci
SET period_year = EXTRACT(
        YEAR FROM (make_date(ci.period_year - 100, ci.period_month, 1) - INTERVAL '1 month')
    )::int,
    period_month = EXTRACT(
        MONTH FROM (make_date(ci.period_year - 100, ci.period_month, 1) - INTERVAL '1 month')
    )::int,
    closing_date = (
        date_trunc('month', make_date(ci.period_year - 100, ci.period_month, 1))::date
        + (
            LEAST(
                pm.billing_closing_day,
                EXTRACT(DAY FROM (
                    date_trunc('month', make_date(ci.period_year - 100, ci.period_month, 1))
                    + INTERVAL '1 month - 1 day'
                ))::int
            ) - 1
        )
    ),
    due_date = (
        date_trunc(
            'month',
            make_date(ci.period_year - 100, ci.period_month, 1)
            + CASE
                WHEN pm.billing_due_day > pm.billing_closing_day THEN INTERVAL '0 month'
                ELSE INTERVAL '1 month'
              END
        )::date
        + (
            LEAST(
                pm.billing_due_day,
                EXTRACT(DAY FROM (
                    date_trunc(
                        'month',
                        make_date(ci.period_year - 100, ci.period_month, 1)
                        + CASE
                            WHEN pm.billing_due_day > pm.billing_closing_day THEN INTERVAL '0 month'
                            ELSE INTERVAL '1 month'
                          END
                    )
                    + INTERVAL '1 month - 1 day'
                ))::int
            ) - 1
        )
    )
FROM payment_methods pm
WHERE ci.payment_method_id = pm.id
  AND pm.type = 'CARD'
  AND pm.billing_closing_day IS NOT NULL
  AND pm.billing_due_day IS NOT NULL;

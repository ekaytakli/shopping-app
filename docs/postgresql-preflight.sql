-- Read-only checks. Review results before making any schema or data changes.
SELECT lower(trim(email)) AS normalized_email, count(*) AS occurrences
FROM users
GROUP BY lower(trim(email))
HAVING count(*) > 1;

SELECT id
FROM users
WHERE email IS NULL OR trim(email) = '' OR length(email) > 254
   OR password IS NULL OR password = '';

SELECT product_id
FROM products
WHERE product_name IS NULL OR length(product_name) > 40
   OR length(quantity_per_unit) > 20 OR unit_price < 0 OR units_in_stock < 0;

SELECT p.product_id
FROM products p
LEFT JOIN categories c ON c.category_id = p.category_id
WHERE c.category_id IS NULL;

-- inventory-web-study
-- DB構築後の読み取り専用確認SQL
--
-- このファイルはINSERT、UPDATE、DELETEを行いません。

-- 1. 接続先確認
SELECT
    current_database() AS database_name,
    current_user AS database_user,
    current_setting('server_version') AS postgresql_version;

-- 2. 必要なテーブルが存在するか
SELECT
    to_regclass('public.products')
        AS products_table,

    to_regclass('public.app_users')
        AS app_users_table,

    to_regclass('public.product_operation_logs')
        AS product_operation_logs_table;

-- 3. 列定義確認
SELECT
    table_name,
    ordinal_position,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable,
    column_default,
    is_identity
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN (
      'products',
      'app_users',
      'product_operation_logs'
  )
ORDER BY
    table_name,
    ordinal_position;

-- 4. 主キー・UNIQUE・CHECK・外部キー確認
SELECT
    conrelid::regclass AS table_name,
    conname AS constraint_name,
    CASE contype
        WHEN 'p' THEN 'PRIMARY KEY'
        WHEN 'u' THEN 'UNIQUE'
        WHEN 'c' THEN 'CHECK'
        WHEN 'f' THEN 'FOREIGN KEY'
        ELSE contype::text
    END AS constraint_type,
    pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE conrelid IN (
    'products'::regclass,
    'app_users'::regclass,
    'product_operation_logs'::regclass
)
ORDER BY
    conrelid::regclass::text,
    constraint_type,
    conname;

-- 5. 商品一覧
SELECT
    id,
    name,
    price,
    stock
FROM products
ORDER BY id;

-- 6. ユーザー確認
-- password_hashの中身は表示せず、形式だけを確認する。
SELECT
    id,
    login_id,
    display_name,
    length(password_hash) AS password_hash_length,
    array_length(
        string_to_array(password_hash, ':'),
        1
    ) AS password_hash_parts
FROM app_users
ORDER BY id;

-- password_hash_partsが3なら、
-- 「反復回数:salt:hash」の3部分に分かれている。

-- 7. 操作履歴
SELECT
    logs.id,
    logs.product_id,
    products.name AS product_name,
    logs.operation,
    logs.operated_by,
    users.login_id AS operated_by_login_id,
    logs.operated_at
FROM product_operation_logs AS logs
LEFT JOIN products
    ON products.id = logs.product_id
JOIN app_users AS users
    ON users.id = logs.operated_by
ORDER BY logs.id DESC;

-- 8. 不正な価格・在庫数がないか
-- 正常なら0件。
SELECT
    id,
    name,
    price,
    stock
FROM products
WHERE price < 0
   OR stock < 0;

-- 9. 存在しないユーザーを参照する履歴がないか
-- 外部キーが正常なら0件。
SELECT logs.*
FROM product_operation_logs AS logs
LEFT JOIN app_users AS users
    ON users.id = logs.operated_by
WHERE users.id IS NULL;

-- 10. 商品が削除された後の履歴
-- ON DELETE SET NULLによりproduct_idがNULLになった履歴を確認する。
SELECT
    id,
    operation,
    operated_by,
    operated_at
FROM product_operation_logs
WHERE product_id IS NULL
ORDER BY id DESC;

-- 11. 件数まとめ
SELECT
    (SELECT count(*) FROM products)
        AS product_count,

    (SELECT count(*) FROM app_users)
        AS user_count,

    (SELECT count(*) FROM product_operation_logs)
        AS operation_log_count;

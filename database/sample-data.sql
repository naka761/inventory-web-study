-- inventory-web-study
-- ローカル学習環境用のサンプルデータ
--
-- 実行順:
--   1. schema.sql
--   2. sample-data.sql
--
-- 注意:
-- このファイルに含まれるログイン情報は公開された学習用データです。
-- インターネットへ公開する環境や実務環境では使用しないでください。
--
-- 学習用ログイン:
--   login_id: training-admin
--   password: training-only-2026
--
-- password_hashは、現在のPasswordUtilと同じ次の条件で作成しています。
--   PBKDF2WithHmacSHA256
--   iterations: 120000
--   salt length: 16 bytes
--   key length: 256 bits

BEGIN;

INSERT INTO app_users (
    login_id,
    password_hash,
    display_name
)
VALUES (
    'training-admin',
    '120000:23IWyVo/Zwf+KjuiJx0Ymg==:CATTWVhiZQmoF7s0KQDgY5ndaKC6/TWvwYJZzU9oNkg=',
    '学習用管理者'
)
ON CONFLICT (login_id) DO NOTHING;

INSERT INTO products (
    name,
    price,
    stock
)
SELECT
    'りんご',
    150,
    30
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE name = 'りんご'
);

INSERT INTO products (
    name,
    price,
    stock
)
SELECT
    'みかん',
    200,
    20
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE name = 'みかん'
);

INSERT INTO products (
    name,
    price,
    stock
)
SELECT
    'バナナ',
    180,
    15
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE name = 'バナナ'
);

COMMIT;

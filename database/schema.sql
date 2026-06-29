-- inventory-web-study
-- PostgreSQL 18向けのテーブル定義
--
-- 実行対象:
--   database: postgres
--
-- このファイルは既存データを削除しません。
-- 既に同名テーブルがある場合はCREATEをスキップします。
-- 作り直す場合は、必要なデータを退避してから手動でDROPしてください。

BEGIN;

CREATE TABLE IF NOT EXISTS products (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price INTEGER NOT NULL,
    stock INTEGER NOT NULL,

    CONSTRAINT products_price_non_negative
        CHECK (price >= 0),

    CONSTRAINT products_stock_non_negative
        CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS app_users (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(500) NOT NULL,
    display_name VARCHAR(100) NOT NULL,

    CONSTRAINT app_users_login_id_unique
        UNIQUE (login_id)
);

CREATE TABLE IF NOT EXISTS product_operation_logs (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    product_id INTEGER,

    operation VARCHAR(20) NOT NULL,

    operated_by INTEGER NOT NULL,

    operated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT product_operation_logs_product_fk
        FOREIGN KEY (product_id)
        REFERENCES products(id)
        ON DELETE SET NULL,

    CONSTRAINT product_operation_logs_user_fk
        FOREIGN KEY (operated_by)
        REFERENCES app_users(id)
);

COMMENT ON TABLE products IS
    '在庫管理対象の商品';

COMMENT ON COLUMN products.id IS
    'PostgreSQLが自動発行する商品ID';

COMMENT ON COLUMN products.name IS
    '商品名';

COMMENT ON COLUMN products.price IS
    '0以上の価格';

COMMENT ON COLUMN products.stock IS
    '0以上の在庫数';

COMMENT ON TABLE app_users IS
    '在庫管理Webアプリへログインするユーザー';

COMMENT ON COLUMN app_users.password_hash IS
    'PBKDF2の反復回数:salt:hashを保存する';

COMMENT ON TABLE product_operation_logs IS
    '商品の操作履歴';

COMMENT ON COLUMN product_operation_logs.product_id IS
    '対象商品。商品削除後はNULLになる';

COMMENT ON COLUMN product_operation_logs.operation IS
    'CREATE、UPDATE、DELETEなどの操作名';

COMMENT ON COLUMN product_operation_logs.operated_by IS
    '操作したapp_users.id';

COMMENT ON COLUMN product_operation_logs.operated_at IS
    '操作日時。未指定時はPostgreSQLの現在日時';

COMMIT;

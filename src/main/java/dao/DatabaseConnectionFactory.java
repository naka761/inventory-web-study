package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * PostgreSQLへの接続作成を一か所にまとめるクラス。
 *
 * DBパスワードをJavaソースへ直接書かず、
 * 環境変数から取得する。
 */
public final class DatabaseConnectionFactory {

    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/postgres";

    private static final String DEFAULT_USER =
            "postgres";

    private static final String ENV_URL =
            "INVENTORY_DB_URL";

    private static final String ENV_USER =
            "INVENTORY_DB_USER";

    private static final String ENV_PASSWORD =
            "INVENTORY_DB_PASSWORD";

    private DatabaseConnectionFactory() {
    }

    /**
     * 環境変数を使ってPostgreSQLとの接続を作成する。
     *
     * URLとユーザー名が未設定の場合は、
     * ローカル学習環境用の既定値を使用する。
     *
     * パスワードには既定値を設けず、
     * INVENTORY_DB_PASSWORDを必須とする。
     */
    public static Connection getConnection()
            throws SQLException {

        loadDriver();

        String url = getEnvironmentValueOrDefault(
                ENV_URL,
                DEFAULT_URL);

        String user = getEnvironmentValueOrDefault(
                ENV_USER,
                DEFAULT_USER);

        String password =
                System.getenv(ENV_PASSWORD);

        if (password == null
                || password.trim().isEmpty()) {

            throw new SQLException(
                    "環境変数 "
                    + ENV_PASSWORD
                    + " が設定されていません。");
        }

        return DriverManager.getConnection(
                url,
                user,
                password);
    }

    /**
     * PostgreSQL JDBC Driverを読み込む。
     */
    private static void loadDriver()
            throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "PostgreSQL JDBCドライバが見つかりません。",
                    e);
        }
    }

    /**
     * 環境変数が未設定または空文字なら既定値を返す。
     */
    private static String getEnvironmentValueOrDefault(
            String environmentName,
            String defaultValue) {

        String value =
                System.getenv(environmentName);

        if (value == null
                || value.trim().isEmpty()) {

            return defaultValue;
        }

        return value;
    }
}

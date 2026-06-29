package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import security.PasswordUtil;
import model.User;

/**
 * app_usersテーブルへのアクセスを担当するクラス。
 */
public class UserDAO {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/postgres";

    private static final String USER = "postgres";

    // PostgreSQLの実際のパスワードに変更する
    private static final String PASSWORD = "YOUR_DB_PASSWORD";

    private Connection getConnection() throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "PostgreSQL JDBCドライバが見つかりません。",
                    e
            );
        }

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    /**
     * ログインIDとパスワードが一致するユーザーを取得する。
     *
     * @return 一致したUser。存在しなければnull。
     */
    public User findByLoginIdAndPassword(
            String loginId,
            String password) throws SQLException {

        String sql =
                "SELECT id, login_id, display_name, password_hash " +
                "FROM app_users " +
                "WHERE login_id = ?";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setString(1, loginId);

            try (ResultSet resultSet =
                    statement.executeQuery()) {

                if (!resultSet.next()) {
                    return null;
                }

                String passwordHash =
                        resultSet.getString("password_hash");

                if (!PasswordUtil.verify(
                        password,
                        passwordHash)) {

                    return null;
                }

                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("login_id"),
                        resultSet.getString("display_name")
                );
            }
        }
    }
}
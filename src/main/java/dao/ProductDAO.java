package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Statement;

import model.Product;

/**
 * productsテーブルへのアクセスを担当するクラス。
 */
public class ProductDAO {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/postgres";

    private static final String USER = "postgres";

    // 自分のPostgreSQLの本物のパスワードへ変更する
    private static final String PASSWORD = "YOUR_DB_PASSWORD";

    /**
     * PostgreSQLとの接続を作成する。
     */
    private Connection getConnection() throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "PostgreSQL JDBCドライバが見つかりません", e);
        }

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }

    /**
     * 全商品を取得する。
     */
    public List<Product> findAll() throws SQLException {

        List<Product> products = new ArrayList<Product>();

        String sql =
                "SELECT id, name, price, stock " +
                "FROM products " +
                "ORDER BY id";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql);

            ResultSet resultSet =
                    statement.executeQuery()
        ) {
            while (resultSet.next()) {

                Product product = new Product(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("price"),
                        resultSet.getInt("stock")
                );

                products.add(product);
            }
        }

        return products;
    }
    /**
     * 商品を新規登録する。
     */
    public int insert(Product product) throws SQLException {

        String sql =
                "INSERT INTO products " +
                "(name, price, stock) " +
                "VALUES (?, ?, ?)";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setString(1, product.getName());
            statement.setInt(2, product.getPrice());
            statement.setInt(3, product.getStock());

            return statement.executeUpdate();
        }
    }
    /**
     * 指定されたIDの商品を1件取得する。
     */
    public Product findById(int id) throws SQLException {

        String sql =
                "SELECT id, name, price, stock " +
                "FROM products " +
                "WHERE id = ?";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return new Product(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("price"),
                            resultSet.getInt("stock")
                    );
                }
            }
        }

        return null;
    }
    /**
     * 商品情報を更新する。
     */
    public int update(Product product) throws SQLException {

        String sql =
                "UPDATE products " +
                "SET name = ?, price = ?, stock = ? " +
                "WHERE id = ?";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setString(1, product.getName());
            statement.setInt(2, product.getPrice());
            statement.setInt(3, product.getStock());
            statement.setInt(4, product.getId());

            return statement.executeUpdate();
        }
    }
    /**
     * 指定されたIDの商品を削除する。
     */
    public int deleteById(int id) throws SQLException {

        String sql =
                "DELETE FROM products " +
                "WHERE id = ?";

        try (
            Connection connection = getConnection();

            PreparedStatement statement =
                    connection.prepareStatement(sql)
        ) {
            statement.setInt(1, id);

            return statement.executeUpdate();
        }
    }
    /**
     * 商品と操作履歴を一つのトランザクションで登録する。
     */
    public int insertWithLog(
            Product product,
            int userId) throws SQLException {

        String productSql =
                "INSERT INTO products " +
                "(name, price, stock) " +
                "VALUES (?, ?, ?)";

        String logSql =
                "INSERT INTO product_operation_logs " +
                "(product_id, operation, operated_by) " +
                "VALUES (?, ?, ?)";

        try (Connection connection = getConnection()) {

            // ここから自動確定を止める
            connection.setAutoCommit(false);

            try {
                int productId;

                try (
                    PreparedStatement productStatement =
                            connection.prepareStatement(
                                    productSql,
                                    Statement.RETURN_GENERATED_KEYS)
                ) {
                    productStatement.setString(
                            1, product.getName());

                    productStatement.setInt(
                            2, product.getPrice());

                    productStatement.setInt(
                            3, product.getStock());

                    int insertedCount =
                            productStatement.executeUpdate();

                    if (insertedCount != 1) {
                        throw new SQLException(
                                "商品の登録件数が不正です。");
                    }

                    try (ResultSet keys =
                            productStatement.getGeneratedKeys()) {

                        if (!keys.next()) {
                            throw new SQLException(
                                    "商品IDを取得できませんでした。");
                        }

                        productId = keys.getInt(1);
                    }
                }

                try (
                    PreparedStatement logStatement =
                            connection.prepareStatement(logSql)
                ) {
                    logStatement.setInt(1, productId);
                    logStatement.setString(2, "CREATE");
                    //logStatement.setString(2, null);
                    logStatement.setInt(3, userId);

                    int logCount =
                            logStatement.executeUpdate();

                    if (logCount != 1) {
                        throw new SQLException(
                                "操作履歴を登録できませんでした。");
                    }
                }

                // 2つとも成功したのでDBへ確定
                connection.commit();

                return productId;

            } catch (SQLException e) {

                // 途中で失敗したので全部取り消す
                try {
                    connection.rollback();

                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }
        }
    }
}
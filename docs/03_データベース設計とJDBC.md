# 03. データベース設計とJDBC

この章では、`inventory-web-study`で使用するPostgreSQLの表構造と、JavaからJDBCを使ってデータを読み書きする仕組みを整理します。

このプロジェクトでは、単にSQLを実行するだけではなく、次の流れを作っています。

```text
JavaのProductやUser
  ↓ DAO
PreparedStatement
  ↓ JDBC Driver
PostgreSQL
  ↓ ResultSet・更新件数
JavaのProductやUser
```

この章の目的は、次の疑問へ答えられるようになることです。

- PostgreSQLサーバー、データベース、テーブルは何が違うのか
- 主キー、外部キー、制約は何を守っているのか
- Javaの`Product`とDBの`products`はどう対応しているのか
- `PreparedStatement`の`?`へ何を入れているのか
- `ResultSet.next()`がなぜ必要なのか
- INSERT、UPDATE、DELETEの戻り値は何なのか
- 自動採番された商品IDをどう取得しているのか
- 商品登録と操作履歴をなぜ1つのトランザクションにしたのか
- パスワードをなぜ平文で保存しないのか

---

## 1. PostgreSQLサーバー、データベース、テーブルの違い

最初に、似た言葉を分けます。

```text
PostgreSQLサーバー
└─ postgresデータベース
   ├─ productsテーブル
   ├─ app_usersテーブル
   └─ product_operation_logsテーブル
```

| 用語 | このプロジェクトでの意味 |
| --- | --- |
| PostgreSQLサーバー | Windowsサービスとして動いているDBMS本体 |
| データベース | 接続先として指定している`postgres` |
| テーブル | 商品、ユーザー、操作履歴を保存する表 |
| 行・レコード | 商品1件、ユーザー1人、操作履歴1件 |
| 列・カラム | `id`、`name`、`price`などの項目 |

接続URLは次です。

```text
jdbc:postgresql://localhost:5432/postgres
```

分解すると、

```text
jdbc:postgresql://   PostgreSQL用JDBC接続
localhost            このPC
5432                 PostgreSQLのポート
postgres             接続するデータベース名
```

です。

---

## 2. このプロジェクトで使う3テーブル

### 2.1 全体関係

```text
app_users
  id
   ↑
   │ operated_by
   │
product_operation_logs
   │
   │ product_id
   ↓
products
  id
```

意味は次の通りです。

```text
products
  └─ 商品そのもの

app_users
  └─ ログインできるユーザー

product_operation_logs
  └─ どの商品を、誰が、いつ、何の操作をしたか
```

### 2.2 関係の読み方

`product_operation_logs`の1行は、次のような情報を表します。

```text
商品ID 8を
ユーザーID 1の管理者が
CREATEという操作で
2026-06-29 22:46:32に登録した
```

商品とユーザーの本体情報を毎回ログへ複製せず、IDで参照します。

---

## 3. `products`テーブル

このプロジェクトで作成した商品テーブルは、次の構造です。

```sql
CREATE TABLE products (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price INTEGER NOT NULL CHECK (price >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0)
);
```

### 3.1 各列

| 列 | 型 | 役割 |
| --- | --- | --- |
| `id` | `INTEGER` | 商品を一意に識別する番号 |
| `name` | `VARCHAR(100)` | 商品名 |
| `price` | `INTEGER` | 価格 |
| `stock` | `INTEGER` | 在庫数 |

### 3.2 `PRIMARY KEY`

```sql
PRIMARY KEY
```

主キーは、1行を一意に特定する列です。

```text
商品ID 1 → りんご
商品ID 2 → みかん
```

同じIDを2行へ登録することはできません。

主キーがあることで、次のSQLが「どの商品か」を特定できます。

```sql
SELECT ...
FROM products
WHERE id = ?;
```

```sql
UPDATE products
SET ...
WHERE id = ?;
```

```sql
DELETE FROM products
WHERE id = ?;
```

商品名を主キーにしない理由は、同名商品があり得たり、商品名が変更されたりするためです。

---

## 4. `GENERATED ALWAYS AS IDENTITY`

```sql
id INTEGER GENERATED ALWAYS AS IDENTITY
```

これは、PostgreSQLに商品IDを自動発行させる指定です。

INSERTでは、IDを指定しません。

```sql
INSERT INTO products (name, price, stock)
VALUES ('りんご', 150, 30);
```

PostgreSQLが次の番号を発行します。

```text
id = 1
```

Javaで新規商品を作るとき、登録前の`Product`には仮に`id=0`を入れています。

```java
new Product(0, name, price, stock)
```

本物のIDはINSERT時にPostgreSQLが決めます。

### 4.1 IDが欠番になる理由

検証では、商品IDが次のようになりました。

```text
1, 2, 3, 4, 5, 8, 10 ...
```

途中の番号がないことがあります。

これは異常ではありません。IDENTITY用の採番値は、次のような場合でも消費されることがあります。

- INSERT後にトランザクションをrollbackした
- 登録後に行を削除した
- 登録処理の途中で失敗した

主キーの目的は連番を美しく並べることではなく、**各行を一意に識別すること**です。

---

## 5. `NOT NULL`と`CHECK`

### 5.1 `NOT NULL`

```sql
name VARCHAR(100) NOT NULL
```

`NOT NULL`は、その列へ`NULL`を入れられない制約です。

```text
name = NULL
price = NULL
stock = NULL
```

は許可されません。

ただし、空文字`''`と`NULL`は別です。

```text
NULL → 値そのものがない
''   → 長さ0の文字列
```

そのため、商品名の空文字はServlet側でも確認しています。

```java
name == null || name.trim().isEmpty()
```

### 5.2 `CHECK`

```sql
CHECK (price >= 0)
CHECK (stock >= 0)
```

DB自身が、価格と在庫数の負数を拒否します。

Servlet側でも確認しています。

```java
if (price < 0) {
    // エラー
}
```

なぜ両方に必要なのでしょうか。

```text
Servletの入力チェック
  └─ 利用者へ分かりやすいエラーを返す

DBのCHECK制約
  └─ どの経路から来ても不正データを保存しない
```

Javaアプリ以外から直接SQLを実行されても、DB制約が最後の防波堤になります。

---

## 6. `app_users`テーブル

最初は平文パスワード列を持つ形で作りました。

```sql
CREATE TABLE app_users (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL
);
```

しかし、セキュリティ学習で平文の`password`列を削除し、`password_hash`へ移行しました。

最終的に必要な構造は、概ね次です。

```sql
CREATE TABLE app_users (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(500) NOT NULL,
    display_name VARCHAR(100) NOT NULL
);
```

### 6.1 各列

| 列 | 役割 |
| --- | --- |
| `id` | ユーザーを一意に識別する |
| `login_id` | ログイン画面で入力するID |
| `password_hash` | PBKDF2で生成した保存用文字列 |
| `display_name` | 画面へ表示する「管理者」などの名前 |

### 6.2 `UNIQUE`

```sql
login_id VARCHAR(50) NOT NULL UNIQUE
```

`UNIQUE`により、同じログインIDを複数ユーザーへ登録できません。

```text
admin
admin
```

という2人のユーザーがいると、ログイン時にどちらを選ぶのか分からなくなるためです。

---

## 7. なぜパスワードを平文保存しないのか

平文保存：

```text
login_id = admin
password = admin123
```

DBが漏れた場合、利用者のパスワードをそのまま読めます。

現在は、次のような保存用文字列を使用します。

```text
120000:ソルトをBase64化した値:ハッシュをBase64化した値
```

構成：

```text
反復回数 : salt : hash
```

Javaでは`PasswordUtil`がPBKDF2を使用します。

```text
入力されたパスワード
  +
DBに保存されたsalt
  +
反復回数
  ↓
PBKDF2
  ↓
計算したハッシュ
  ↓
DBのハッシュと比較
```

`User`オブジェクトにはパスワードもハッシュも持たせていません。

```java
private int id;
private String loginId;
private String displayName;
```

ログイン後の画面表示や操作履歴には、生パスワードもハッシュも不要だからです。

---

## 8. パスワードハッシュに改行が混入した失敗

ハッシュ生成結果をpsqlへ貼り付けたとき、末尾へ改行が混入し、ログインできなくなりました。

正常な値：

```text
120000:...:...=
```

実際に保存された状態：

```text
120000:...:...=\n
```

Java側が計算したハッシュと、DBから取得した余計な改行付き文字列が一致しません。

末尾の空白・改行を削除しました。

```sql
UPDATE app_users
SET password_hash =
    BTRIM(password_hash, E' \t\r\n')
WHERE login_id = 'admin';
```

この失敗から分かることは、次です。

```text
画面上では似て見える文字列でも
実際の文字数・末尾文字が違えば別の値
```

---

## 9. `product_operation_logs`テーブル

操作履歴テーブルは次の構造で作成しました。

```sql
CREATE TABLE product_operation_logs (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    product_id INTEGER
        REFERENCES products(id)
        ON DELETE SET NULL,

    operation VARCHAR(20) NOT NULL,

    operated_by INTEGER NOT NULL
        REFERENCES app_users(id),

    operated_at TIMESTAMP NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);
```

### 9.1 各列

| 列 | 役割 |
| --- | --- |
| `id` | 履歴1件を識別する番号 |
| `product_id` | 操作対象の商品ID |
| `operation` | `CREATE`などの操作名 |
| `operated_by` | 操作したユーザーID |
| `operated_at` | 操作日時 |

---

## 10. 外部キーとは何か

### 10.1 `product_id`

```sql
product_id INTEGER
    REFERENCES products(id)
```

これは、

> `product_operation_logs.product_id`へ入れる値は、原則として`products.id`に存在する値でなければならない

という制約です。

### 10.2 `operated_by`

```sql
operated_by INTEGER NOT NULL
    REFERENCES app_users(id)
```

存在しないユーザーIDを操作履歴へ登録できません。

```text
app_usersにid=999が存在しない
  ↓
operated_by=999の履歴INSERT
  ↓
外部キー違反
```

### 10.3 外部キーが守るもの

外部キーがない場合、次のような意味不明なデータを作れます。

```text
商品ID 9999の操作履歴
操作ユーザーID 8888
```

しかし、対応する商品やユーザーが存在しません。

外部キーは、表同士のつながりをDB自身に守らせます。

---

## 11. `ON DELETE SET NULL`

```sql
product_id INTEGER
    REFERENCES products(id)
    ON DELETE SET NULL
```

商品を削除したとき、履歴行まで削除せず、`product_id`を`NULL`へします。

削除前：

```text
履歴ID=1
product_id=8
operation=CREATE
```

商品ID 8を削除後：

```text
履歴ID=1
product_id=NULL
operation=CREATE
```

これにより、

> 商品本体はもう存在しないが、過去に登録操作が行われた

という履歴を残せます。

そのため、`product_id`には`NOT NULL`を付けていません。

一方、`operated_by`は`NOT NULL`です。このプロジェクトでは、操作したユーザーの存在を必須としています。

---

## 12. `DEFAULT CURRENT_TIMESTAMP`

```sql
operated_at TIMESTAMP NOT NULL
    DEFAULT CURRENT_TIMESTAMP
```

INSERT時に日時を指定しなくても、PostgreSQLが現在日時を入れます。

JavaのINSERT：

```sql
INSERT INTO product_operation_logs
(product_id, operation, operated_by)
VALUES (?, ?, ?)
```

`operated_at`を書いていません。

しかしDB側のDEFAULTにより、日時が自動設定されます。

```text
Java：何を、誰が、どの操作をしたか
DB：いつ登録されたかを現在日時で補う
```

---

## 13. JavaのModelとDB列の対応

### 13.1 `Product`

```java
public class Product {
    private int id;
    private String name;
    private int price;
    private int stock;
}
```

対応：

| Java | DB |
| --- | --- |
| `Product.id` | `products.id` |
| `Product.name` | `products.name` |
| `Product.price` | `products.price` |
| `Product.stock` | `products.stock` |

### 13.2 `User`

```java
public class User {
    private int id;
    private String loginId;
    private String displayName;
}
```

対応：

| Java | DB |
| --- | --- |
| `User.id` | `app_users.id` |
| `User.loginId` | `app_users.login_id` |
| `User.displayName` | `app_users.display_name` |

`password_hash`は認証時に一時的に読むだけで、`User`へ保存しません。

---

## 14. JavaBeanとしてのModel

`Product`と`User`には、次があります。

- privateフィールド
- publicなgetter
- publicなsetter
- 引数なしコンストラクタ
- 値をまとめて受け取るコンストラクタ

例：

```java
public String getName() {
    return name;
}
```

JSPのEL式は、次のようにgetterを利用します。

```jsp
${product.name}
```

概念上：

```java
product.getName()
```

DBの列名をJSPが直接読んでいるわけではありません。

```text
DB列
  ↓ DAO
Productのフィールド
  ↓ getter
JSP
```

---

## 15. DAOの役割

DAOはData Access Objectの略です。

このプロジェクトでは次の役割です。

```text
Servlet
  ↓「商品一覧をください」
ProductDAO
  ↓ SQLを実行
PostgreSQL
  ↓ ResultSet
ProductDAO
  ↓ Productへ変換
Servlet
```

DAOへDB処理をまとめることで、ServletはSQLの細部を知らずに済みます。

Servlet側：

```java
List<Product> products = productDAO.findAll();
```

DAO側：

```sql
SELECT id, name, price, stock
FROM products
ORDER BY id
```

---

## 16. JDBC接続を作る流れ

現在のDAOでは、次の情報を使用します。

```java
private static final String URL =
        "jdbc:postgresql://localhost:5432/postgres";

private static final String USER = "postgres";

private static final String PASSWORD =
        "YOUR_DB_PASSWORD";
```

ドライバを読み込みます。

```java
Class.forName("org.postgresql.Driver");
```

接続を作ります。

```java
DriverManager.getConnection(
    URL,
    USER,
    PASSWORD
);
```

返り値は`Connection`です。

```text
Connection
  └─ JavaアプリとPostgreSQLの1本の接続
```

> [!CAUTION]
> GitHub版の`YOUR_DB_PASSWORD`は公開用の文字列であり、本物のDBパスワードではない。  
> 本物の値をソースへ書いてcommitしない。

---

## 17. `PreparedStatement`を使う理由

文字列連結でSQLを作ると、入力値がSQLの一部として解釈される危険があります。

危険な形：

```java
String sql =
    "SELECT * FROM app_users " +
    "WHERE login_id = '" + loginId + "'";
```

現在の形：

```java
String sql =
    "SELECT id, login_id, display_name, password_hash " +
    "FROM app_users " +
    "WHERE login_id = ?";

PreparedStatement statement =
        connection.prepareStatement(sql);

statement.setString(1, loginId);
```

`?`は後から値を入れる場所です。

```text
SQLの構造
WHERE login_id = ?

値
admin
```

値はSQL命令ではなく、検索条件のデータとして扱われます。

これがSQLインジェクション対策の基本です。

---

## 18. `?`の番号は1から始まる

Javaの配列添字は0から始まりますが、`PreparedStatement`のパラメータ番号は1からです。

```sql
UPDATE products
SET name = ?, price = ?, stock = ?
WHERE id = ?
```

対応：

```java
statement.setString(1, product.getName());
statement.setInt(2, product.getPrice());
statement.setInt(3, product.getStock());
statement.setInt(4, product.getId());
```

```text
1番目の? → name
2番目の? → price
3番目の? → stock
4番目の? → id
```

順番を間違えると、別の列へ別の値を設定しようとしてエラーになったり、意図しない更新になったりします。

---

## 19. SELECTと`executeQuery()`

商品一覧では、次を実行します。

```java
ResultSet resultSet =
        statement.executeQuery();
```

`executeQuery()`は主にSELECT用で、戻り値は`ResultSet`です。

```text
SELECT
  ↓
ResultSet
```

`ResultSet`には検索結果の複数行が入っています。

---

## 20. `ResultSet.next()`が必要な理由

SELECT直後のカーソルは、最初の行の手前にあります。

```text
カーソル
  ↓
[行1]
[行2]
[行3]
```

`next()`を呼ぶと次の行へ進みます。

```java
while (resultSet.next()) {
```

```text
1回目のnext() → 行1へ移動、true
2回目のnext() → 行2へ移動、true
3回目のnext() → 行3へ移動、true
4回目のnext() → 次の行なし、false
```

現在行の列を読みます。

```java
resultSet.getInt("id");
resultSet.getString("name");
resultSet.getInt("price");
resultSet.getInt("stock");
```

一覧は複数行なので`while`です。

1件検索は最大1行なので`if`です。

```java
if (resultSet.next()) {
    return new Product(...);
}
```

---

## 21. `ResultSet`から`List<Product>`へ変換

商品一覧DAOの内部：

```text
ResultSet
├─ 行1：1, りんご, 150, 30
├─ 行2：2, みかん, 200, 20
└─ 行3：3, バナナ, 180, 15
```

各行からJavaオブジェクトを作ります。

```java
Product product = new Product(
    resultSet.getInt("id"),
    resultSet.getString("name"),
    resultSet.getInt("price"),
    resultSet.getInt("stock")
);
```

リストへ追加します。

```java
products.add(product);
```

結果：

```text
List<Product>
├─ Product(1, "りんご", 150, 30)
├─ Product(2, "みかん", 200, 20)
└─ Product(3, "バナナ", 180, 15)
```

DAOを抜けると`Connection`と`ResultSet`は閉じますが、通常の`Product`と`List`はその後もServletやJSPで使えます。

---

## 22. INSERT、UPDATE、DELETEと`executeUpdate()`

データを変更するSQLでは、主に`executeUpdate()`を使います。

```java
int count = statement.executeUpdate();
```

戻り値は、変更された行数です。

### INSERT

```text
1 → 1行登録
```

### UPDATE

```text
1 → 対象を1行更新
0 → WHEREに一致する商品がない
```

### DELETE

```text
1 → 1行削除
0 → 対象が存在しない
```

この戻り値を使って、Servletは404を返すか判断します。

---

## 23. 商品のINSERT

SQL：

```sql
INSERT INTO products
(name, price, stock)
VALUES (?, ?, ?)
```

Java：

```java
productStatement.setString(
        1, product.getName());

productStatement.setInt(
        2, product.getPrice());

productStatement.setInt(
        3, product.getStock());
```

`id`はPostgreSQLがIDENTITYで発行するため、INSERT対象へ含めません。

---

## 24. 自動発行された商品IDを取得する

操作履歴へ`product_id`を登録するには、今INSERTした商品のIDが必要です。

Statement作成時に指定します。

```java
connection.prepareStatement(
    productSql,
    Statement.RETURN_GENERATED_KEYS
);
```

INSERT後：

```java
ResultSet keys =
        productStatement.getGeneratedKeys();
```

次のキー行へ進みます。

```java
if (!keys.next()) {
    throw new SQLException(
        "商品IDを取得できませんでした。");
}
```

IDを取得します。

```java
int productId = keys.getInt(1);
```

流れ：

```text
productsへINSERT
  ↓ PostgreSQLがid=8を発行
getGeneratedKeys()
  ↓
productId = 8
  ↓
操作履歴のproduct_idへ設定
```

---

## 25. 商品更新

SQL：

```sql
UPDATE products
SET name = ?, price = ?, stock = ?
WHERE id = ?
```

このSQLでは、1行の全商品情報を更新します。

```text
name
price
stock
```

を新しい値へ変更し、

```text
id
```

で更新対象を特定します。

`WHERE id = ?`を忘れると、全商品を更新する危険があります。

```sql
UPDATE products
SET price = 100;
```

これは全行の価格を100へします。

UPDATEとDELETEでは、WHERE句を特に注意して確認します。

---

## 26. 商品削除

SQL：

```sql
DELETE FROM products
WHERE id = ?
```

削除対象は主キーで1件に絞ります。

```java
statement.setInt(1, id);
```

戻り値が0なら、対象商品は既に存在しません。

---

## 27. ユーザー検索はログインIDだけをSQL条件にする

現在の認証SQL：

```sql
SELECT id, login_id, display_name, password_hash
FROM app_users
WHERE login_id = ?
```

パスワードはSQL条件へ入れません。

古い平文方式：

```sql
WHERE login_id = ?
AND password = ?
```

現在のハッシュ方式：

```text
1. login_idでユーザーを取得
2. password_hashをJavaへ読む
3. PasswordUtil.verify()で照合
```

ハッシュは同じ生パスワード文字列と直接比較できないため、JavaでPBKDF2計算を行います。

---

## 28. トランザクション制御

商品登録時には、2つのテーブルを変更します。

```text
① productsへ商品をINSERT
② product_operation_logsへ履歴をINSERT
```

片方だけ成功すると不整合です。

```text
商品は登録された
しかし操作履歴がない
```

そこで同じConnectionでトランザクションを開始します。

```java
connection.setAutoCommit(false);
```

### 28.1 正常時

```text
商品INSERT成功
  ↓
履歴INSERT成功
  ↓
commit()
  ↓
両方を確定
```

### 28.2 失敗時

```text
商品INSERT成功
  ↓
履歴INSERT失敗
  ↓
rollback()
  ↓
商品INSERTも取り消す
```

---

## 29. rollbackを実際に検証した方法

操作履歴の`operation`列は`NOT NULL`です。

通常：

```java
logStatement.setString(2, "CREATE");
```

検証時だけ、一時的に次へ変更しました。

```java
logStatement.setString(2, null);
```

結果：

```text
productsへのINSERT
  ↓ 一時的には実行
product_operation_logsへのINSERT
  ↓ NOT NULL違反
catch
  ↓ rollback()
```

確認SQL：

```sql
SELECT id, name
FROM products
WHERE name = 'ロールバック確認';
```

結果：

```text
0行
```

操作履歴も増えていませんでした。

つまり、最初の商品INSERTまで取り消されたことを確認できました。

検証後は、必ず次へ戻しました。

```java
logStatement.setString(2, "CREATE");
```

---

## 30. `try-with-resources`

DAOでは次の形を使います。

```java
try (
    Connection connection = getConnection();
    PreparedStatement statement =
            connection.prepareStatement(sql);
    ResultSet resultSet =
            statement.executeQuery()
) {
    // 処理
}
```

ブロック終了時、自動的に閉じられます。

```text
ResultSet.close()
PreparedStatement.close()
Connection.close()
```

例外が起きても閉じられるため、接続やSQL実行資源の閉じ忘れを防げます。

トランザクションのConnectionは、commitまたはrollback後に外側のtry-with-resourcesで閉じられます。

---

## 31. 例外がServletまで伝わる流れ

DAOメソッドは`SQLException`を投げます。

```java
public List<Product> findAll()
        throws SQLException
```

Servletで捕捉します。

```java
catch (SQLException e) {
    throw new ServletException(
        "商品一覧の取得に失敗しました", e);
}
```

流れ：

```text
PostgreSQL・JDBCで失敗
  ↓ SQLException
DAO
  ↓ throws
Servlet
  ↓ ServletExceptionへ包む
Tomcat
  ↓ 500エラー
```

元の`SQLException`を原因として渡しているため、Tomcatのログで根本原因を追跡できます。

---

## 32. 現在のDB設計でまだ行っていないこと

このアプリは学習用の小規模構成です。次は未実施または簡略化されています。

### 32.1 接続情報の外部化

現在はDAO内に次があります。

```java
URL
USER
PASSWORD
```

GitHub版は`YOUR_DB_PASSWORD`ですが、本番では環境変数や設定ファイル、DataSourceなどへ分離します。

### 32.2 接続プール

現在はDAO呼び出しごとに`DriverManager.getConnection()`します。

実務では、TomcatのDataSourceなどで接続を再利用することがあります。

### 32.3 インデックス設計

主キーとUNIQUE制約に伴うもの以外、追加のインデックス設計はしていません。

データ量が増えた場合、検索条件や結合条件を見て検討します。

### 32.4 更新・削除操作の履歴

現在の操作履歴は、商品登録時の`CREATE`のみです。

編集や削除についても残すなら、`UPDATE`、`DELETE`の履歴設計が必要です。

### 32.5 Service層

現在はServletがDAOを直接呼びます。

```text
Servlet → DAO
```

規模が大きくなれば、業務ルールとトランザクションをServiceへ分ける設計もあります。

```text
Servlet → Service → DAO
```

ただし、現在の小規模なMVC学習用アプリでは必須ではありません。

---

## 33. 再現用SQLをプロジェクトへ残す理由

現在、PostgreSQLのテーブルはPC内に存在しますが、Javaプロジェクトだけを別PCへ持っていってもDBは再現されません。

```text
GitHubのJavaコード：ある
別PCのPostgreSQLテーブル：ない
```

そのため、今後次を追加します。

```text
database/
├─ schema.sql
├─ sample-data.sql
└─ verification.sql
```

### `schema.sql`

テーブル・制約を作るSQL。

### `sample-data.sql`

りんご、みかん、管理者ユーザーなど、動作確認用データ。

### `verification.sql`

テーブル内容や操作履歴を確認するSELECT。

DBもコードと同じように、再現手順をGitHubへ残す必要があります。

---

## 34. よく使う確認SQL

### 商品一覧

```sql
SELECT id, name, price, stock
FROM products
ORDER BY id;
```

### 最新の商品

```sql
SELECT id, name, price, stock
FROM products
ORDER BY id DESC
LIMIT 3;
```

### ユーザー確認

```sql
SELECT id, login_id, display_name, password_hash
FROM app_users;
```

### 操作履歴

```sql
SELECT
    id,
    product_id,
    operation,
    operated_by,
    operated_at
FROM product_operation_logs
ORDER BY id DESC;
```

### ユーザー名も合わせて表示

```sql
SELECT
    l.id,
    l.product_id,
    l.operation,
    u.login_id,
    l.operated_at
FROM product_operation_logs l
JOIN app_users u
    ON l.operated_by = u.id
ORDER BY l.id DESC;
```

---

## 35. JDBC処理を読む順番

DAOのコードを読み返すときは、次の順で見ると理解しやすいです。

```text
1. SQL文字列を見る
2. Connectionをどこで作るか見る
3. PreparedStatementを作る
4. ?へ何番目の値を設定するか見る
5. executeQuery / executeUpdateを見る
6. ResultSetまたは更新件数を見る
7. Product・Userへどう変換するか見る
8. try-with-resourcesで何が閉じるか見る
9. トランザクションならcommit / rollbackを見る
```

---

## 36. この章を読み終えた時点で説明できること

- PostgreSQLサーバー、データベース、テーブルの違い
- `products`、`app_users`、`product_operation_logs`の役割
- 主キー、外部キー、UNIQUE、NOT NULL、CHECK、DEFAULTの意味
- IDENTITYが自動採番し、欠番が問題ではない理由
- `ON DELETE SET NULL`で商品削除後も履歴を残す仕組み
- Javaの`Product`・`User`とDB列の対応
- DAOがSQLとJavaオブジェクトの変換を担当すること
- JDBC APIとPostgreSQL JDBC Driverの関係
- PreparedStatementの`?`とパラメータ番号
- SQLインジェクションを防ぐ基本
- `executeQuery()`と`executeUpdate()`の違い
- `ResultSet.next()`が必要な理由
- INSERT後の自動発行IDを`getGeneratedKeys()`で取得する方法
- 同じConnectionでトランザクションを行う理由
- commitとrollbackを実際にどう検証したか
- DB再現用SQLをGitHubへ残す必要性

# 在庫管理Webアプリ学習プロジェクト

Java 8、Servlet、JSP、JDBC、PostgreSQLを使用して作成した、在庫管理Webアプリケーションです。

このプロジェクトは、会社研修で扱うWebアプリケーション開発の内容を復習し、後から教科書として読み返すことを目的としています。

## 学習できる内容

* HTTPのGET・POST
* Apache Tomcat
* Servlet
* JSP
* JDBC
* PostgreSQL
* MVCモデル
* DAOパターン
* ログイン認証
* HttpSession
* Filter
* CRUD処理
* PreparedStatement
* トランザクション制御
* SQLインジェクション対策
* XSS対策
* CSRF対策
* パスワードのハッシュ化

## 使用技術

| 種類         | 使用技術            |
| ---------- | --------------- |
| Java       | Java 8          |
| Webサーバー    | Apache Tomcat 9 |
| View       | JSP / JSTL      |
| Controller | Servlet         |
| データベース     | PostgreSQL      |
| DB接続       | JDBC            |
| ビルド管理      | Maven           |
| IDE        | Eclipse         |

## 主な機能

* ログイン
* ログアウト
* 商品一覧表示
* 商品登録
* 商品編集
* 商品削除
* 未ログインアクセスの防止
* 商品登録時の操作履歴保存

## アプリケーション構成

```text
ブラウザ
  ↓ HTTP
Filter
  ↓
Servlet（Controller）
  ↓
DAO・Model
  ↓ JDBC
PostgreSQL
  ↓
Servlet
  ↓
JSP（View）
  ↓ HTML
ブラウザ
```

## MVCの対応

| MVC        | このプロジェクトでの役割     |
| ---------- | ---------------- |
| Model      | Product、User、DAO |
| View       | JSP              |
| Controller | Servlet          |

## ディレクトリ構成

```text
src/main/java
├─ controller
│  ├─ LoginServlet
│  ├─ LogoutServlet
│  ├─ ProductListServlet
│  ├─ ProductCreateServlet
│  ├─ ProductEditServlet
│  └─ ProductDeleteServlet
├─ dao
│  ├─ ProductDAO
│  └─ UserDAO
├─ filter
│  ├─ LoginFilter
│  └─ CsrfFilter
├─ model
│  ├─ Product
│  └─ User
└─ security
   └─ PasswordUtil

src/main/webapp
├─ index.jsp
└─ WEB-INF/jsp
   ├─ login.jsp
   ├─ product-list.jsp
   ├─ product-form.jsp
   └─ product-edit.jsp
```

## セキュリティ対策

| 対象          | 対策                 |
| ----------- | ------------------ |
| SQLインジェクション | PreparedStatement  |
| 未ログインアクセス   | LoginFilter        |
| セッション固定攻撃   | ログイン時に古いセッションを破棄   |
| XSS         | c:out、fn:escapeXml |
| CSRF        | CSRFトークン           |
| パスワード漏えい    | PBKDF2によるハッシュ保存    |

## トランザクション

商品登録時に、以下の2処理を一つのトランザクションとして実行します。

```text
productsへ商品をINSERT
+
product_operation_logsへ履歴をINSERT
```

両方成功した場合は`commit()`し、途中で失敗した場合は`rollback()`します。

## 注意

データベースの本物のパスワードや秘密情報は、GitHubへ登録しないでください。

現在、DAO内のパスワードは次のプレースホルダーになっています。

```java
private static final String PASSWORD = "YOUR_DB_PASSWORD";
```

## 今後追加するドキュメント

* 環境構築と初期設定
* Webアプリ全体の処理の流れ
* データベース設計
* Servlet・JSP・JDBCの詳細
* トランザクションの検証
* セキュリティの検証
* エラーとトラブルシューティング

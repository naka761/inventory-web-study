# 在庫管理Webアプリ学習プロジェクト

Java 8、Servlet、JSP、JDBC、PostgreSQLを使って作成した、学習用の在庫管理Webアプリケーションです。

会社研修で扱うJava Web開発を自分の手で再現し、数か月後でも「何をしたか」だけでなく、**なぜその設定・実装が必要なのか**を読み返せる教科書として整理しています。

---

## このプロジェクトで学べること

- HTTPのGET・POST
- Apache Tomcat
- Servlet / JSP / JSTL
- MVCモデル
- DAOパターン
- JDBC / PostgreSQL
- Maven / WAR
- CRUD処理
- ログイン認証
- HttpSession
- Filter
- PreparedStatement
- トランザクション
- SQLインジェクション対策
- XSS対策
- CSRF対策
- PBKDF2によるパスワードハッシュ化
- 環境変数によるDB接続設定
- Git / GitHubによる変更履歴管理

---

## 使用技術

| 種類 | 使用技術 |
| --- | --- |
| Java | Java 8 |
| IDE | Eclipse |
| Webサーバー / Servletコンテナ | Apache Tomcat 9 |
| Servlet API | `javax.servlet-api 4.0.1` |
| View | JSP / JSTL |
| Controller | Servlet |
| Model / DBアクセス | JavaBean / DAO |
| データベース | PostgreSQL 18 |
| DB接続 | JDBC / PostgreSQL JDBC Driver |
| ビルド管理 | Maven |
| 成果物 | WAR |

---

## 主な機能

- ログイン
- ログアウト
- 商品一覧表示
- 商品登録
- 商品編集
- 商品削除
- 未ログインアクセスの遮断
- 商品登録時の操作履歴保存
- 商品と履歴のトランザクション制御
- CSRFトークン検証
- PBKDF2によるパスワード照合
- 商品名100文字制限
- 入力エラー時のフォーム値保持

---

## アプリケーション全体の流れ

```text
ブラウザ
  ↓ HTTPリクエスト
Apache Tomcat 9
  ↓
Filter
  ↓
Servlet（Controller）
  ↓
DAO・Model
  ↓ JDBC
PostgreSQL
  ↑
Servlet
  ↓ request属性
JSP（View）
  ↓ HTML
ブラウザ
```

### MVCとの対応

| MVC | このプロジェクト |
| --- | --- |
| Model | `Product`、`User`、`ProductDAO`、`UserDAO` |
| View | JSP |
| Controller | Servlet |

Filterは、ログイン確認やCSRF検査など、Servletへ到達する前の共通処理を担当します。

---

## ディレクトリ構成

```text
inventory-web-study
├─ README.md
├─ docs
│  ├─ 01_環境構築と初期設定.md
│  ├─ 02_Webアプリ全体の処理の流れ.md
│  ├─ 03_データベース設計とJDBC.md
│  ├─ 04_トランザクションとセキュリティ検証.md
│  ├─ 05_エラーとトラブルシューティング.md
│  ├─ 06_コードレビューと改善計画.md
│  └─ 07_DB接続設定の外部化.md
├─ database
│  ├─ schema.sql
│  ├─ sample-data.sql
│  └─ verification.sql
├─ src
│  └─ main
│     ├─ java
│     │  ├─ controller
│     │  ├─ dao
│     │  │  ├─ DatabaseConnectionFactory.java
│     │  │  ├─ ProductDAO.java
│     │  │  └─ UserDAO.java
│     │  ├─ filter
│     │  ├─ model
│     │  └─ security
│     └─ webapp
│        ├─ index.jsp
│        └─ WEB-INF
│           └─ jsp
└─ pom.xml
```

---

## 教科書ドキュメント

初めて読む場合は、上から順番に進む想定です。

1. [環境構築と初期設定](docs/01_環境構築と初期設定.md)  
   Java 8、Eclipse、Tomcat、Maven、PostgreSQLの役割と設定を整理します。

2. [Webアプリ全体の処理の流れ](docs/02_Webアプリ全体の処理の流れ.md)  
   ブラウザからFilter、Servlet、DAO、JSPまでのデータ移動を追います。

3. [データベース設計とJDBC](docs/03_データベース設計とJDBC.md)  
   テーブル、制約、PreparedStatement、ResultSet、トランザクションを説明します。

4. [トランザクションとセキュリティ検証](docs/04_トランザクションとセキュリティ検証.md)  
   commit、rollback、SQLインジェクション、XSS、CSRF、PBKDF2を実際の検証履歴から確認します。

5. [エラーとトラブルシューティング](docs/05_エラーとトラブルシューティング.md)  
   PKIX、文字化け、DB停止、Javaバージョン、Git認証など、実際に遭遇した問題を整理します。

6. [コードレビューと改善計画](docs/06_コードレビューと改善計画.md)  
   不具合、改善案、学習用として現状維持でよい部分を分けて整理します。

7. [DB接続設定の外部化](docs/07_DB接続設定の外部化.md)  
   本物のDBパスワードをJavaソースへ書かず、環境変数から読み取る構成を説明します。

---

## データベース用ファイル

| ファイル | 役割 |
| --- | --- |
| [`database/schema.sql`](database/schema.sql) | 3テーブルと制約を作成する |
| [`database/sample-data.sql`](database/sample-data.sql) | 学習用ユーザーと商品を登録する |
| [`database/verification.sql`](database/verification.sql) | 表構造、制約、データ、不整合を確認する |

### 学習用ログイン

`sample-data.sql`には、ローカル学習用の公開アカウントが含まれています。

```text
login_id: training-admin
password: training-only-2026
```

この認証情報は、インターネットへ公開する環境や実務環境では使用しないでください。

---

## ローカルで動かすまでの流れ

### 1. 必要な環境を用意する

- Temurin JDK 8
- Eclipse
- Apache Tomcat 9
- PostgreSQL 18

詳細は[環境構築と初期設定](docs/01_環境構築と初期設定.md)を参照してください。

### 2. PostgreSQLへテーブルを作る

リポジトリのルートディレクトリから実行する例です。

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -h localhost `
  -p 5432 `
  -U postgres `
  -d postgres `
  -f database/schema.sql
```

### 3. サンプルデータを登録する

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -h localhost `
  -p 5432 `
  -U postgres `
  -d postgres `
  -f database/sample-data.sql
```

### 4. DB状態を確認する

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" `
  -h localhost `
  -p 5432 `
  -U postgres `
  -d postgres `
  -f database/verification.sql
```

---

## DB接続用の環境変数

Javaソースへ本物のDBパスワードを書かず、環境変数から読み取ります。

| 環境変数 | 必須 | 未設定時 |
| --- | --- | --- |
| `INVENTORY_DB_PASSWORD` | はい | DB接続前にエラー |
| `INVENTORY_DB_URL` | いいえ | `jdbc:postgresql://localhost:5432/postgres` |
| `INVENTORY_DB_USER` | いいえ | `postgres` |

標準的なローカル環境では、`INVENTORY_DB_PASSWORD`だけ設定します。

### Windowsで設定する

1. Windows検索で「環境変数」と入力する
2. 「システム環境変数の編集」を開く
3. 「環境変数」を押す
4. ユーザー環境変数の「新規」を押す
5. 次を登録する

```text
変数名：
INVENTORY_DB_PASSWORD

変数値：
自分のPostgreSQLパスワード
```

必要な場合だけ、次も設定します。

```text
INVENTORY_DB_URL
INVENTORY_DB_USER
```

設定後は、Eclipseを完全に終了して再起動してください。Eclipseから起動するTomcatは、Eclipse起動時の環境変数を引き継ぎます。

詳細は[DB接続設定の外部化](docs/07_DB接続設定の外部化.md)を参照してください。

---

## Eclipseから起動する

```text
EclipseへMavenプロジェクトとしてImport
  ↓
JRE System LibraryをJavaSE-1.8にする
  ↓
Tomcat 9 Runtimeを登録
  ↓
プロジェクトをTomcatへ追加
  ↓
Publish
  ↓
Tomcatを起動
```

ブラウザ：

```text
http://localhost:8080/inventory-web/
```

---

## セキュリティ対策

| 対象 | 対策 |
| --- | --- |
| SQLインジェクション | `PreparedStatement` |
| 未ログインアクセス | `LoginFilter` |
| セッション固定攻撃 | ログイン成功時に古いSessionを破棄 |
| XSS | `c:out`、`fn:escapeXml` |
| CSRF | SessionとフォームのCSRFトークンを比較 |
| パスワード漏えい | PBKDF2によるハッシュ保存 |
| 複数DB更新の不整合 | `commit()` / `rollback()` |
| 不正な価格・在庫数 | Servletの入力チェックとDBの`CHECK`制約 |
| DBパスワードの誤公開 | 環境変数から読み取り |

---

## トランザクション

商品登録時、次の2処理を同じConnectionで実行します。

```text
productsへ商品をINSERT
+
product_operation_logsへ操作履歴をINSERT
```

両方成功した場合だけ`commit()`し、途中で失敗した場合は`rollback()`します。

異常系の検証では、履歴登録を意図的に失敗させ、商品INSERTも取り消されることを確認しました。

---

## 注意事項

- 本物のDBパスワード、APIキー、トークンをGitHubへ登録しない
- 環境変数名はGitHubへ載せてよいが、環境変数の値は載せない
- `sample-data.sql`のログイン情報は学習用
- 本プロジェクトはローカル学習用であり、そのまま実運用へ使用しない
- Java 8、Tomcat 9、`javax.servlet`の組み合わせを崩さない
- 検証用にコードを壊した場合は、正常状態へ戻してからcommitする

---

## 今後の改善候補

- PasswordUtilの単体テスト
- 入力検証の自動テスト
- DAO・トランザクションの統合テスト
- Connection Poolの利用
- Service層の追加
- 更新・削除操作も操作履歴へ保存
- 管理者と一般利用者の権限分離
- GitHub Actionsによるビルド確認

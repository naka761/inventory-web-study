# 02. Webアプリ全体の処理の流れ

この章では、`inventory-web-study`でブラウザの操作がどのJavaクラスへ届き、どのデータがどこへ移動し、最後にどのJSPからHTMLが返るのかを追跡します。

Servlet、DAO、JSPを個別に覚えるだけでは、Webアプリ全体の動きは見えにくいです。そこで、この章では次の疑問に答えます。

- ブラウザはJavaのメソッドを直接呼んでいるのか
- Filter、Servlet、DAO、JSPはどの順で動くのか
- `getParameter()`、`setAttribute()`、Sessionは何が違うのか
- `forward()`と`sendRedirect()`は何が違うのか
- `ResultSet`がどのように`List<Product>`へ変わるのか
- ログイン状態は、次のHTTPリクエストへどう引き継がれるのか
- 登録、編集、削除でGETとPOSTをどう使い分けているのか

---

## 1. Webアプリは「1回の処理」ではなく、何度もHTTP通信する

ブラウザで商品を登録するとき、最初から最後まで1本のJava処理が動き続けているわけではありません。

実際には、複数の独立したHTTPリクエストが発生します。

```text
① 登録画面を開く
GET /inventory-web/products/create

② フォームを送信する
POST /inventory-web/products/create

③ 登録成功後、一覧へ移動する
GET /inventory-web/products
```

それぞれの通信ごとに、Tomcatは新しい`HttpServletRequest`と`HttpServletResponse`を用意します。

```text
1回目のrequest：登録画面をください
2回目のrequest：この商品を登録してください
3回目のrequest：最新の商品一覧をください
```

HTTPは基本的に、リクエストとレスポンスが1組になったら終了します。

```text
ブラウザ ──リクエスト──> Tomcat
ブラウザ <─レスポンス─── Tomcat
```

次のアクセスは、また別のリクエストです。

---

## 2. このプロジェクトの役割分担

### 2.1 全体像

```text
ブラウザ
  ↓ HTTPリクエスト
Tomcat
  ↓
Filter
  ↓ 通過を許可
Servlet（Controller）
  ↓
DAO・Model
  ↓ JDBC
PostgreSQL
  ↑ 検索結果・更新件数
DAO・Model
  ↑
Servlet
  ↓ requestへ表示データを保存
JSP（View）
  ↓ HTMLを生成
Tomcat
  ↓ HTTPレスポンス
ブラウザ
```

### 2.2 各部品の責任

| 部品 | このプロジェクトでの責任 |
| --- | --- |
| ブラウザ | URLへアクセスし、フォームを送信し、HTMLを表示する |
| Tomcat | HTTPを受け取り、Filter・Servlet・JSPを実行する |
| Filter | Servletへ到達する前に、共通の検査を行う |
| Servlet | 入力を受け取り、処理を選び、DAOやJSPを呼ぶ |
| DAO | JDBCを使ってSQLを実行する |
| Model | 商品やユーザーをJavaオブジェクトとして表す |
| JSP | Servletから渡されたデータをHTMLとして表示する |
| PostgreSQL | テーブルへデータを保存し、SQLへ結果を返す |

### 2.3 MVCとの対応

| MVC | このプロジェクト |
| --- | --- |
| Model | `Product`、`User`、`ProductDAO`、`UserDAO` |
| View | `WEB-INF/jsp`配下のJSP |
| Controller | 各Servlet |

FilterはMVCの3役へ無理に当てはめず、Servletの前で動く**横断的な共通処理**と考えます。

---

## 3. URLと担当クラスの対応表

Tomcatは`@WebServlet`の値を見て、URLに対応するServletを探します。

| HTTP | URL | 担当 | 主な処理 |
| --- | --- | --- | --- |
| GET | `/login` | `LoginServlet#doGet` | ログイン画面表示 |
| POST | `/login` | `LoginServlet#doPost` | ID・パスワード認証 |
| POST | `/logout` | `LogoutServlet#doPost` | Session破棄 |
| GET | `/products` | `ProductListServlet#doGet` | 商品一覧取得・表示 |
| GET | `/products/create` | `ProductCreateServlet#doGet` | 商品登録画面表示 |
| POST | `/products/create` | `ProductCreateServlet#doPost` | 商品・操作履歴登録 |
| GET | `/products/edit?id=1` | `ProductEditServlet#doGet` | 編集対象取得・画面表示 |
| POST | `/products/edit` | `ProductEditServlet#doPost` | 商品更新 |
| POST | `/products/delete` | `ProductDeleteServlet#doPost` | 商品削除 |

`@WebServlet("/products")`は、Javaのクラス名をURLへ出しているわけではありません。

```text
URL：/products
  ↓ @WebServletの対応付け
クラス：ProductListServlet
```

ブラウザは`ProductListServlet`というJavaクラス名を知る必要がありません。

---

## 4. FilterはServletより前に動く

このプロジェクトには、主に2つのFilterがあります。

| Filter | 対象 | 役割 |
| --- | --- | --- |
| `CsrfFilter` | `/*` | 文字コード設定、CSRFトークン生成、POST時のトークン検査 |
| `LoginFilter` | `/products`、`/products/*` | 商品関連URLへの未ログインアクセスを遮断 |

商品一覧へアクセスすると、概念上は次のようになります。

```text
GET /products
  ↓
対象となるFilter
  ↓
ProductListServlet
```

Filterで次を呼ぶと、その先のFilterまたはServletへ進みます。

```java
chain.doFilter(request, response);
```

呼ばなければ、そこで処理を止められます。

```text
ログイン済み
  ↓ chain.doFilter()
Servletへ進む

未ログイン
  ↓ sendRedirect("/login")
Servletへ進まない
```

> [!NOTE]
> 複数のアノテーションFilterの厳密な実行順を、ファイルの並びだけで決めつけないこと。  
> このプロジェクトで重要なのは、該当するFilterがServletより前に動き、通過を許可された場合だけServletへ進むことです。

---

## 5. 文字コード設定もFilterで先に行う

`CsrfFilter`では、パラメータを読む前に次を実行しています。

```java
httpRequest.setCharacterEncoding("UTF-8");
```

その後で、CSRFトークンを取得します。

```java
String requestToken =
        httpRequest.getParameter("csrfToken");
```

順序が重要です。

```text
正しい順序

setCharacterEncoding("UTF-8")
  ↓
getParameter()
```

`getParameter()`を先に呼ぶと、Tomcatがフォーム本文を先に解析してしまいます。

```text
getParameter()
  ↓ フォーム全体を解析済み
setCharacterEncoding("UTF-8")
  ↓ 手遅れ
```

この問題は、商品名「トランザクション確認」が文字化けしてDBへ保存された検証で実際に起きました。

---

## 6. ログイン画面を開く流れ

ブラウザで次へアクセスします。

```text
GET /inventory-web/login
```

### 6.1 処理の流れ

```text
ブラウザ
  ↓ GET /login
Tomcat
  ↓
CsrfFilter
  ├─ Sessionを取得または作成
  └─ csrfTokenがなければ生成してSessionへ保存
  ↓
LoginServlet#doGet
  ├─ ログイン済みか確認
  ├─ ログイン済み → /productsへredirect
  └─ 未ログイン → login.jspへforward
  ↓
login.jsp
  ↓ HTML
ブラウザ
```

### 6.2 `getSession(false)`の意味

`LoginServlet#doGet`では次を使います。

```java
HttpSession session = request.getSession(false);
```

`false`の意味は、

> Sessionが既にあれば返す。なければ新しく作らず`null`を返す

です。

```text
getSession(false)
  ├─ 既存Sessionあり → そのSession
  └─ 既存Sessionなし → null
```

ログイン済み判定：

```java
session != null
&& session.getAttribute("loginUser") != null
```

`loginUser`があれば、ログイン画面を再表示せず商品一覧へ移動します。

---

## 7. JSPのログインフォームは何を送るのか

`login.jsp`には、次のようなフォームがあります。

```jsp
<form action="${pageContext.request.contextPath}/login"
      method="post">
```

送信先は次です。

```text
POST /inventory-web/login
```

フォーム内の`name`属性が、Servletで受け取るパラメータ名になります。

```jsp
<input name="loginId">
<input name="password">
<input name="csrfToken">
```

Servlet側：

```java
request.getParameter("loginId");
request.getParameter("password");
```

対応関係：

```text
HTMLのname="loginId"
  ↓ HTTPフォームデータ
getParameter("loginId")
```

Javaの変数名が自動的にブラウザへ伝わるわけではありません。HTMLの`name`と、Javaの`getParameter()`に同じ文字列を書くことで対応させています。

---

## 8. ログインPOSTの全体フロー

ログイン画面で次を入力したとします。

```text
ログインID：admin
パスワード：admin123
```

### 8.1 ブラウザから届く内容

概念上、次のようなフォームデータが送られます。

```text
loginId=admin
password=admin123
csrfToken=ランダムな文字列
```

### 8.2 FilterでCSRFを確認

```text
POST /login
  ↓
CsrfFilter
  ├─ Session内のcsrfToken
  ├─ フォーム内のcsrfToken
  └─ 一致しなければ403
```

正規の`login.jsp`から送れば、Sessionとフォームに同じトークンがあります。

### 8.3 `LoginServlet#doPost`

Servletは次を取得します。

```java
String loginId = request.getParameter("loginId");
String password = request.getParameter("password");
```

未入力なら、DAOを呼ばずエラーメッセージをrequestへ入れます。

```java
request.setAttribute(
        "errorMessage",
        "ログインIDとパスワードを入力してください。");
```

その後、同じリクエストのまま`login.jsp`へforwardします。

### 8.4 `UserDAO`でユーザーを検索

```text
LoginServlet
  ↓ loginIdとpassword
UserDAO.findByLoginIdAndPassword()
```

実行するSQL：

```sql
SELECT id, login_id, display_name, password_hash
FROM app_users
WHERE login_id = ?
```

重要なのは、SQLで生パスワードを比較していないことです。

```text
SQL：login_idでユーザー行を探す
Java：入力パスワードとpassword_hashを照合する
```

`PreparedStatement`の`?`へログインIDを設定します。

```java
statement.setString(1, loginId);
```

### 8.5 `ResultSet`からユーザー行を読む

```java
if (!resultSet.next()) {
    return null;
}
```

`ResultSet`は、最初から1行目を指していません。`next()`で次の行へ移動します。

```text
SELECT直後
  ↓ カーソルは行の手前
resultSet.next()
  ↓ 1行あればtrue、なければfalse
```

ユーザーが見つからなければ`null`です。

### 8.6 パスワードをPBKDF2で照合

DBから読み出す値：

```java
String passwordHash =
        resultSet.getString("password_hash");
```

照合：

```java
PasswordUtil.verify(password, passwordHash)
```

```text
入力された生パスワード
  ↓ 保存済みsalt・反復回数を使ってPBKDF2
計算されたハッシュ
  ↓
DBのハッシュと比較
```

一致しなければ`null`、一致すれば次のような`User`を返します。

```java
new User(
    id,
    loginId,
    displayName
)
```

`User`へ生パスワードは保存しません。

---

## 9. ログイン成功時にSessionへ何を保存するのか

`LoginServlet`へ`User`が返ると、古いSessionを破棄します。

```java
HttpSession oldSession = request.getSession(false);

if (oldSession != null) {
    oldSession.invalidate();
}
```

その後、新しいSessionを作ります。

```java
HttpSession newSession = request.getSession(true);
```

`true`は、

> Sessionがなければ新しく作る

という意味です。

ログインユーザーを保存します。

```java
newSession.setAttribute("loginUser", user);
```

Session内のイメージ：

```text
Session
├─ loginUser
│  └─ User(
│       id=1,
│       loginId="admin",
│       displayName="管理者"
│     )
└─ csrfToken
   └─ ランダムな文字列
```

さらに、30分間操作がなければSessionを終了する設定です。

```java
newSession.setMaxInactiveInterval(30 * 60);
```

---

## 10. HTTPは独立しているのに、なぜログイン状態が続くのか

HTTPリクエスト自体は毎回別です。

```text
POST /login
GET /products
POST /products/create
```

それでも同じ利用者だと分かるのは、通常、TomcatがSession IDをCookieでブラウザとやり取りするためです。

概念図：

```text
ログイン成功
  ↓
TomcatがSessionを作る
  ↓
ブラウザへSession IDをCookieで渡す
  ↓
次のリクエストでブラウザがCookieを送る
  ↓
Tomcatが対応するSessionを見つける
```

ブラウザに`User`オブジェクト全体を保存しているわけではありません。

```text
ブラウザ：Sessionを識別するID
Tomcat側：loginUserなどのSessionデータ
```

---

## 11. `sendRedirect()`で商品一覧へ移る

ログイン成功後：

```java
response.sendRedirect(
        request.getContextPath() + "/products");
```

ブラウザへ、

> 次は`/inventory-web/products`へアクセスしてください

と返します。

```text
POST /login
  ↓ 302 Redirect
ブラウザ
  ↓ 新しいGET /products
```

ここで、ログインPOSTの`request`は終了します。

---

## 12. 商品一覧表示の流れ

### 12.1 全体

```text
ブラウザ
  ↓ GET /products
CsrfFilter
  ↓
LoginFilter
  ├─ SessionにloginUserがあるか
  └─ なければ/loginへredirect
  ↓
ProductListServlet#doGet
  ↓
ProductDAO.findAll()
  ↓
PostgreSQLでSELECT
  ↓
List<Product>
  ↓
request.setAttribute("products", products)
  ↓
product-list.jspへforward
  ↓
HTML
```

### 12.2 `ProductDAO.findAll()`

SQL：

```sql
SELECT id, name, price, stock
FROM products
ORDER BY id
```

最初に空のリストを作ります。

```java
List<Product> products =
        new ArrayList<Product>();
```

`ResultSet`を1行ずつ進めます。

```java
while (resultSet.next()) {
```

現在行の列から`Product`を作ります。

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

変換のイメージ：

```text
ResultSetの1行目
id=1, name=りんご, price=150, stock=30
  ↓
new Product(1, "りんご", 150, 30)
  ↓
Listへ追加

ResultSetの2行目
  ↓
次のProductをListへ追加
```

最後に、Servletへ`List<Product>`を返します。

### 12.3 なぜResultSetをそのままJSPへ渡さないのか

`ResultSet`はDB接続中の検索結果を読むためのJDBCオブジェクトです。DAOを抜けるときにはConnectionなどを閉じます。

そこで、必要な値を通常のJavaオブジェクトへ移します。

```text
ResultSet
  ↓ DAOで変換
Product
  ↓ 複数件を格納
List<Product>
```

これにより、JSPはJDBCを知らずに商品だけを表示できます。

---

## 13. `request.setAttribute()`でJSPへ渡す

Servlet：

```java
request.setAttribute("products", products);
```

これは、現在のrequestへ、

```text
名前："products"
値：List<Product>
```

を保存します。

その後、同じrequestをJSPへ渡します。

```java
request.getRequestDispatcher(
        "/WEB-INF/jsp/product-list.jsp")
       .forward(request, response);
```

JSP側：

```jsp
<c:forEach var="product" items="${products}">
```

`${products}`は、requestへ保存した`"products"`を探します。

---

## 14. JSPの`${product.name}`は何をしているのか

JSPのEL式：

```jsp
${product.name}
```

概念上、JavaBeanのgetterを呼びます。

```java
product.getName()
```

同様に、

```jsp
${product.id}
${product.price}
${product.stock}
```

は概ね次に対応します。

```java
product.getId()
product.getPrice()
product.getStock()
```

フィールドを直接読んでいるわけではありません。

商品名は次のように出力しています。

```jsp
<c:out value="${product.name}" />
```

`c:out`は`<`や`>`などをHTMLとして実行させず、文字として表示するために使います。

---

## 15. request parameter、request attribute、Sessionの違い

この3つは似た名前ですが、出所と寿命が違います。

| 種類 | 誰が値を入れるか | 例 | 主な寿命 |
| --- | --- | --- | --- |
| request parameter | ブラウザのURLやフォーム | `name`, `price`, `id` | そのリクエスト |
| request attribute | Servletなどのサーバー側コード | `products`, `errorMessage` | そのリクエスト |
| Session attribute | ServletやFilter | `loginUser`, `csrfToken` | 複数リクエストをまたぐ |

### 15.1 parameter

```java
request.getParameter("name");
```

ブラウザから届いた文字列を取得します。

### 15.2 request attribute

```java
request.setAttribute("products", products);
```

ServletからJSPへ、Javaオブジェクトを渡すために使います。

### 15.3 Session attribute

```java
session.setAttribute("loginUser", user);
```

ログイン後の別リクエストでも利用したい値を保存します。

---

## 16. `forward()`と`sendRedirect()`の違い

### 16.1 forward

```java
request.getRequestDispatcher("/WEB-INF/jsp/product-list.jsp")
       .forward(request, response);
```

Tomcat内部で、同じrequestとresponseをJSPへ渡します。

```text
ブラウザ
  ↓ 1回のリクエスト
Servlet
  ↓ Tomcat内部
JSP
  ↓
ブラウザ
```

特徴：

- ブラウザとの通信は増えない
- 同じrequest attributeをJSPで読める
- ブラウザのURLはServletのURLのまま
- JSPの表示処理へ向いている

### 16.2 redirect

```java
response.sendRedirect(
        request.getContextPath() + "/products");
```

ブラウザへ別URLへの再アクセスを依頼します。

```text
ブラウザ
  ↓ 1回目
Servlet
  ↓ Redirect
ブラウザ
  ↓ 2回目
別URL
```

特徴：

- 新しいHTTPリクエストになる
- 元のrequest attributeは消える
- ブラウザのURLが変わる
- 登録・更新・削除成功後の移動に向いている

---

## 17. 商品登録画面を開く流れ

```text
GET /products/create
  ↓
LoginFilterでログイン確認
  ↓
ProductCreateServlet#doGet
  ↓
product-form.jspへforward
```

この段階では、まだDBへ商品を登録しません。

GETの仕事は画面を表示することです。

---

## 18. 商品登録POSTの流れ

フォーム：

```text
name=商品名
price=100
stock=10
csrfToken=...
```

全体：

```text
POST /products/create
  ↓
CsrfFilterでトークン検査
  ↓
LoginFilterでログイン確認
  ↓
ProductCreateServlet#doPost
  ├─ getParameter()
  ├─ 数値変換
  ├─ 入力チェック
  ├─ Product作成
  ├─ SessionからloginUser取得
  └─ ProductDAO.insertWithLog()
          ↓
      productsへINSERT
          ↓
      発行された商品IDを取得
          ↓
      product_operation_logsへINSERT
          ↓
      両方成功ならcommit
      失敗ならrollback
  ↓
/productsへredirect
```

### 18.1 すべてのフォーム値は最初は文字列

```java
String priceText = request.getParameter("price");
```

HTMLで`type="number"`でも、HTTPからJavaへ届く値は文字列です。

```text
"100"
  ↓ Integer.parseInt()
100
```

変換できなければ`NumberFormatException`です。

### 18.2 Productを作る

```java
Product product = new Product(
    0,
    name.trim(),
    price,
    stock
);
```

新規登録前は、DBが商品IDを発行するため、ここでは`id=0`を仮に入れています。

### 18.3 操作者をSessionから取得

```java
User loginUser =
        (User) session.getAttribute("loginUser");
```

操作履歴へ「誰が登録したか」を保存するためです。

---

## 19. 登録トランザクションで同じConnectionが必要な理由

`insertWithLog()`では、次の2処理を一つのConnectionで実行します。

```text
同じConnection
├─ productsへINSERT
└─ product_operation_logsへINSERT
```

最初に自動コミットを止めます。

```java
connection.setAutoCommit(false);
```

2つとも成功：

```java
connection.commit();
```

途中で失敗：

```java
connection.rollback();
```

別々のConnectionを使うと、片方だけ確定した後にもう片方を取り消すことができません。

```text
Connection A：商品INSERTを確定済み
Connection B：履歴INSERTに失敗
  ↓
商品だけ残る
```

同じConnectionなら、まとめて取り消せます。

---

## 20. 登録後にredirectする理由（PRG）

登録成功後、JSPへ直接forwardせず商品一覧へredirectします。

```text
POST：商品登録
  ↓ 成功
Redirect
  ↓
GET：商品一覧
```

これはPost/Redirect/Get、略してPRGと呼ばれる形です。

もしPOSTの結果画面をそのまま表示すると、ブラウザの再読み込みで同じPOSTが再送信され、二重登録の原因になります。

```text
POST結果を表示
  ↓ 更新ボタン
同じPOSTを再送信
  ↓
同じ商品がもう一度登録される可能性
```

redirect後は最後の通信がGETなので、更新しても一覧を再取得するだけです。

---

## 21. 入力エラー時はなぜforwardなのか

登録内容に問題がある場合：

```java
request.setAttribute(
        "errorMessage",
        "価格と在庫数は整数で入力してください。");
```

そのまま登録JSPへforwardします。

```text
同じrequest
├─ ブラウザから来たparam.nameなど
└─ Servletが入れたerrorMessage
```

JSPは両方を使って再表示できます。

```jsp
${errorMessage}
${param.name}
${param.price}
${param.stock}
```

redirectすると新しいrequestになるため、これらはそのままでは消えます。

---

## 22. 商品編集画面を開く流れ

一覧のリンク：

```text
/products/edit?id=1
```

`?id=1`はクエリパラメータです。

```java
String idText = request.getParameter("id");
```

全体：

```text
GET /products/edit?id=1
  ↓
ProductEditServlet#doGet
  ↓
idをintへ変換
  ↓
ProductDAO.findById(1)
  ↓
SELECT ... WHERE id = ?
  ↓
Productまたはnull
```

商品があれば：

```java
request.setAttribute("product", product);
```

編集JSPへforwardします。

商品がなければ：

```text
404 Not Found
```

IDが数字でなければ：

```text
400 Bad Request
```

---

## 23. 商品更新POSTの流れ

```text
POST /products/edit
  ↓
id、name、price、stockを取得
  ↓
数値変換・入力チェック
  ↓
Productを作る
  ↓
ProductDAO.update(product)
  ↓
UPDATE products
SET name=?, price=?, stock=?
WHERE id=?
  ↓
更新件数を返す
```

`executeUpdate()`が返す値は、更新された行数です。

```text
1 → 1件更新できた
0 → 該当IDが存在しなかった
```

0件なら404を返します。

成功後は、登録と同じく一覧へredirectします。

---

## 24. 商品削除POSTの流れ

削除は状態を変更するためGETではなくPOSTです。

```text
POST /products/delete
  ↓
CSRF検査
  ↓
ログイン検査
  ↓
ProductDeleteServlet#doPost
  ↓
idをintへ変換
  ↓
ProductDAO.deleteById(id)
  ↓
DELETE FROM products WHERE id = ?
  ↓
削除件数
```

削除件数：

```text
1 → 削除成功
0 → 対象が存在しないので404
```

成功後は一覧へredirectします。

### 24.1 現在のGitHub版で見つかった注意点

`inventory-web-study`の現在の`product-list.jsp`では、ログアウトフォームには`csrfToken`がありますが、**削除フォームには`csrfToken`がありません**。

その状態では、`CsrfFilter`が全POSTにトークンを要求するため、削除ボタンを押すと403になります。

削除フォーム内には次が必要です。

```jsp
<input type="hidden"
       name="csrfToken"
       value="${sessionScope.csrfToken}">
```

これは以前、CSRF検証のために一時的に外して403を確認した箇所です。元の実行用プロジェクトと、GitHubへ複製した学習用プロジェクトで状態がずれた可能性があります。

---

## 25. ログアウトの流れ

商品一覧のログアウトフォームから送信します。

```text
POST /logout
  ↓
CsrfFilterでトークン確認
  ↓
LogoutServlet#doPost
  ↓
既存Sessionを取得
  ↓
session.invalidate()
  ↓
/loginへredirect
```

`invalidate()`は`loginUser`だけでなく、そのSession全体を無効にします。

```text
ログアウト前
Session
├─ loginUser
└─ csrfToken

      ↓ invalidate()

ログアウト後
以前のSessionは無効
```

その後、`/products`へ直接アクセスすると、`LoginFilter`が`loginUser`を見つけられず、ログイン画面へ戻します。

---

## 26. HTTPステータスの使い分け

このプロジェクトでは、状況に応じてTomcatのエラー画面を返します。

| ステータス | 意味 | このプロジェクトの例 |
| ---: | --- | --- |
| 400 | リクエスト形式が不正 | 商品IDが数字ではない |
| 403 | サーバーが処理を拒否 | CSRFトークンなし・不一致 |
| 404 | 対象が存在しない | 編集・削除対象の商品がない |
| 500 | サーバー内部で失敗 | SQL例外をServletExceptionとして送出 |

例：

```java
response.sendError(
        HttpServletResponse.SC_BAD_REQUEST,
        "商品IDが不正です。");
```

ログイン失敗はサーバー故障ではないため、現在はログイン画面へエラーメッセージを表示します。

---

## 27. `try-with-resources`で何が閉じられるのか

DAOでは次の形を使います。

```java
try (
    Connection connection = getConnection();
    PreparedStatement statement =
            connection.prepareStatement(sql);
    ResultSet resultSet =
            statement.executeQuery()
) {
    // 結果を読む
}
```

ブロックを抜けると、後ろから自動でcloseされます。

```text
ResultSet
  ↓ close
PreparedStatement
  ↓ close
Connection
  ↓ close
```

例外が起きても閉じられるため、DB接続の閉じ忘れを防げます。

---

## 28. `executeQuery()`と`executeUpdate()`の違い

| メソッド | 主なSQL | 戻り値 |
| --- | --- | --- |
| `executeQuery()` | SELECT | `ResultSet` |
| `executeUpdate()` | INSERT、UPDATE、DELETE | 変更行数 |

例：

```java
ResultSet resultSet =
        statement.executeQuery();
```

```java
int updatedCount =
        statement.executeUpdate();
```

INSERTで自動発行IDが必要な場合は、更新後に`getGeneratedKeys()`を使います。

---

## 29. なぜJSPを`WEB-INF`へ置くのか

JSPは次の場所にあります。

```text
src/main/webapp/WEB-INF/jsp/
```

`WEB-INF`配下は、ブラウザからURLを直接指定して開けません。

```text
ブラウザ → /WEB-INF/jsp/product-list.jsp
          直接アクセス不可
```

Servletからはforwardできます。

```text
Servlet
  ↓ Tomcat内部
/WEB-INF/jsp/product-list.jsp
```

これにより、DAOを呼ばず、必要なrequest attributeもない状態でJSPだけを直接開かれることを防げます。

---

## 30. ControllerがHTMLを書かず、JSPがSQLを書かない理由

役割を混ぜると、変更時に影響範囲が分かりにくくなります。

悪い混在例：

```text
Servletが巨大なHTML文字列を出力する
JSPでJDBC接続してSELECTする
DAOが画面遷移を決める
```

現在の分担：

```text
Servlet
├─ HTTP入力を受け取る
├─ DAOを呼ぶ
├─ エラーや移動先を決める
└─ JSPへデータを渡す

DAO
├─ SQLを作る
├─ JDBCで実行する
└─ ProductやUserへ変換する

JSP
├─ 渡されたデータを読む
└─ HTMLとして表示する
```

小さいアプリなのでServletがDAOを直接呼んでいます。さらに規模が大きくなれば、ServletとDAOの間にService層を追加する設計もありますが、MVCを成立させるための必須条件ではありません。

---

## 31. このプロジェクトを読むおすすめ順

初めてコードを読み直すときは、次の順が理解しやすいです。

### 商品一覧

```text
1. product-list.jspのリンクやフォームを見る
2. ProductListServletを見る
3. ProductDAO.findAll()を見る
4. Productを見る
5. 再びproduct-list.jspのEL式を見る
```

### ログイン

```text
1. login.jspのformとnameを見る
2. CsrfFilterを見る
3. LoginServlet#doPostを見る
4. UserDAOを見る
5. PasswordUtil.verify()を見る
6. LoginFilterを見る
```

### 商品登録

```text
1. product-form.jspを見る
2. ProductCreateServlet#doPostを見る
3. SessionからloginUserを取る部分を見る
4. ProductDAO.insertWithLog()を見る
5. commitとrollbackを見る
```

---

## 32. 1本の線で全体を説明すると

### ログイン

```text
login.jsp
  ↓ POST
CsrfFilter
  ↓
LoginServlet
  ↓
UserDAO
  ↓
app_users
  ↓
PasswordUtil
  ↓
User
  ↓
HttpSession
  ↓ redirect
ProductListServlet
```

### 商品一覧

```text
GET /products
  ↓
LoginFilter
  ↓
ProductListServlet
  ↓
ProductDAO.findAll()
  ↓
productsテーブル
  ↓
ResultSet
  ↓
List<Product>
  ↓ request属性
product-list.jsp
  ↓
HTML
```

### 商品登録

```text
product-form.jsp
  ↓ POST
CsrfFilter
  ↓
LoginFilter
  ↓
ProductCreateServlet
  ↓
Product + loginUser
  ↓
ProductDAO.insertWithLog()
  ├─ productsへINSERT
  ├─ product_operation_logsへINSERT
  └─ commit / rollback
  ↓ redirect
GET /products
```

---

## 33. この章を読み終えた時点で説明できること

- ブラウザがJavaクラスを直接呼ぶのではなく、Tomcatが仲介すること
- URLとServletが`@WebServlet`で対応していること
- FilterがServletより前に共通検査を行うこと
- HTMLの`name`と`getParameter()`が対応すること
- request parameter、request attribute、Sessionの違い
- `ResultSet`を`Product`へ変換してJSPへ渡す理由
- `${product.name}`がgetterを利用すること
- `forward()`が同じrequestを渡し、redirectが新しいリクエストを起こすこと
- 登録成功後にPRGを使う理由
- SessionとCookieによってログイン状態を引き継ぐ仕組み
- GETを表示、POSTを状態変更に使う考え方
- 400、403、404、500の使い分け
- Servlet、DAO、JSPの責任を分ける理由

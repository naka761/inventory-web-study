<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<%@ taglib prefix="c"
           uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>商品一覧</title>
</head>
<body>

<h1>商品一覧</h1>

<p>
    ようこそ、
    <c:out value="${sessionScope.loginUser.displayName}" />
    さん😼
</p>

<form action="${pageContext.request.contextPath}/logout"
      method="post">
      
      <input type="hidden"
       name="csrfToken"
       value="${sessionScope.csrfToken}">

    <button type="submit">
        ログアウト
    </button>

</form>

<p>
    <a href="${pageContext.request.contextPath}/products/create">
        新しい商品を登録する
    </a>
</p>
<c:if test="${empty products}">
    <p>商品は登録されていません。</p>
</c:if>

<c:if test="${not empty products}">
    <table border="1">
        <tr>
    <th>商品ID</th>
    <th>商品名</th>
    <th>価格</th>
    <th>在庫数</th>
    <th>操作</th>
</tr>

        <c:forEach var="product" items="${products}">
    <tr>
        <td>${product.id}</td>
        <td>
    <c:out value="${product.name}" />
</td>
        <td>${product.price}円</td>
        <td>${product.stock}個</td>

        <td>
    <a href="${pageContext.request.contextPath}/products/edit?id=${product.id}">
        編集
    </a>

    <form action="${pageContext.request.contextPath}/products/delete"
          method="post"
          style="display: inline;"
          onsubmit="return confirm('本当に削除しますか？');">

        <input type="hidden"
               name="id"
               value="${product.id}">

        <button type="submit">削除</button>
    </form>
</td>
    </tr>
</c:forEach>
    </table>
</c:if>

<p>
    <a href="${pageContext.request.contextPath}/">
        トップへ戻る
    </a>
</p>

</body>
</html>
<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
         <%@ taglib prefix="fn"
           uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>商品編集</title>
</head>
<body>

<h1>商品編集</h1>

<p style="color: red;">${errorMessage}</p>

<form action="${pageContext.request.contextPath}/products/edit"
      method="post">
      <input type="hidden"
       name="csrfToken"
       value="${sessionScope.csrfToken}">

    <%-- 更新対象を特定するため、画面には見せず商品IDを送る --%>
    <input type="hidden"
           name="id"
           value="${product.id}">

    <p>
        商品ID：${product.id}
    </p>

    <p>
        <label>
            商品名：
            <input type="text"
                   name="name"
                   value="${fn:escapeXml(product.name)}"
                   required>
        </label>
    </p>

    <p>
        <label>
            価格：
            <input type="number"
                   name="price"
                   value="${product.price}"
                   min="0"
                   required>
        </label>
    </p>

    <p>
        <label>
            在庫数：
            <input type="number"
                   name="stock"
                   value="${product.stock}"
                   min="0"
                   required>
        </label>
    </p>

    <button type="submit">更新</button>

</form>

<p>
    <a href="${pageContext.request.contextPath}/products">
        商品一覧へ戻る
    </a>
</p>

</body>
</html>
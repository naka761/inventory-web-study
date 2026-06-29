<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<%@ taglib prefix="c"
           uri="http://java.sun.com/jsp/jstl/core" %>

<%@ taglib prefix="fn"
           uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>商品登録</title>
</head>
<body>

<h1>商品登録</h1>

<c:if test="${not empty errorMessage}">
    <p style="color: red;">
        <c:out value="${errorMessage}" />
    </p>
</c:if>

<form action="${pageContext.request.contextPath}/products/create"
      method="post">

    <input type="hidden"
           name="csrfToken"
           value="${sessionScope.csrfToken}">

    <p>
        <label>
            商品名：
            <input type="text"
                   name="name"
                   value="${fn:escapeXml(param.name)}"
                   maxlength="100"
                   required>
        </label>
    </p>

    <p>
        <label>
            価格：
            <input type="number"
                   name="price"
                   value="${param.price}"
                   min="0"
                   required>
        </label>
    </p>

    <p>
        <label>
            在庫数：
            <input type="number"
                   name="stock"
                   value="${param.stock}"
                   min="0"
                   required>
        </label>
    </p>

    <button type="submit">登録</button>

</form>

<p>
    <a href="${pageContext.request.contextPath}/products">
        商品一覧へ戻る
    </a>
</p>

</body>
</html>

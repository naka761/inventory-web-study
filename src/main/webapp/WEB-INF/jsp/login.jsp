<%@ page language="java"
         contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>

<%@ taglib prefix="c"
           uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>ログイン</title>
</head>
<body>

<h1>在庫管理システム ログイン</h1>

<c:if test="${not empty errorMessage}">
    <p style="color: red;">
        <c:out value="${errorMessage}" />
    </p>
</c:if>

<form action="${pageContext.request.contextPath}/login"
      method="post">
      <input type="hidden"
       name="csrfToken"
       value="${sessionScope.csrfToken}">

    <p>
        <label>
            ログインID：
            <input type="text"
                   name="loginId"
                   required>
        </label>
    </p>

    <p>
        <label>
            パスワード：
            <input type="password"
                   name="password"
                   required>
        </label>
    </p>

    <button type="submit">ログイン</button>

</form>

</body>
</html>
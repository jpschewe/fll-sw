<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>

<head>
<title>Permission Denied</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <p>
        Your password was changed in a different session. You will need
        to visit the <a href="<c:url value='/login.jsp'/>">login
            page</a> to login again.
    </p>

</body>

</html>
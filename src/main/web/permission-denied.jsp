<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.PermissionDenied.populateContext(session, request);
%>

<html>

<head>
<title>Permission Denied</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <p>You do not have permission to access the requested page.</p>
    <c:choose>
        <c:when test="${authentication.loggedIn}">
            <p>
                You are logged in as ${authentication.username}. If you
                want to change users you can visit the <a
                    href="<c:url value='/login.jsp'/>">login page</a>
                and login as a different user.
            </p>
        </c:when>
        <c:otherwise>
            <p>
                You are not logged in. You can visit the <a
                    href="<c:url value='/login.jsp'/>">login page</a>
                and login.
            </p>
        </c:otherwise>
    </c:choose>

</body>

</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>
<%
fll.web.admin.AskCreateAdmin.populateContext(application, pageContext);
%>

<html>

<head>
<title>Create Admin?</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <div>
        The following admin users exist in the database. Would you like
        to create an additional admin user?
        <ul>
            <c:forEach items="${adminUsers}" var="user">
                <li>${user}</li>
            </c:forEach>
        </ul>
    </div>
    <form method="GET" action="createUsername.jsp" name="create_admin"
        id="create_admin">
        <input type="hidden" name="ADMIN" />

        <input name="submit_create_admin" id="submit_create_admin"
            value="Create Admin User" type="submit" />
    </form>

    <form method="GET" action="<c:url value='/'/>"
        name="no_create_admin" id="no_create_admin">
        <input name="submit_no_create_admin" id="submit_no_create_admin"
            value="Do not create an admin user" type="submit" />
    </form>

</body>

</html>
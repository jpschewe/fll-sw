<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />


<%
fll.web.admin.UserInfo.populateContext(application, pageContext);
%>

<html>
<head>
<title>User Information</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />


<style>
#user-info table {
    border-collapse: collapse;
}

#user-info th, #user-info td {
    border: 1px solid black;
    padding: 8px;
    text-align: center;
}
</style>

</head>

<body>

    <table id="user-info">
        <tr>
            <th>Username</th>
            <th>Last Page Access</th>
            <th>Number of Login Failures</th>
            <th>Last Login Failure</th>
            <th>Roles</th>
        </tr>

        <c:forEach items="${users}" var="user">
            <tr>
                <td>${user.username}</td>
                <td>
                    <javatime:format value="${user.lastAccess}"
                        style="SS" />
                </td>
                <td>${user.loginFailures}</td>
                <td>
                    <javatime:format value="${user.lastLoginFailure}"
                        style="SS" />
                </td>
                <td>${user.roles}</td>
            </tr>
        </c:forEach>
    </table>

</body>
</html>

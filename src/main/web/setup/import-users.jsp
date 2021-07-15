<%@ include file="/WEB-INF/jspf/init.jspf"%>
<fll-sw:required-roles roles="ADMIN" allowSetup="true" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>Import users from previous database. Select the users to
        keep.</p>

    <form action="ImportUsers" id='import-users-form' method="POST">
        <table>
            <tr>
                <th>Import</th>
                <th>Username</th>
                <th>Roles</th>
            </tr>

            <c:forEach items="${previousAccounts}" var="account">
                <tr>
                    <td>
                        <input type="checkbox"
                            name="${account.username}" checked />
                    </td>
                    <td>${account.username}</td>
                    <td>${account.roles}</td>
                </tr>
            </c:forEach>

        </table>

        <input type="submit" name="import_users" value="Import Users" />
    </form>

</body>
</html>

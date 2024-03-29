<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.RemoveUser.populateContext(application, pageContext);
%>

<html>

<head>
<title>Remove User</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <c:choose>
        <c:when test="${authentication.admin}">

            <c:choose>
                <c:when test="${all_users.size() == 1 }">
                    <p>There are no users other than the one you are
                        logged in as. There is nothing to do here.</p>
                </c:when>
                <c:otherwise>

                    <p>Select the user to remove. You cannot remove
                        the user that you are logged in as.</p>

                    <form name="form.remove_user" id="form.remove_user"
                        action="RemoveUser" method="POST">

                        <select id="remove_user" name="remove_user">

                            <c:forEach items="${all_users }"
                                var="loop_user">
                                <c:if
                                    test="${loop_user != authentication.username }">
                                    <option value="${loop_user }">${loop_user }</option>
                                </c:if>
                            </c:forEach>

                        </select>

                        <input name="submit_remove_user"
                            id="submit_remove_user" value="Remove User"
                            type="submit" />

                    </form>
                </c:otherwise>
            </c:choose>

        </c:when>
        <c:otherwise>
            <p>
                You must be logged in as an admin user. You should visit
                the <a href="<c:url value='/login.jsp'/>">login page</a>
                to log in.
            </p>
        </c:otherwise>
    </c:choose>


</body>

</html>

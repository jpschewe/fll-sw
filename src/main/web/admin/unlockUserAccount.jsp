<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.UnlockUserAccount.populateContext(application, pageContext);
%>

<html>

<head>
<title>Unlock User Account</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <c:choose>
        <c:when test="${authentication.admin}">

            <p>Select the user to unlock.</p>

            <form name="form.unlock_user" id="form.unlock_user"
                action="UnlockUserAccount" method="POST">

                <select id="unlock_user" name="unlock_user">

                    <c:forEach items="${all_users }" var="loop_user">
                        <c:if
                            test="${loop_user != authentication.username }">
                            <option value="${loop_user }">${loop_user }</option>
                        </c:if>
                    </c:forEach>

                </select>

                <input name="submit_unlock_user" id="submit_unlock_user"
                    value="Unlock User" type="submit" />

            </form>
        </c:when>
        <c:otherwise>
            <p>
                You must be logged in as an admin. You should visit the
                <a href="<c:url value='/login.jsp'/>">login page</a> to
                log in.
            </p>
        </c:otherwise>
    </c:choose>


</body>

</html>

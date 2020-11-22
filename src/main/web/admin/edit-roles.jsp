<%@ include file="/WEB-INF/jspf/init.jspf"%>
<%@ page import="fll.web.UserRole"%>

<c:if test="${not authentication.admin}">
    <jsp:forward page="/permission-denied.jsp"></jsp:forward>
</c:if>

<%
fll.web.admin.EditRoles.populateContext(application, pageContext);
%>

<html>

<head>
<title>Edit Roles</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <div>
        <dl>
            <c:forEach items="${possibleRoles}" var="role">
                <dt>${role}</dt>
                <dd>${role.description}</dd>
            </c:forEach>

        </dl>
    </div>

    <form method="POST" action="EditRoles" name="edit_roles"
        id="edit_roles">
        <table>
            <tr>
                <th></th>
                <c:forEach items="${possibleRoles}" var="role">
                    <th>${role}</th>
                </c:forEach>
            </tr>

            <c:forEach items="${users}" var="user">
                <tr>
                    <c:set var="selectedRoles"
                        value="${userRoles[user]}" />

                    <td>${user}</td>

                    <c:forEach items="${possibleRoles}" var="role">
                        <c:if test="${selectedRoles.contains(role) }">
                            <c:set var="checked" value="checked" />
                        </c:if>


                        <td>
                            <c:choose>
                                <c:when
                                    test="${user eq authentication.username && UserRole.ADMIN eq role}">
                                    <%--Need to use a hidden field so that the value shows up in the request, disabled fields aren't sent --%>
                                    <input name="${user}_${role}"
                                        id="${user}_${role}"
                                        type="hidden" value="on" />
                                    <input type="checkbox" ${checked}
                                        disabled />
                                </c:when>
                                <c:otherwise>
                                    <input name="${user}_${role}"
                                        id="${user}_${role}"
                                        type="checkbox" ${checked} />
                                </c:otherwise>
                            </c:choose>
                        </td>

                        <c:remove var="checked" />
                    </c:forEach>

                </tr>
            </c:forEach>

        </table>

        <input name="submit_edit_roles" value="Save Changes"
            type="submit" />
    </form>


</body>

</html>
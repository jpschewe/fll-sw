<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="true" />

<%
fll.web.admin.CreateUser.populateContext(request, pageContext);
%>

<html>

<head>
<title>Create User</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript" src="check_passwords_match.js"></script>

</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form method="POST" action="CreateUser" name="create_user"
        id="create_user"
        onsubmit="return CheckPasswordsModule.validatePasswordsMatch()">
        <div>
            Username:
            <input type="text" size="15" maxlength="64" name="user"
                id="user" pattern="[a-zA-Z0-9_]+" required>
            valid characters: letters, numbers and underscore
        </div>
        <div>
            Password :
            <input type="password" size="15" name="pass" id="pass"
                required>
        </div>
        <div>
            Repeat Password :
            <input type="password" size="15" name="pass_check"
                id="pass_check" required>
        </div>

        <div>Roles:</div>
        <c:forEach items="${possibleRoles}" var="role">
            <div>
                <label>
                    <c:if test="${selectedRoles.contains(role) }">
                        <c:set var="checked" value="checked" />
                    </c:if>

                    <input type='checkbox' name='role_${role}'
                        value='true' id='role_${role}' ${checked} />${role}
                    - ${role.description}

                    <c:remove var="checked" />
                </label>
            </div>
        </c:forEach>

        <input name="submit_create_user" value="Create User"
            type="submit" />
    </form>


</body>

</html>
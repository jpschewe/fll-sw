<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>

<head>
<title>Administration</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript" src="check_passwords_match.js"></script>

</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <c:choose>
        <c:when test="${authentication.loggedIn}">
            <p>Change password for ${authentication.username}.</p>

            <form method="POST" action="ChangePassword"
                name="change_password" id="change_password"
                onsubmit="return CheckPasswordsModule.validatePasswordsMatch()">
                <div>
                    Old password :
                    <input type="password" size="15" maxlength="64"
                        name="old_password" required>
                </div>

                <div>
                    New password :
                    <input type="password" size="15" name="pass"
                        id="pass" required>
                </div>

                <div>
                    Repeat new password :
                    <input type="password" size="15" name="pass_check"
                        id="pass_check" required>
                </div>

                <input name="submit_change_password"
                    value="Change Password" type="submit" />
            </form>
        </c:when>
        <c:otherwise>
            <p>
                You are not currently logged in as a user, therefore you
                cannot change your password. Please visit the <a
                    href="<c:url value='/login.jsp'/>">login page</a> to
                log in.
            </p>
        </c:otherwise>
    </c:choose>

</body>

</html>
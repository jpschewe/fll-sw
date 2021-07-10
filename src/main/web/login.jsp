<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="true" />

<%
fll.web.DoLogin.storeParameters(request, session);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Login Page</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<style type='text/css'>
body {
    font-size: x-large;
}

input {
    font-size: x-large;
}
</style>
</head>
<body>
    <h1>Login to FLL-SW</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form method="POST" action="<c:url value='/DoLogin'/>" name="login"
        data-ajax="false">

        <div>
            <label for="user"> Username:</label>
            <input type="text" size="15" maxlength="64" name="user"
                id="user" autocapitalize="off" autocomplete="off"
                spellcheck="false" autofocus="autofocus" />
        </div>

        <div>
            <label for="pass">Password:</label>
            <input type="password" size="15" name="pass" id="pass" />
        </div>

        <div>
            <input name="submit_login" value="Login" type="submit" />
        </div>
    </form>
</body>
</html>

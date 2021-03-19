<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="true" />

<% fll.web.DoLogin.storeParameters(request, session); %>

<!DOCTYPE HTML>
<html>
<head>
<title>Login Page</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet"
    href="<c:url value='/extlib/jquery.mobile-1.4.5/jquery.mobile-1.4.5.min.css'/>" />

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/extlib/jquery.mobile-1.4.5/jquery.mobile-1.4.5.min.js'/>"></script>

</head>
<body>
    <h1>Login to FLL</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form method="POST" action="<c:url value='/DoLogin'/>" name="login" data-ajax="false">
        Username :
        <input type="text" size="15" maxlength="64" name="user"
            autocorrect="off" autocapitalize="off" autocomplete="off"
            spellcheck="false" />
        <br />
        Password :
        <input type="password" size="15" name="pass" />
        <br />
        <input name="submit_login" value="Login" type="submit" />
    </form>
</body>
</html>

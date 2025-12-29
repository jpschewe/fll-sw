<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="true" />

<!DOCTYPE HTML>
<html>
<head>
<title>Login Page</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css" href="login.css" />

<script type='text/javascript' src='login.js'></script>

</head>
<body>
    <h1>Login to FLL-SW</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form method="POST" action="<c:url value='/DoLogin'/>" name="login"
        data-ajax="false">

        <input type='hidden' name="workflow_id"
            value='${param.workflow_id}' />

        <div>
            <label for="user"> Username:</label>
            <input type="text" size="15" maxlength="64" name="user"
                id="user" autocapitalize="off" autocomplete="off"
                spellcheck="false" autofocus="autofocus" />
        </div>

        <div>
            <label for="pass">Password:</label>
            <input type="password" size="15" name="pass" id="pass" />

            <input type="checkbox" id="show_password" />
            <label for="show_password">Show password</label>
        </div>

        <div>
            <input name="submit_login" value="Login" type="submit" />
        </div>
    </form>
</body>
</html>

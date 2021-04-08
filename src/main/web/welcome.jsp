<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@page import="fll.web.Welcome"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html>
<head>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<meta http-equiv='refresh' content='90' />

<title>Welcome</title>
<style type='text/css'>
html {
    margin-top: 5px;
    margin-bottom: 5px;
    margin-left: 5px;
    margin-right: 5px;
}

body {
    margin-top: 4;
}

.title {
    font-weight: bold;
    font-size: 200%;
    padding: 20px;
}
</style>

</head>

<body>

    <div class='center'>
        <h1>${challengeDescription.title }</h1>

        <table align="center" width="60%">
            <tr>
                <td align="center"><img
                    src='<c:url value="/images/htk_logo.jpg"/>'
                    width="100%" align="middle" /></td>
        </table>

        <%
            Welcome.outputLogos(application, out);
        %>

        <table align="center" width="50%">
            <tr>
                <td align="center"><img src='images/fll_logo.gif'
                    width="60%" /></td>
            </tr>
        </table>

        <h2>${ScorePageText }</h2>

    </div>

</body>
</html>

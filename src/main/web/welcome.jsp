<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.Welcome.populateContext(pageContext);
%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html>
<head>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/base.css'/>" />

<link rel="stylesheet" type="text/css"
    href="<c:url value='/welcome.css'/>" />

<meta http-equiv='refresh' content='90' />

<title>Welcome</title>

</head>

<body>

    <div class='center'>
        <h1>${challengeDescription.title }</h1>

        <table class="center partner_logo">
            <tr>
                <td>
                    <img src='${partner_logo}' />
                </td>
        </table>

        <%
        fll.web.Welcome.outputLogos(application, out);
        %>

        <table class="center fll_logo">
            <tr>
                <td>
                    <img src='${fll_logo}' />
                </td>
            </tr>
        </table>

        <h2>${ScorePageText }</h2>

    </div>

</body>
</html>

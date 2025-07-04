<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.DatabaseBackupsIndex.populateContext(pageContext);
%>

<html>
<head>
<title>Database backups</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Database backups</h1>
    <p>Download the database file that you want to reset to.</p>

    <ul>
        <c:forEach items="${backups}" var="backup">
            <li>
                <a href="<c:url value='/${BASE_URL}/${backup}' />">${backup}</a>
            </li>
        </c:forEach>
    </ul>

</body>

</html>
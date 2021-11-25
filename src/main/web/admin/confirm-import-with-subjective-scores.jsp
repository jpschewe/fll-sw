<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Verify export with subjective scores</title>
</head>
<body>
    <p>
        There are
        <b>subjective</b>
        scores in this database that will be over written. Do you want
        to continue?
    </p>
    <form action="<c:url value='/developer/importdb/FindMissingTeams'/>">
        <input type='submit' value="Yes, overwrite subjective scores" />
    </form>
    <form action="index.jsp">
        <input type='submit' value="No, go back to the admin index" />
    </form>
</body>
</html>
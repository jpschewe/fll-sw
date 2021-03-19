<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Verify export with subjective scores</title>
</head>
<body>
    <p>There are subjective scores in this database. Are you sure
        that you want to export the performance data?</p>
    <form action="ExportPerformanceData">
        <input type='submit' value="Export anyway" />
    </form>
    <form action="index.jsp">
        <input type='submit' value="No, go back to the admin index" />
    </form>
</body>
</html>
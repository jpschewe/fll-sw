<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<title>Confirm Performance Scores Exist</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Confirm Performance Scores Exist</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>
        Performance scores have already been entered for this
        tournament. Uploading a new schedule can cause issues with
        existing scores and the reports. Are you sure you want to
        replace the schedule?
        <br />
        <a href='<c:url value="/schedule/CheckScheduleExists"/>'>
            <button>Yes</button>
        </a> <a href='<c:url value="/admin/index.jsp"/>'>
            <button>No</button>
        </a>
        <br />
    </p>

</body>
</html>

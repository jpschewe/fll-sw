<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<title>Violations (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Violations (Upload Schedule)</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>There are some warnings in the schedule.</p>

    <ul>
        <c:forEach items="${uploadScheduleData.violations}"
            var="violation">
            <li class='soft-violation'>${violation.message }</li>
        </c:forEach>
    </ul>

    <p>
        Do you want to proceed and use this schedule?
        <a id='yes'
            href='<c:url value="/schedule/GatherTeamInformationChanges"/>'>
            <button>Yes</button>
        </a>
        <a id='no' href='<c:url value="/admin/index.jsp"/>'>
            <button>No</button>
        </a>
    </p>

</body>
</html>

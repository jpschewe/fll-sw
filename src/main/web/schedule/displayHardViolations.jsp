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
    <p>There are some errors with this schedule, please correct them
        and try again.</p>

    <ul>
        <c:forEach items="${uploadScheduleData.violations}"
            var="violation">
            <c:choose>
                <c:when test="${violation.type == 'HARD' }">
                    <li class='hard-violation'>${violation.message }</li>
                </c:when>
                <c:otherwise>
                    <li class='soft-violation'>${violation.message }</li>
                </c:otherwise>
            </c:choose>
        </c:forEach>
    </ul>

    <p>
        <a href='<c:url value="/admin/index.jsp"/>'>Return to admin
            page</a>
        <br />
    </p>

</body>
</html>

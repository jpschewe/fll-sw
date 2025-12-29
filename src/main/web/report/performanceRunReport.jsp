<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF,JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.PerformanceRunReport.populateContext(application, session, request, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>${RunName}&nbsp;scores</title>

<style>
table#perf-data {
    border-collapse: collapse;
}

table#perf-data, table#perf-data th, table#perf-data td {
    border: 1px solid black;
}

table#perf-data th, table#perf-data td {
    padding-right: 5px;
    padding-left: 5px;
}
</style>

</head>

<body>
    <h1>${RunName}</h1>


    <c:forEach items="${results}" var="entry">
        <c:set var="awardGroup" value="${entry.key}" />
        <c:set var="scores" value="${entry.value}" />
        <h2>Award Group ${awardGroup}</h2>
        <table id="perf-data">
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
                <th>Organization</th>
                <th>Score</th>
                <th>Last Edited</th>
                <th></th>
            </tr>
            <c:forEach items="${scores}" var="score">
                <tr>
                    <td>${score.teamNumber}</td>
                    <td>${score.teamName}</td>
                    <td>${score.organization}</td>
                    <td>
                        <c:choose>
                            <c:when test="${score.noShow}">
                            No Show
                            </c:when>
                            <c:otherwise>
                                    ${score.computedTotal}
                                </c:otherwise>
                        </c:choose>
                    </td>
                    <td class="right">
                        <javatime:format value="${score.lastEdited}"
                            pattern="h:mm" />
                    </td>
                    <td>
                        <a
                            href='<c:url value="/scoreEntry/scoreEntry.jsp?TeamNumber=${score.teamNumber}&EditFlag=true&RunNumber=${RunNumber}&workflow_id=${workflow_id}"/>'>Edit
                        </a>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </c:forEach>

</body>
</html>

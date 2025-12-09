<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF,JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.TeamPerformanceReport.populateContext(application, session, request, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>${team.teamName }&nbsp;-&nbsp;${team.teamNumber}&nbsp;
    Performance Scores</title>

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
    <h1>${team.teamName }&nbsp;-&nbsp;${team.teamNumber}&nbsp;Performance
        Scores</h1>
    <table id="perf-data">
        <tr>
            <th>Run</th>
            <th>Score</th>
            <th>Last Edited</th>
            <th></th>
        </tr>
        <c:forEach items="${data}" var="score">
            <tr>
                <td>${score.runName}</td>
                <td>
                    <c:choose>
                        <c:when test="${score.noShow}">
                        No Show
                    </c:when>
                        <c:when test="${score.bye}">
                        Bye
                    </c:when>
                        <c:otherwise>
                    ${score.computedTotal}
                    </c:otherwise>
                    </c:choose>
                </td>
                <td class="right">
                    <javatime:format value="${score.lastEdited}"
                        pattern="h:mm " />
                </td>
                <td>
                    <a
                        href='<c:url value="/scoreEntry/scoreEntry.jsp?TeamNumber=${team.teamNumber}&EditFlag=true&RunNumber=${score.runNumber}&workflow_id=${workflow_id} "/>'>Edit
                    </a>
                </td>
            </tr>
        </c:forEach>
    </table>

</body>
</html>

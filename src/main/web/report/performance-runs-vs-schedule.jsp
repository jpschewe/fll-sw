<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF,HEAD_JUDGE" allowSetup="false" />

<%
fll.web.report.PerformanceRunsVsSchedule.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

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

<title>Performance Runs vs. Schedule</title>
</head>

<body>
    <h1>Performance Runs vs. Schedule</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <table id="perf-data" class="center">

        <tr>
            <th>Team Number</th>
            <th>Team Name</th>
            <th>Organization</th>

            <th>Round</th>
            <th>Scheduled Table</th>
            <th>Scheduled Time</th>
            <th>Last edited Time</th>
            <th>Score</th>
            <th>Table</th>
        </tr>

        <c:forEach items="${data}" var="entry">
            <tr>

                <td>${entry.team.teamNumber}</td>
                <td>${entry.team.teamName}</td>
                <td>${entry.team.organization}</td>

                <td>${entry.roundNumber}</td>
                <c:choose>
                    <c:when test="${not empty entry.performanceTime}">
                        <td>
                            <javatime:format
                                value="${entry.performanceTime.time}"
                                pattern="h:mm " />
                        </td>
                        <td>${entry.performanceTime.tableAndSide}</td>
                    </c:when>
                    <c:otherwise>
                        <td>&nbsp;</td>
                        <td>&nbsp;</td>
                    </c:otherwise>
                </c:choose>

                <c:choose>
                    <c:when test="${not empty entry.lastEdited}">
                        <td>
                            <javatime:format value="${entry.lastEdited}"
                                pattern="h:mm " />
                        </td>
                    </c:when>
                    <c:otherwise>
                        <td>&nbsp;</td>
                    </c:otherwise>
                </c:choose>
                
                <td>${entry.formattedScore}</td>
                <td>${entry.table}</td>

            </tr>
        </c:forEach>

    </table>

</body>
</html>

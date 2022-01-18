<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Ref Links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Ref links</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>

    <a class="wide"
        href="<c:url value='/scoreEntry/scoreEntry.jsp?tablet=true&practice=true&showScores=false'/>">Practice
        round score entry</a>

    <a class="wide" href="scoreEntry/choose-table.jsp">Score Entry -
        follow this link on the performance score entry computers.</a>

    <a class="wide"
        href="<c:url value='/report/regular-match-play-runs.jsp' />">Regular
        Match Play performance scores</a>

    <a class="wide"
        href="<c:url value='/report/topScoreReportPerAwardGroup.jsp' />">Top
        performance scores by award group. This creates a text report of
        the top regular match play round scores by award group.</a>

    <div class="wide">
        <form
            ACTION="<c:url value='/report/performanceRunReport.jsp' />"
            METHOD='POST'>
            Show scores for performance run
            <select name='RunNumber'>
                <c:forEach var="index" begin="1" end="${maxRunNumber}">
                    <option value='${index }'>${index }</option>
                </c:forEach>
            </select>
            <input type='submit' value='Show Scores' />
        </form>
    </div>

    <div class="wide">
        <form
            action="<c:url value='/report/teamPerformanceReport.jsp' />"
            method='post'>
            Show performance scores for team
            <select name='TeamNumber'>
                <c:forEach items="${tournamentTeams}" var="team">
                    <option value='<c:out value="${team.teamNumber}"/>'>
                        <c:out value="${team.teamNumber}" /> -
                        <c:out value="${team.teamName}" />
                    </option>
                </c:forEach>
            </select>
            <input type='submit' value='Show Scores' />
        </form>
    </div>

    <a class="wide" href="<c:url value='/report/unverifiedRuns.jsp' />">Unverified
        runs. Unverfied performance runs.</a>

    <a class="wide" href="<c:url value='/admin/PerformanceSchedule' />"
        target="_new">Performance Schedule</a>

    <a class="wide"
        href="<c:url value='/admin/PerformanceSchedulePerTable' />"
        target="_new">Performance Schedule per table</a>

    <a class="wide" href="<c:url value='/admin/PerformanceSheets' />"
        target="_new">Performance sheets for regular match play</a>


</body>
</html>

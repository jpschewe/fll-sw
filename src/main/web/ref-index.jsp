<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
fll.web.PageVariables.populateCompletedRunData(application, pageContext);
fll.web.PageVariables.populateTournamentTeams(application, pageContext);
%>

<html>
<head>
<title>Ref Links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Ref links</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>
        The current tournament is
        <b>${tournamentData.currentTournament.description} on
            ${tournamentData.currentTournament.dateString}
            [${tournamentData.currentTournament.name}]</b>
    </p>

    <a class="wide" href="scoreEntry/choose-table.jsp">Score Entry -
        follow this link on the performance score entry computers.</a>

    <a class="wide"
        href="<c:url value='/report/regular-match-play-runs.jsp' />">Regular
        Match Play performance scores</a>

    <a class="wide"
        href="<c:url value='/report/performance-runs-vs-schedule.jsp' />">Performance
        runs compared with the schedule</a>

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
                <c:forEach items="${completedRunMetadata}" var="md">
                    <option value='${md.runNumber}'>${md.displayName}</option>
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
        runs. Unverified performance runs.</a>

    <a class="wide" href="<c:url value='/admin/PerformanceSchedule' />"
        target="_new">Performance Schedule</a>

    <a class="wide"
        href="<c:url value='/admin/PerformanceSchedulePerTable' />"
        target="_new">Performance Schedule per table</a>

    <a class="wide" href="<c:url value='/admin/PerformanceNotes' />"
        target="_new">Performance Schedule per table for notes</a>

    <a class="wide" href="<c:url value='/admin/PerformanceSheets' />"
        target="_new">Performance sheets for regular match play</a>

    <a class="wide"
        href="<c:url value='/scoreEntry/scoreEntry.jsp?tablet=true&practice=true&showScores=false'/>">
        Score entry training</a>




</body>
</html>

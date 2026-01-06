<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.PageVariables.populateTournamentTeams(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Additional Reports</title>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>

</head>

<body>
    <h1>Additional Reports</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <h2>Judging Reports</h2>

    <a class="wide"
        href="<c:url value='/report/CategoryScoresByScoreGroup'/>"
        target="_blank">Award category scores - by category and
        judging group</a>

    <a class='wide'
        href="<c:url value='/report/ScaledSubjectiveByAwardGroup'/>"
        target="_blank">Subjective scaled scores by award group</a>

    <a class="wide" href="<c:url value='/report/Awards.csv'/>">CSV
        file of award winners.</a>

    <a class="wide" href="<c:url value='/report/AwardsReport'/>"
        target="_blank">Award Winners Report</a>

    <!-- Team Results -->
    <a class="wide" href="<c:url value='/report/TeamResults'/>"
        target="_blank">Team Results. This is a zip file containing
        the results to return to the teams. This will take some time to
        generate, be patient.</a>

    <div class="wide">
        <form action="<c:url value='/report/TeamResults'/>"
            method='post' target="_blank">
            Results for a single team
            <select name='TeamNumber'>
                <c:forEach items="${tournamentTeams}" var="team">
                    <option value='<c:out value="${team.teamNumber}"/>'>
                        <c:out value="${team.teamNumber}" /> -
                        <c:out value="${team.teamName}" />
                    </option>
                </c:forEach>
            </select>
            <input type='submit' value='Get Results' />
        </form>
    </div>
    <!-- end Team Results -->

    <h2>Robot Match Reports</h2>

    <a class="wide" href="<c:url value='/report/PerformanceReport'/>"
        target="_blank"> Performance scores - full tournament </a>

    <a class="wide"
        href="<c:url value='/report/PerformanceScoreReport'/>"
        target="_blank">Performance scores - by team </a>

    <a class="wide" href="<c:url value='/report/PlayoffReport'/>"
        target="_blank">Winners of each head to head bracket</a>

    <h2>Cross-tournament reports</h2>
    <p>Reports that use data across multiple tournaments.</p>

    <a class="wide"
        href="<c:url value='/report/TournamentAdvancement'/>"
        target="_blank">Tournament advancement report - this is a
        CSV file that contains information about the teams that are
        advancing from each tournament. For this to work all of the
        databases need to be merged and the award winners need to be
        specified for each tournament.</a>

</body>
</html>
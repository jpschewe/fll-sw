<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Reporting</title>

<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

</head>

<body>
    <h1>Reporting</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <h2>All Tournaments</h2>
    <a class="wide" href="summarizePhase1.jsp">Compute summarized
        scores. This needs to be executed before any reports can be
        generated. You will be returned to this page if there are no
        errors summarizing scores.</a>

    <a class="wide" href="NonNumericNomineesReport" target="_blank">Optional
        award nominations</a>

    <a class="wide" href="topScoreReportPerAwardGroup.jsp">Top
        performance - Award group </a>

    <a class="wide" href="topScoreReportPerJudgingStation.jsp"> Top
        performance - Judging group </a>

    <a class="wide" href='FinalComputedScores' target="_blank">
        Summarized numeric scores - by judging group aka "Final Computed
        Scores"</a>

    <a class="wide" href="CategoryScoresByScoreGroup" target="_blank">Award
        category scores Categorized</a>

    <a class='wide' href='SubjectiveByJudge' target="_blank">Summarized
        numeric scores - by judges </a>

    <a class="wide" href="PerformanceReport" target="_blank">
        Performance scores - full tournament </a>

    <a class="wide" href="PerformanceScoreReport" target="_blank">Performance
        scores - by team </a>

    <a class="wide" href="PlayoffReport" target="_blank">Winners of
        each head to head bracket</a>

    <a class="wide" href="non-numeric-nominees.jsp" target="_blank">Entry:
        Non-numeric nominations <br /> This is used to enter the teams
        that are up for consideration for the non-scored subjective
        categories. This information can come from the subjective
        judging application. The information entered here transfers over
        to the finalist scheduling web application.
    </a>

    <a class="wide" href="edit-award-winners.jsp" target="_blank">Entry:
        Award winners </a>

    <a class="wide" href="edit-advancing-teams.jsp" target="_blank">Enter
        the teams advancing to the next level of tournament</a>

    <a class="wide" href="AwardsReport" target="_blank">Report of
        winners for the tournament. This can be published on the web or
        used for the awards ceremony.</a>

    <a class="wide" href="TeamResults" target="_blank">Team Results.
        This is a zip file containing the results to return to the
        teams. This will take some time to generate, be patient.</a>

    <div class="wide">
        <form action='TeamResults' method='post' target="_blank">
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

    <a class="wide" href="<c:url value='/report/awards/index.jsp'/>">Edit
        awards report and rewards script properties.</a>


    <h2>Finalist scheduling</h2>
    <p>This is used at tournaments where there is more than 1
        judging group in an award group. This is typically the case at a
        state tournament where all teams are competing for first place
        in each category, but there are too many teams for one judge to
        see.</p>

    <a class="wide" href="finalist/load.jsp" target="_blank">Schedule
        Finalists. Before visiting this page, all subjective scores need
        to be uploaded and any head to head brackets that will occur
        during the finalist judging should be created to avoid
        scheduling conflicts.</a>

    <c:if test="${not empty finalistDivisions}">
        <div class="wide">
            <form ACTION='finalist/PdfFinalistSchedule' METHOD='POST'
                target="_blank">
                <select name='division'>
                    <c:forEach var="division"
                        items="${finalistDivisions }">
                        <option value='${division }'>${division }</option>
                    </c:forEach>
                </select>
                <input type='submit' value='Finalist Schedule (PDF)' />
            </form>
        </div>
    </c:if>

    <a class="wide" href="finalist/TeamFinalistSchedule" target="_blank">Finalist
        Schedule for each team</a>

    <h2>Cross-tournament reports</h2>
    <p>Reports that use data across multiple tournaments.</p>

    <a class="wide" href="TournamentAdvancement" target="_blank">Tournament
        advancement report - this is a CSV file that contains
        information about the teams that are advancing from each
        tournament. For this to work all of the databases need to be
        merged and the award winners need to be specified for each
        tournament.</a>

    <h2>Other useful reports</h2>
    <p>Some reports that are handy for intermediate reporting and
        checking of the current tournament state.</p>

    <div class="wide">
        <form ACTION='performanceRunReport.jsp' METHOD='POST'>
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
        <form action='teamPerformanceReport.jsp' method='post'>
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

    <a class="wide" href="unverifiedRuns.jsp">Unverified runs.
        Unverfied performance runs.</a>

    <a class="wide" href="PerformanceScoreDump">CSV file containing
        all performance scores, excluding byes. This is useful to
        manually determine awards for most consistent or most improved
        robot performance.</a>

</body>
</html>

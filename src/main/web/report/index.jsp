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
    <ol>
        <li>
            <a href="summarizePhase1.jsp">Compute summarized scores</a>.
            This needs to be executed before any reports can be
            generated. You will be returned to this page if there are no
            errors summarizing scores. <a
                href='javascript:display("SummarizeHelp")'>[help]</a>
            <div id='SummarizeHelp' class='help' style='display: none'>
                This can be executed over and over again without worry
                of losing data. <a
                    href='javascript:hide("SummarizeHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a href="topScoreReportPerAwardGroup.jsp">Top
                performance scores by award group</a>. This creates a text
            report of the top regular match play round scores by award
            group.
        </li>

        <li>
            <a href="topScoreReportPerJudgingStation.jsp">Top
                performance scores by judging station</a>. This creates a
            text report of the top regular match play round scores by
            judging station.
        </li>

        <li>
            <a href='FinalComputedScores' target="_blank">Final
                Computed Scores</a>. This is the report that the head judge
            will want to determine which teams advance to the next
            tournament.<a href='javascript:display("FinalReportHelp")'>[help]</a>
            <div id='FinalReportHelp' class='help' style='display: none'>
                This report can be generated at any time. It is
                preferred to generate this report after all scores are
                entered. However if one award group is finished much
                earlier than the others it is fairly common to generate
                the report once to get information on the finished award
                group and then generate it again when the remaining
                award groups are finished.<a
                    href='javascript:hide("FinalReportHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a href="CategoryScoresByScoreGroup" target="_blank">Categorized
                Scores by Judging Group</a>. This displays the scaled scores
            for each category by judging group. This is useful for
            checking the winners of each category.
        </li>

        <li>
            <a href="PerformanceReport" target="_blank">Performance
                report</a>. This displays the performance scores for each
            team (including head to head) and computes some statistics
            on the scores.
        </li>

        <li>
            <a href="PerformanceScoreReport" target="_blank">Performance
                Score Report</a>. This displays the details of the
            performance runs for each team.
        </li>

        <li>
            <a href="PlayoffReport" target="_blank">Winners of each
                head to head bracket</a>. This is useful for the awards
            ceremony.
        </li>

        <li>
            <a href="non-numeric-nominees.jsp" target="_blank">Enter
                non-numeric nominees</a>. This is used to enter the teams
            that are up for consideration for the non-scored subjective
            categories. This information can come from the subjective
            judging application. The information entered here transfers
            over to the finalist scheduling web application.
        </li>

        <li>
            <a href="NonNumericNomineesReport" target="_blank">Report
                showing the teams that have been nominated for
                non-numeric awards</a>
        </li>

        <li>
            <a href="edit-award-winners.jsp" target="_blank">Enter
                the winners of awards for use in the awards report</a>
        </li>

        <li>
            <a href="edit-advancing-teams.jsp" target="_blank">Enter
                the teams advancing to the next level of tournament</a>
        </li>

        <li>
            <a href="AwardsReport" target="_blank">Report of winners
                for the tournament.</a> This can be published on the web or
            used for the awards ceremony.
        </li>

        <li>
            <a href="TeamResults" target="_blank">Team Results</a>. This
            is a zip file containing the results to return to the teams.
            This will take some time to generate, be patient.
            <ul>
                <li>
                    <form action='TeamResults' method='post'
                        target="_blank">
                        Results for a single team
                        <select name='TeamNumber'>
                            <c:forEach items="${tournamentTeams}"
                                var="team">
                                <option
                                    value='<c:out value="${team.teamNumber}"/>'>
                                    <c:out value="${team.teamNumber}" />
                                    -
                                    <c:out value="${team.teamName}" />
                                </option>
                            </c:forEach>
                        </select>
                        <input type='submit' value='Get Results' />
                    </form>
                </li>
            </ul>
        </li>

    </ol>


    <h2>Finalist scheduling</h2>
    <p>This is used at tournaments where there is more than 1
        judging group in an award group. This is typically the case at a
        state tournament where all teams are competing for first place
        in each category, but there are too many teams for one judge to
        see.</p>

    <ul>

        <li>
            <a href="finalist/load.jsp" target="_blank">Schedule
                Finalists</a>. Before visiting this page, all subjective
            scores need to be uploaded and any head to head brackets
            that will occur during the finalist judging should be
            created to avoid scheduling conflicts.
        </li>

        <li>
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
        </li>

        <li>
            <a href="finalist/TeamFinalistSchedule" target="_blank">Finalist
                Schedule for each team</a>
        </li>


    </ul>

    <h2>Cross-tournament reports</h2>
    <p>Reports that use data across multiple tournaments.</p>
    <ul>
        <li>
            <a href="TournamentAdvancement" target="_blank">Tournament
                advancement report</a> - this is a CSV file that contains
            information about the teams that are advancing from each
            tournament. For this to work all of the databases need to be
            merged and the award winners need to be specified for each
            tournament.
        </li>
    </ul>

    <h2>Other useful reports</h2>
    <p>Some reports that are handy for intermediate reporting and
        checking of the current tournament state.</p>

    <ul>
        <li>
            <form ACTION='performanceRunReport.jsp' METHOD='POST'>
                Show scores for performance run
                <select name='RunNumber'>
                    <c:forEach var="index" begin="1"
                        end="${maxRunNumber}">
                        <option value='${index }'>${index }</option>
                    </c:forEach>
                </select>
                <input type='submit' value='Show Scores' />
            </form>
        </li>

        <li>
            <form action='teamPerformanceReport.jsp' method='post'>
                Show performance scores for team
                <select name='TeamNumber'>
                    <c:forEach items="${tournamentTeams}" var="team">
                        <option
                            value='<c:out value="${team.teamNumber}"/>'>
                            <c:out value="${team.teamNumber}" /> -
                            <c:out value="${team.teamName}" />
                        </option>
                    </c:forEach>
                </select>
                <input type='submit' value='Show Scores' />
            </form>
        </li>

        <li>
            <a href="unverifiedRuns.jsp">Unverified runs</a>. Unverfied
            performance runs.
        </li>

        <li>
            <a href="PerformanceScoreDump">CSV file containing all
                performance scores</a>, excluding byes. This is useful to
            manually determine awards for most consistent or most
            improved robot performance.
        </li>

    </ul>


</body>
</html>

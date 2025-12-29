<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.ReportIndex.populateContext(application, session, pageContext, true);
fll.web.PageVariables.populateCompletedRunData(application, pageContext);
fll.web.PageVariables.populateTournamentTeams(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Reporting</title>

<script type='text/javascript'>
  var categoryJudges = JSON.parse('${categoryJudgesJson}');

  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>

<script type='text/javascript' src='index.js'></script>

</head>

<body>
    <h1>Reporting</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <h2>All Tournaments</h2>
    <a class="wide"
        href="<c:url value='/report/ComputeSummarizedScores'/>">Compute
        summarized scores. This should not be needed anymore as the
        reports take care of this themselves.</a>

    <a class="wide" href="<c:url value='/report/judge-summary.jsp'/>"
        target="_judge-summary">Judge summary. This shows which
        judges have scores entered.</a>

    <a class="wide"
        href="<c:url value='/report/NonNumericNomineesReport'/>"
        target="_blank">Optional Award Nominations</a>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="<c:url value='/report/topScoreReportPerAwardGroup.jsp'/>">Robot
        Match Scores (HTML)</a>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="TopScoreReportPerAwardGroupPdf">Robot Match Scores
        (PDF)</a>

    <c:if test="${awardGroups != judgingStations}">
        <a class="wide" target="_topScoreReportPerJudgingStation"
            href="topScoreReportPerJudgingStation.jsp">Robot Match
            Scores - Judging group (HTML)</a>

        <a class="wide" target="_topScoreReportPerJudgingStation"
            href="TopScoreReportPerJudgingStationPdf"> Robot Match
            Scores - Judging group (PDF)</a>
    </c:if>

    <!--  FinalComputedScores -->
    <div class="wide">
        Final Computed Scores
        <form action="<c:url value='/report/FinalComputedScores' />"
            target="_finalComputedScores" method="POST">
            <input type="hidden" name="selector"
                value="<%=fll.web.report.FinalComputedScores.ReportSelector.AWARD_GROUP.name()%>" />
            <select name="groupName">
                <option
                    value="<%=fll.web.report.FinalComputedScores.ALL_GROUP_NAME%>">All</option>
                <c:forEach items="${awardGroups}" var="awardGroup">
                    <option value="${awardGroup}">${awardGroup}</option>
                </c:forEach>
            </select>
            <select name="sortOrder">
                <c:forEach items="${sortOrders}" var="sortOrder">
                    <option value="${sortOrder}">Sort by
                        ${sortOrder}</option>
                </c:forEach>
            </select>
            <input type="submit" value="Report by Award Group" />
        </form>

        <c:if test="${awardGroups != judgingStations}">
            <form action="FinalComputedScores"
                target="_finalComputedScores" method="POST">
                <input type="hidden" name="selector"
                    value="<%=fll.web.report.FinalComputedScores.ReportSelector.JUDGING_STATION.name()%>" />
                <select name="groupName">
                    <option
                        value="<%=fll.web.report.FinalComputedScores.ALL_GROUP_NAME%>">All</option>
                    <c:forEach items="${judgingStations}"
                        var="judgingStation">
                        <option value="${judgingStation}">${judgingStation}</option>
                    </c:forEach>
                </select>
                <select name="sortOrder">
                    <c:forEach items="${sortOrders}" var="sortOrder">
                        <option value="${sortOrder}">Sort by
                            ${sortOrder}</option>
                    </c:forEach>
                </select>
                <input type="submit" value="Report by Judging Station" />
            </form>
        </c:if>
    </div>
    <!-- end FinalComputedScores -->

    <div class="wide">
        <form action="<c:url value='/report/AwardSummarySheet'/>"
            method="POST" target="award-summary-sheet">
            Award Summary Sheet
            <select name="groupName">
                <c:forEach items="${judgingStations}"
                    var="judgingStation">
                    <option value="${judgingStation}">${judgingStation}</option>
                </c:forEach>
            </select>
            <select name="sortOrder">
                <c:forEach items="${sortOrders}" var="sortOrder">
                    <option value="${sortOrder}">Sort by
                        ${sortOrder}</option>
                </c:forEach>
            </select>
            <input type='submit' value='Create Sheet' />
        </form>
    </div>

    <a class="wide"
        href="<c:url value='/report/CategoryScoresByScoreGroup'/>"
        target="_blank">Award category scores - by category and
        judging group</a>

    <a class='wide' href="<c:url value='/report/SubjectiveByJudge'/>"
        target="_blank">Final Computed Score Consolidated</a>

    <a class='wide'
        href="<c:url value='/report/ScaledSubjectiveByAwardGroup'/>"
        target="_blank">Subjective scaled scores by award group</a>

    <!-- VirtualSubjectiveCategoryReport -->
    <c:if
        test="${not empty challengeDescription.virtualSubjectiveCategories}">
        <c:forEach
            items="${challengeDescription.virtualSubjectiveCategories}"
            var="category">
            <a class="wide"
                href="<c:url
                    value='/report/VirtualSubjectiveCategoryReport' />?categoryName=${category.name}"
                target="_blank"> ${category.title} Score Comparison
            </a>
        </c:forEach>
    </c:if>
    <!-- end VirtualSubjectiveCategoryReport -->

    <a class="wide" href="<c:url value='/report/PerformanceReport'/>"
        target="_blank"> Performance scores - full tournament </a>

    <a class="wide"
        href="<c:url value='/report/PerformanceScoreReport'/>"
        target="_blank">Performance scores - by team </a>

    <!-- performance ranks -->
    <div class="wide">
        <form action="<c:url value='/scoreboard/dynamic.jsp'/>"
            method="POST" target="report-top10">
            Display performance scores with ranks
            <input type="hidden" name="layout" value="top_scores_all" />

            <select name="divisionIndex">
                <c:forEach items="${awardGroups}" var="awardGroup"
                    varStatus="loopStatus">
                    <option value="${loopStatus.index}">${awardGroup}</option>
                </c:forEach>

            </select>
            <input type='submit' value='Display Performance Ranks' />
        </form>
    </div>
    <!-- end performance ranks -->

    <a class="wide" href="<c:url value='/report/PlayoffReport'/>"
        target="_blank">Winners of each head to head bracket</a>

    <a class="wide"
        href="<c:url value='/report/non-numeric-nominees.jsp'/>"
        target="_blank">Entry: Optional Award Nominations <br />
        This is used to enter the teams that are up for consideration
        for the non-scored subjective categories. The information
        entered here transfers over to the finalist scheduling web
        application.
    </a>

    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp'/>"
        target="_blank">Award Winner Write-up </a>

    <a class="wide"
        href="<c:url value='/report/edit-advancing-teams.jsp'/>"
        target="_blank">Advancing Teams</a>

    <a class="wide"
        href="<c:url value='/report/awards/edit-awards-presenters.jsp'/>">Award
        Presenters</a>

    <a class="wide" href="<c:url value='/report/AwardsReport'/>"
        target="_blank">Award Winners Report</a>

    <a class="wide" href="<c:url value='/report/Awards.csv'/>">Export
        Awards List CSV file of award winners</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/awards/AwardsScriptReport'/>">Awards
        Script</a>

    <!-- Team Results -->
    <a class="wide" href="<c:url value='/report/TeamResults'/>"
        target="_blank">Team Rubrics. This is a zip file containing
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

    <div class="wide">
        <form action="<c:url value='/report/JudgeRubrics'/>"
            method='post' target='_blank'>
            Rubrics for a single judge
            <select name='category_name' id='rubric_category_name'>
                <c:forEach
                    items="${challengeDescription.subjectiveCategories}"
                    var="category">
                    <option value='${category.name}'>${category.title}</option>
                </c:forEach>
            </select>
            <select name='judge' id='rubric_judge'></select>

            <input type='submit' value='Get Rubrics' />
        </form>

    </div>

    <a class='wide'
        href='<c:url value="/report/edit-award-group-order.jsp"/>'>Award
        Group Order</a>


    <a class="wide"
        href="<c:url value='/report/edit-award-determination-order.jsp' />">Edit
        the order that awards are determined.</a>


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
            <form
                ACTION="<c:url value='/report/finalist/PdfFinalistSchedule'/>"
                METHOD='POST' target="_blank">
                <select name='division'>
                    <c:forEach var="division"
                        items="${finalistDivisions }">
                        <option value='${division }'>${division }</option>
                    </c:forEach>
                </select>
                <input type='submit' value='Finalist Schedule by Time' />
            </form>
        </div>
    </c:if>

    <a class="wide"
        href="<c:url value='/report/finalist/TeamFinalistSchedule'/>"
        target="_blank">Finalist Schedule for each team</a>

    <a class="wide"
        href="<c:url value='/report/deliberation/specify_category_order.jsp'/>"
        target="_blank">Specify Deliberation Category Order</a>

    <a class="wide"
        href="<c:url value='/report/deliberation/index.jsp'/>"
        target="_blank">Deliberations</a>

    <h2>Cross-tournament reports</h2>
    <p>Reports that use data across multiple tournaments.</p>

    <a class="wide"
        href="<c:url value='/report/TournamentAdvancement'/>"
        target="_blank">Tournament advancement report - this is a
        CSV file that contains information about the teams that are
        advancing from each tournament. For this to work all of the
        databases need to be merged and the award winners need to be
        specified for each tournament.</a>

    <h2>Other useful reports</h2>
    <p>Some reports that are handy for intermediate reporting and
        checking of the current tournament state.</p>

    <div class="wide">
        <form ACTION="<c:url value='/report/performanceRunReport.jsp'/>"
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
            action="<c:url value='/report/teamPerformanceReport.jsp'/>"
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

    <a class="wide" href="<c:url value='/report/unverifiedRuns.jsp'/>">Unverified
        runs. Unverified performance runs.</a>

    <a class="wide" href="PerformanceScoreDump">CSV file containing
        all performance scores, excluding byes. This is useful to
        manually determine awards for most consistent or most improved
        robot performance.</a>

</body>
</html>

<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
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

    <a class="wide" href="NonNumericNomineesReport" target="_blank">Optional
        award nominations (from subjective judging rubrics)</a>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="topScoreReportPerAwardGroup.jsp">Top performance -
        Award group (HTML)</a>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="TopScoreReportPerAwardGroupPdf">Top performance -
        Award group (PDF)</a>

    <a class="wide" target="_topScoreReportPerJudgingStation"
        href="topScoreReportPerJudgingStation.jsp"> Top performance
        - Judging group (HTML)</a>

    <a class="wide" target="_topScoreReportPerJudgingStation"
        href="TopScoreReportPerJudgingStationPdf"> Top performance -
        Judging group (PDF)</a>

    <div class="wide">
        Summarized numeric scores - by judging group aka "Final Computed
        Scores"
        <form action="FinalComputedScores" target="_finalComputedScores"
            method="POST">
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

        <form action="FinalComputedScores" target="_finalComputedScores"
            method="POST">
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
    </div>
    <!-- end FinalComputedScores -->

    <div class="wide">
        <form action="AwardSummarySheet" method="POST"
            target="award-summary-sheet">
            Generate award summary sheet for judging group
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

    <a class="wide" href="CategoryScoresByScoreGroup" target="_blank">Award
        category scores - by category and judging group</a>

    <a class='wide' href='SubjectiveByJudge' target="_blank">Summarized
        numeric scores - by judges </a>

    <a class='wide' href='ScaledSubjectiveByAwardGroup' target="_blank">Subjective
        scaled scores by award group</a>

    <!-- VirtualSubjectiveCategoryReport -->
    <c:if
        test="${not empty challengeDescription.virtualSubjectiveCategories}">
        <div class="wide">
            <form
                action="<c:url value='/report/VirtualSubjectiveCategoryReport'/>"
                method="POST" target="_blank">
                Detailed score report for virtual subjective score
                category

                <select name="categoryName">
                    <c:forEach
                        items="${challengeDescription.virtualSubjectiveCategories}"
                        var="category">
                        <option value="${category.name}">${category.title}</option>
                    </c:forEach>
                </select>

                <input type='submit' value='Create Report' />
            </form>
        </div>
    </c:if>
    <!-- end VirtualSubjectiveCategoryReport -->

    <a class="wide" href="PerformanceReport" target="_blank">
        Performance scores - full tournament </a>

    <a class="wide" href="PerformanceScoreReport" target="_blank">Performance
        scores - by team </a>

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

    <a class="wide" href="PlayoffReport" target="_blank">Winners of
        each head to head bracket</a>

    <a class="wide" href="non-numeric-nominees.jsp" target="_blank">Entry:
        Non-numeric nominations <br /> This is used to enter the teams
        that are up for consideration for the non-scored subjective
        categories. The information entered here transfers over to the
        finalist scheduling web application.
    </a>

    <a class="wide" href="edit-award-winners.jsp" target="_blank">Entry:
        Award winners </a>

    <a class="wide" href="edit-advancing-teams.jsp" target="_blank">Enter
        the teams advancing to the next level of tournament</a>

    <a class="wide"
        href="<c:url value='/report/awards/edit-awards-presenters.jsp'/>">Edit
        presenters for the awards ceremony.</a>

    <c:if test="${authentication.admin}">
        <a class="wide" href="<c:url value='/report/awards/index.jsp'/>">Edit
            awards report and awards script properties.</a>
    </c:if>

    <a class="wide" href="AwardsReport" target="_blank">Report of
        winners for the tournament. This can be published on the web.</a>

    <a class="wide" href="Awards.csv">CSV file of award winners.</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/awards/AwardsScriptReport'/>">Awards
        Script PDF </a>

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

    <div class="wide">
        <form action='JudgeRubrics' method='post' target='_blank'>
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
        href='<c:url value="/report/edit-award-group-order.jsp"/>'>Edit
        award group order</a>


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
                - Can be used for check-in
            </form>
        </div>
    </c:if>

    <a class="wide" href="finalist/TeamFinalistSchedule" target="_blank">Finalist
        Schedule for each team</a>

    <a class="wide" href="deliberation/specify_category_order.jsp"
        target="_blank">Specify Deliberation Category Order</a>

    <a class="wide" href="deliberation/index.jsp" target="_blank">Deliberations</a>

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

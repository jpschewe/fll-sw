<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext, false);
fll.web.report.ReportIndex.populateContext(application, session, pageContext, false);
%>

<html>
<head>
<title>Head Judge</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

</head>

<body>
    <h1>Head Judge</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>


    <h2>Deliberation Reports</h2>
    <a class="wide" target="_subjective"
        href="<c:url value='/subjective/Auth'/>"
        onclick="return openMinimalBrowser(this)">Judge Scoring</a>

    <!--  FIXME needs a page -->
    <a class="wide" target="_TeamSchedules"
        href='<c:url value="/report/team-schedules.jsp"/>'>Team
        Schedules</a>

    <a class="wide" href="<c:url value='/report/judge-summary.jsp'/>"
        target="_judge-summary">Judge summary. This shows which
        judges have scores entered.</a>

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
            <form action="<c:url value='/report/FinalComputedScores'/>"
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
        href="<c:url value='/report/NonNumericNomineesReport'/>"
        target="_blank">Optional Award Nominations</a>

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

    <a class='wide' href="<c:url value='/report/SubjectiveByJudge'/>"
        target="_blank">Final Computed Score Consolidated</a>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="<c:url value='/report/topScoreReportPerAwardGroup.jsp'/>">Robot
        Match Scores</a>

    <a class="wide" target="_additional_reports"
        href="<c:url value='/report/additional-reports.jsp'/>'">Additional
        Reports</a>

    <h2>Awards</h2>
    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp' />"
        target="_blank">Award Winner Write-up</a>

    <a class="wide"
        href="<c:url value='/report/edit-advancing-teams.jsp' />"
        target="_blank">Advancing Teams</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/awards/AwardsScriptReport'/>">Awards
        Script </a>

    <a class='wide'
        href='<c:url value="/report/edit-award-group-order.jsp"/>'>Award
        Group Order</a>

    <h2>
        <a class='wide' href="<c:url value='/head-judge-state.jsp'/>">STATE</a>
    </h2>

</body>
</html>

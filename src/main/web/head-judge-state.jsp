<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext, false);
fll.web.report.ReportIndex.populateContext(application, session, pageContext, false);
%>

<html>
<head>
<title>Head Judge State</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

</head>

<body>
    <h1>Head Judge Sate</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <h2>Preliminary Reports</h2>
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
        <form
            action="<c:url value='/report/FinalComputedScores' />"
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
            <form
                action="<c:url value='/report/FinalComputedScores'/>"
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

    <h2>Finalist Scheduling</h2>

    <div>
        <b>**Warning**</b>
        Before visiting the links below, all subjective scores need to
        be uploaded and any head to head brackets that will occur during
        the finalist judging should be created to avoid scheduling
        conflicts.
    </div>

    <a class="wide"
        href="<c:url value='/report/non-numeric-nominees.jsp'/>"
        target="_blank">Optional Award Nominations <br /> This is
        used to enter the teams that are up for consideration for the
        non-scored subjective categories. The information entered here
        transfers over to the finalist scheduling web application.
    </a>

    <a class="wide" href="finalist/load.jsp" target="_blank">Schedule
        Finalists</a>

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

    <h2>Awards</h2>

    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp'/>"
        target="_blank">Award Winner Write-up </a>

    <a class="wide"
        href="<c:url value='/report/edit-advancing-teams.jsp'/>"
        target="_blank">Advancing Teams</a>

    <a class="wide"
        href="<c:url value='/report/awards/edit-awards-presenters.jsp'/>">Award
        Presenters</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/awards/AwardsScriptReport'/>">Awards
        Script</a>

    <a class="wide" href="<c:url value='/report/AwardsReport'/>"
        target="_blank">Award Winners Report</a>

    <a class='wide'
        href='<c:url value="/report/edit-award-group-order.jsp"/>'>Award
        Group Order</a>


</body>
</html>

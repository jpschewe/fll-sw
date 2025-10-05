<!DOCTYPE html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.web.report.ReportIndex.populateContext(application, session, pageContext, false);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext, false);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Team Schedules</title>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js' />"></script>

</head>

<body>
    <h1>Team Schedules</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <a class="wide"
        href="<c:url value='/admin/SubjectiveScheduleByTime'/>"
        target="_blank">Judging Schedule</a>

    <a class="wide"
        href="<c:url value='/admin/SubjectiveScheduleByCategory'/>"
        target="_blank">Judging Schedule by Category</a>


    <a class="wide" href="<c:url value='/admin/ScheduleByTeam'/>"
        target="_blank">Full schedule sorted by team</a>

    <a class="wide" href="<c:url value='/admin/ScheduleByWaveAndTeam'/>"
        target="_blank">Full schedule sorted by wave and team</a>

    <div class="wide">
        <form action="<c:url value='/admin/TeamSchedules' />"
            method="post" target="_blank">
            Team schedule for
            <select name='TeamNumber'>
                <c:forEach items="${tournamentTeams}" var="team">
                    <option value='<c:out value="${team.teamNumber}"/>'>
                        <c:out value="${team.teamNumber}" /> -
                        <c:out value="${team.teamName}" />
                    </option>
                </c:forEach>
            </select>
            <input type='submit' value='Output Schedule' />
        </form>
    </div>

</body>
</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REPORT_GENERATOR" allowSetup="false" />

<%
fll.web.report.ReportIndex.populateContext(application, session, pageContext, false);
%>

<html>
<head>
<title>Tournament Reporter</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type='text/javascript' src="<c:url value='/report/index.js'/>"></script>

<script type='text/javascript'>
  var categoryJudges = JSON.parse('${categoryJudgesJson}');
</script>

</head>

<body>
    <h1>Tournament Reporter</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <a class="wide" target="_topScoreReportPerAwardGroup"
        href="<c:url value='/report/TopScoreReportPerAwardGroupPdf'/>">Top
        Robot Match Scores (PDF)</a>

    <a class="wide" href="<c:url value='/report/TeamResults'/>"
        target="_blank">Team Rubrics. This is a zip file containing
        the results to return to the teams. This will take some time to
        generate, be patient.</a>

    <a class="wide" href="<c:url value='/report/AwardsReport'/>"
        target="_blank">Awards Report for Website</a>

    <a class="wide" href="<c:url value='/report/Awards.csv'/>">Export
        Awards List CSV file of award winners</a>

    <a class="wide" target="_report"
        href="<c:url value='/report/awards/AwardsScriptReport'/>">Awards
        Script</a>

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

</body>
</html>
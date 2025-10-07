<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext, false);
fll.web.JudgeIndex.populateContext(application, pageContext);
%>

<html>
<head>
<title>Judge Links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>


</head>

<body>
    <h1>Judge links</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <p>
        The current tournament is
        <b>${currentTournament.description} on
            ${currentTournament.dateString} [${currentTournament.name}]</b>
    </p>

    <a class="wide" target="_subjective"
        href="<c:url value='/subjective/Auth' />"
        onclick="return openMinimalBrowser(this)">Judge Scoring</a>

    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp' />"
        target="_blank">Award Winner Write Up</a>

    <div class="wide">
        <form method="POST"
            action="<c:url value='/report/SubjectiveScoreRubrics'/>">
            Generate the rubrics with scores for the specified category
            and award group
            <select name='categoryName'>
                <c:forEach
                    items="${challengeDescription.subjectiveCategories}"
                    var="category">
                    <option value='${category.name}'>${category.title}</option>
                </c:forEach>
            </select>

            <select name='awardGroup'>
                <c:forEach items="${awardGroups}" var="awardGroup">
                    <option value='${awardGroup}'>${awardGroup}</option>
                </c:forEach>
            </select>

            <input type='submit' value='Generate PDFs' />

        </form>
    </div>

    <a class="wide" href="<c:url value='/report/SubjectiveByJudge'/> ">Summarized
        numeric scores - by judges</a>

</body>
</html>

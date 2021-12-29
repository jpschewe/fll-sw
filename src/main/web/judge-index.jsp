<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.JudgeIndex.populateContext(application, pageContext);
%>

<html>
<head>
<title>Judge Links</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
    <h1>Judge links</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>
        The current tournament is
        <b>${currentTournament.description} on
            ${currentTournament.dateString} [${currentTournament.name}]</b>
    </p>

    <h2>Server addresses</h2>
    <p>These are the addresses that can be used on the judges
        electronic devices to connect to this server.</p>
    <ul>
        <c:forEach items="${urls}" var="url">
            <li class="no-marker">
                <a class="wide" href="${url }">${url }</a>
            </li>
        </c:forEach>
    </ul>

    <a class="wide" target="_subjective"
        href="<c:url value='/subjective/Auth' />">Enter subjective
        scores. This is done through the subjective web application</a>.

    <a class="wide"
        href="<c:url value='/report/edit-award-winners.jsp' />"
        target="_blank">Enter the winners of awards for use in the
        awards report</a>

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

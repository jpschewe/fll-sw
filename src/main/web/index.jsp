<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>


<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>${challengeDescription.title }</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>

    <h2>Main links</h2>

    <a class="wide" href="<c:url value='/display'/>">Big Screen
        Display - follow this link on the computer that's used to
        display scores with the projector.</a>


    <c:if test="${authentication.headJudge}">
        <a class="wide" href="<c:url value='/head-judge.jsp' />">Head
            Judge</a>
    </c:if>

    <c:if test="${authentication.ref}">
        <a class="wide" href="<c:url value='/ref-index.jsp' />">Ref</a>
    </c:if>

    <c:if test="${authentication.judge}">
        <a class="wide" href="<c:url value='/judge-index.jsp' />">Judge</a>
    </c:if>

    <c:if test="${authentication.admin}">
        <a class="wide"
            href="<c:url value='/admin/performance-area.jsp' />">Scoring
            Coordinator</a>
    </c:if>

    <a class="wide" href="public">Public landing page. A list of
        pages that the public may want to visit if they are allowed to
        connect to the network.</a>

    <c:if test="${authentication.admin}">
        <h2>Pages for the head computer person</h2>
        <a class="wide" href="documentation/index.html">Documentation</a>

        <a class="wide" href="troubleshooting/index.jsp">Troubleshooting</a>
        <a class="wide" href="<c:url value='/setup'/>">Database
            setup - Use this link to upload a saved database</a>

        <a class="wide" href="admin/index.jsp">Administration</a>

        <a class="wide" href="playoff/index.jsp">Head to head</a>
    </c:if>
    <c:if
        test="${authentication.admin or authentication.reportGenerator or authentication.headJudge}">
        <a class="wide" href="report/index.jsp">Generate reports</a>
        <a class="wide" href='challenge.xml'>Challenge Descriptor.
            One can use this to see the scoring and the tie breaker.</a>
        <a class="wide" href='BlankScoresheet' target="_blankScoresheet">Blank
            performance score sheet, one regular match play and one
            practice (PDF)</a>
        <c:forEach items="${challengeDescription.subjectiveCategories}"
            var="category">
            <a class="wide" href="BlankSubjectiveSheet/${category.name}"
                target="_new">Blank subjective sheet -
                ${category.title} (PDF)</a>
        </c:forEach>
    </c:if>

    <h2>Other useful pages</h2>

    <a class="wide" href='scoreboard/index.jsp'>Score board</a>

    <c:if test="${authentication.admin}">
        <a class="wide" href="developer/index.jsp">Developer page</a>
    </c:if>

    <c:if test="${authentication.admin}">
        <a class="wide"
            href="<c:url value='/scoreEntry/scoreEntry.jsp?tablet=true&practice=true&showScores=true'/>">Practice
            score entry to publish</a>
    </c:if>

    <a class="wide" href="credits/credits.jsp">Credits</a>

</body>
</html>

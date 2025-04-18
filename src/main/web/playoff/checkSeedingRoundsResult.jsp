<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.CheckSeedingRounds.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Team Playoff check</title>
</head>

<body>
    <h1>Check Regular Match Play Rounds</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />


    <h2>Team Playoff check</h2>

    <c:choose>
        <c:when test="${empty teamsNeedingSeedingRuns}">
            <p>No teams are missing regular match play rounds.</p>
        </c:when>
        <c:otherwise>
            <p>Teams missing some regular match play rounds. Teams
                with no runs are excluded from this check.</p>
            <ul>
                <c:forEach items="${teamsNeedingSeedingRuns }"
                    var="team">
                    <li class='warning'>${team.teamName}&nbsp;(${team.teamNumber })</li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>

    <p>
        <a href="index.jsp">Back to Playoff menu</a>
    </p>


</body>
</html>
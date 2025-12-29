<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.PageVariables.populateTournamentTeams(application, pageContext);
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
    Below are listed the web pages that are available to the public.

    <c:if test="$not empty ScorePageText}">
        <div class="wide">${ScorePageText }</div>
    </c:if>

    <div class="wide">
        Current tournament -&gt;
        <b>${tournamentData.currentTournament.description} on
            ${tournamentData.currentTournament.dateString}
            [${tournamentData.currentTournament.name}]</b>
    </div>

    <a class="wide" href='<c:url value="/welcome.jsp"/>'>Welcome
        Page</a>
    <a class="wide" href='<c:url value="/scoreboard/dynamic.jsp" />'>Performance
        Scoreboard</a>

    <a class="wide"
        href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="all_teams_auto_scroll"/></c:url>'>All
        Teams, All Performance Runs</a>

    <a class="wide"
        href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="most_recent"/></c:url>'>Most
        recent performance scores</a>

    <a class="wide"
        href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="top_scores"/></c:url>'
        target="_blank">Top performance scores</a>

    <a class="wide" href='<c:url value="/playoff/remoteMain.jsp"/>'>Head
        to head brackets that are currently on the big screen</a>

    <div class="wide">

        <form action="<c:url value='/admin/TeamSchedules' />"
            method='post' target="_new">
            Team schedule for
            <select name='TeamNumber'>
                <c:forEach items="${tournamentTeams}" var="team">
                    <option value='${team.teamNumber}'>
                        ${team.teamNumber} - ${team.teamName}</option>
                </c:forEach>
            </select>
            <input type='submit' value='Output Schedule' />
        </form>
    </div>

    <a class="wide"
        href='<c:url value="/report/finalist/FinalistTeams.jsp"/>'>
        Finalist Teams (State only)</a>

    <a class="wide" href='<c:url value="/credits/credits.jsp"/>'>Credits</a>

</body>

</html>

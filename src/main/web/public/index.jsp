<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<%
fll.web.PublicIndex.populateContext(application, pageContext);
%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>${challengeDescription.title }</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    Below are listed the web pages that are available to the public.

    <c:if test="$not empty ScorePageText}">
        <div class="wide">${ScorePageText }</div>
    </c:if>

    <div class="wide">Current Tournament -&gt; ${tournamentName }</div>

    <a class="wide" href='<c:url value="/welcome.jsp"/>'>Welcome
        Page</a>
    <a class="wide" href='<c:url value="/scoreboard/main.jsp" />'>Performance
        Scoreboard</a>

    <a class="wide" href='<c:url value="/scoreboard/allteams.jsp"/>'>All
        Teams, All Performance Runs</a>

    <a class="wide" href='<c:url value="/scoreboard/Last8"/>'>Most
        recent performance scores</a>

    <a class="wide" href='<c:url value="/scoreboard/Top10"/>'>Top
        performance scores</a>

    <a class="wide" href='<c:url value="/playoff/remoteMain.jsp"/>'>Head
        to head brackets that are currently on the big screen</a>
    <a class="wide"
        href='<c:url value="/report/finalist/FinalistTeams.jsp"/>'>
        Finalist Teams (State only)</a>

    <a class="wide" href='<c:url value="/credits/credits.jsp"/>'>Credits</a>

</body>

</html>

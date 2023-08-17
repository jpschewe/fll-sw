<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Scoreboard</title>
</head>
<body>
    <h1>Scoreboard</h1>
    <ul>
        <li>
            <a href='<c:url value="main.jsp" />' target="_blank">Primary
                Scoreboard (1024x768)</a>
        </li>
        <li>
            <a href='<c:url value="/scoreboard/main_small.jsp" />'
                target="_blank">Primary Scoreboard (800x600)</a>
        </li>

        <li>
            <a
                href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="all_teams_auto_scroll"/></c:url>'
                target="_blank">All Teams, All Runs</a>
        </li>
        <li>
            <a
                href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="all_teams_no_scroll"/></c:url>'
                target="_blank">All Teams, All Runs (scrolling)</a>
        </li>
        <li>
            <a href='<c:url value="MostRecent"/>' target="_blank">Most
                Recent scores</a>
        </li>
        <li>
            <a href='<c:url value="Top10"/>' target="_blank">Top 10
                scores</a>
        </li>

        <li>
            <a
                href='<c:url value="/scoreboard/dynamic.jsp"><c:param name="layout" value="all_teams_top_scores"/></c:url>'
                target="_blank">All teams and all top scores</a>
        </li>

        <li>
            <a
                href='<c:url value="/report/finalist/FinalistTeams.jsp"/>'
                target="_blank">Teams in finalist judging</a>
        </li>

    </ul>

</body>
</html>

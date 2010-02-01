<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<HTML>
  <HEAD>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Scoreboard</title>
  </HEAD>
  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Scoreboard)</h1>
    <ul>
      <li><a href='<c:url value="main.jsp" />'>Primary Scoreboard (1024x768)</a></li>
      <li><a href='<c:url value="/scoreboard_800/main.jsp" />'>Primary Scoreboard
            (800x600)</a></li>
        
      <li><a href='<c:url value="allteams.jsp"/>'>All Teams, All Runs (primarily for internal use)</a></li>
      <li><a href='<c:url value="Last8"/>'>Last 8 scores</a></li>
      <li><a href='<c:url value="Top10"/>'>Top 10 scores</a></li>
        
    </ul>

  </BODY>
</HTML>

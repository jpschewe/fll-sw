<%@ include file="/WEB-INF/jspf/init.jspf"%>

<% fll.web.report.ReportIndex.populateContext(application, pageContext); %>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Reporting</title>
</head>

<body>
 <h1>Reporting</h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <ol>
  <li><a href="SummarizePhase1">Compute summarized scores</a>. This
   needs to be executed before any reports can be generated. You will be
   returned to this page if there are no errors summarizing scores.</li>

  <li><a href='finalComputedScores.pdf'>Final Computed Scores</a>.
   This is the report that the head judge will want to determine which
   teams advance to the next tournament.</li>

  <li><a href="CategorizedScores">Categorized Scores</a>. This
   shows the top teams in each category after standardization.</li>

  <li><a href="CategoryScoresByJudge">Categorized Scores by
    judge</a>. This shows the top teams for each judge. This is useful for
   checking the winners of each category when there is only 1 judge for
   each team in a category.</li>

  <li><a href="CategoryScoresByScoreGroup">Categorized Scores
    by Judging Station</a>. This displays the scaled scores for each
   category by judging station. This is useful for checking the winners
   of each category when there is more than 1 judge for each team in a
   category.</li>

  <li><a href="finalist/load.jsp">Schedule Finalists</a>. This is
   used when one judge doesn't see all teams in a division and the top
   teams need to be judged again to choose the winners.</li>

  <li>
   <form ACTION='finalist/PrivateFinalistSchedule' METHOD='POST'>
    <select name='division'>
     <c:forEach var="division" items="${finalistDivisions }">
      <option value='${division }'>${division }</option>
     </c:forEach>
    </select> <input type='submit' value='Private Finalist Schedule' /> This
    displays the finalist schedule for all categories.
   </form>
  </li>

  <li>
   <form ACTION='finalist/PublicFinalistSchedule' METHOD='POST'>
    <select name='division'>
     <c:forEach var="division" items="${finalistDivisions }">
      <option value='${division }'>${division }</option>
     </c:forEach>
    </select> <input type='submit' value='Public Finalist Schedule' /> This
    displays the finalist schedule for public categories.
   </form>
  </li>

  <li><a href="RankingReport">Ranking Report for teams</a>. This is
   printed at the end of the day and each team gets their page.</li>

  <li><a href="PlayoffReport">Winners of each playoff bracket</a>.
   This is useful for the awards ceremony.</li>

 </ol>

 <p>Some reports that are handy for intermediate reporting and
  checking of the current tournament state.</p>

 <ul>
  <li>
   <form ACTION='performanceRunReport.jsp' METHOD='POST'>
    Show scores for performance run <select name='RunNumber'>
     <c:forEach var="index" begin="1" end="${maxRunNumber}">
      <option value='${index }'>${index }</option>
     </c:forEach>
    </select> <input type='submit' value='Show Scores' />
   </form>
  </li>

  <li>
   <form action='teamPerformanceReport.jsp' method='post'>
    Show performance scores for team <select name='TeamNumber'>
     <c:forEach items="${tournamentTeams}" var="team">
      <option value='<c:out value="${team.teamNumber}"/>'>
       <c:out value="${team.teamNumber}" />
       -
       <c:out value="${team.teamName}" />
      </option>
     </c:forEach>
    </select> <input type='submit' value='Show Scores' />
   </form>
  </li>

  <li><a href="unverifiedRuns.jsp">Unverified runs</a></li>

 </ul>


</body>
</html>

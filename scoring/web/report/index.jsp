<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Connection"%>

<%
  final DataSource datasource = SessionAttributes.getDataSource(session);
  final Connection connection = datasource.getConnection();
  final Statement stmt = connection.createStatement();
  final ResultSet rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = "
      + Queries.getCurrentTournament(connection));
  final int maxRunNumber;
  if (rs.next()) {
    maxRunNumber = rs.getInt(1);
  } else {
    maxRunNumber = 1;
  }
  pageContext.setAttribute("maxRunNumber", maxRunNumber);
  SQLFunctions.close(rs);
  SQLFunctions.close(stmt);
%>
<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Reporting</title>
</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
  (Reporting)
 </h1>

 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <ol>
  <li><a href="summarizePhase1.jsp">Compute summarized scores</a>.
   This needs to be executed before any reports can be generated. You
   will be returned to this page if there are no errors summarizing
   scores.</li>

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

  <li><a href="RankingReport">Ranking Report for teams</a>. This is
   printed at the end of the day and each team gets their page.</li>

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
    </select> <input type='submit' value='Show Scores'>
   </form>
  </li>

  <li>
   <form action='teamPerformanceReport.jsp' method='post'>
    Show performance scores for team <select name='TeamNumber'>
     <%
       pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values());
     %>
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

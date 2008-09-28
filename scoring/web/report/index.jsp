<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%@ page import="fll.db.Queries" %>

<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>
  
<%
final StringBuffer message = new StringBuffer();
final String messageReq = request.getParameter("message");
if(null != messageReq) {
  message.append("<i>");
  message.append(messageReq);
  message.append("</i><br>");
}

final Connection connection = (Connection)application.getAttribute("connection");
final Statement stmt = connection.createStatement();
final ResultSet rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = '" + Queries.getCurrentTournament(connection) + "'");
final int maxRunNumber;
if(rs.next()) {
  maxRunNumber = rs.getInt(1);
} else {
  maxRunNumber = 1;
}
SQLFunctions.closeResultSet(rs);
SQLFunctions.closeStatement(stmt);
%>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Reporting)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Reporting)</h1>

    <p><%=message.toString()%></p>

    <ol>
      <li><a href="summarizePhase1.jsp">Compute summarized scores</a>.  This
      needs to be executed before any reports can be generated.  You will be
      returned to this page if there are no errors summarizing scores.</li>

      <li><a href='<c:url value="/GetFile">
                     <c:param name="filename" value="finalComputedScores.pdf"/>
                   </c:url>' target='_new'>Final Computed Scores</a>
        </li>

      <li><a href="categorizedScores.jsp">Categorized Scores</a></li>

      <li><a href="categoryScoresByJudge.jsp">Categorized Scores by judge</a></li>

      <li><a href="CategoryScoresByScoreGroup">Categorized Scores by Score Group</a>.  This displays the scaled scores for each category by score group (all judges that saw a team).</li>

      <li><a href="promptForNumFinalists.jsp">Schedule Finalists</a></li>
      
 <li><a href="RankingReport">Ranking Report for teams</a></li>        

    </ol>

    <p>Some reports that are handy for intermediate reporting and
    checking of the current tournament state.</p>
      
    <ul>
      <li>
        <form ACTION='performanceRunReport.jsp' METHOD='POST'>
        Show scores for performance run <select name='RunNumber'>
<% for(int i=0; i<maxRunNumber; i++) { %>
  <option value='<%=(i+1)%>'><%=(i+1)%></option>
<% } %>
        </select>
        <input type='submit' value='Show Scores'>
        </form>
      </li>

      <li>
        <form action='teamPerformanceReport.jsp' method='post'>
          Show performance scores for team <select name='TeamNumber'>
            <% pageContext.setAttribute("tournamentTeams", Queries.getTournamentTeams(connection).values()); %>
            <c:forEach items="${tournamentTeams}" var="team">
              <option value='<c:out value="${team.teamNumber}"/>'><c:out value="${team.teamNumber}"/> - <c:out value="${team.teamName}"/></option>
            </c:forEach>
          </select>
          <input type='submit' value='Show Scores'/>
        </form>
      </li>
      
      <li><a href="unverifiedRuns.jsp">Unverified runs</a></li>
        
    </ul>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

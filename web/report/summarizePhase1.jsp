
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.ScoreStandardization" %>
<%@ page import="fll.Queries" %>
<%@ page import="fll.Utilities" %>
      
<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.util.Map" %>

<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Connection" %>
  
<%
Queries.ensureTournamentTeamsPopulated(application);
      
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final Map tournamentTeams = (Map)application.getAttribute("tournamentTeams");
final String currentTournament = (String)application.getAttribute("currentTournament");
  
Queries.updateScoreTotals(challengeDocument, connection, currentTournament);
ScoreStandardization.setSubjectiveScoreGroups(connection, challengeDocument, currentTournament, tournamentTeams.values());
ScoreStandardization.summarizeSubjectiveScores(connection, challengeDocument, currentTournament);
ScoreStandardization.summarizePerformanceScores(connection, currentTournament);

final Statement stmt = connection.createStatement();
final ResultSet rs = stmt.executeQuery("SELECT ScoreGroup,Category,COUNT(RawScore)"
  + " FROM SummarizedScores"
  + " WHERE Category <> 'Performance'"
  + " AND Tournament = '" + currentTournament + "'"
  + " GROUP BY ScoreGroup ORDER BY Category");
%>

<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Summarize Scores)</title>
  </head>

  <body background="../images/bricks1.gif" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Summarize Scores)</h1>

    <p>Below is a list of score groups found and what categories they scored.
    Remember that score group names are created by joining together the ids of
    the judges by an '_'.</p>
          
    <table border='1'>
      <tr>
        <th>ScoreGroup</th>
        <th>Category</th>
        <td>Num Teams Scored</th>
      </tr>
<%
while(rs.next()) {
  final String scoreGroup = rs.getString(1);
  final String category = rs.getString(2);
  final String numTeams = rs.getString(3);
%>
      <tr>
        <td><%=scoreGroup%></td>
        <td><%=category%></td>
        <td><%=numTeams%></td>
      <tr>        
<%
}
Utilities.closeResultSet(rs);
Utilities.closeStatement(stmt);
%>

    </table>
        
    <p>If these look correct, <a href="summarizePhase2.jsp">continue</a> on to
    the second phase of computing the scores.  This page will return you to
    the reporting menu if everything is ok.</p>
        
<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

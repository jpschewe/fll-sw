<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.report.CategoryScores" %>
<%@ page import="fll.Queries" %>
      
<%@ page import="java.sql.Connection" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final String tournamentReq = request.getParameter("currentTournament");
final String tournament;
if(tournamentReq == null) {
  tournament = Queries.getCurrentTournament(connection);
} else {
  tournament = tournamentReq;
}
%>

<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Categorized Scores)</title>
  </head>

  <body>

<% CategoryScores.generateReport(tournament, challengeDocument, connection, out); %>

  </body>
</html>

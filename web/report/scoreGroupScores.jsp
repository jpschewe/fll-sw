<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.report.ScoreGroupScores" %>
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
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Categorized Scores)</title>
  </head>

  <body>

<% ScoreGroupScores.generateReport(tournament, challengeDocument, connection, out); %>

  </body>
</html>

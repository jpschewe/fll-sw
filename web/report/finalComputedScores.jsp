<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.web.report.FinalComputedScores" %>
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
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Final Computed Scores)</title>
    <style type="text/css">
    td {font-size: 12px}
    th {font-size: 12px}
    .warn { color: red; font-weight: bold }
    </style>
  </head>

  <body>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Final Computed Scores)</h1>

<% FinalComputedScores.generateReport(tournament, challengeDocument, connection, out); %>

  </body>
</html>

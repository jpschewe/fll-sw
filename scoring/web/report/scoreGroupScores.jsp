<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.web.report.ScoreGroupScores" %>
      
<%@ page import="java.sql.Connection" %>

<%@ page import="org.w3c.dom.Document" %>
  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final String tournamentReq = request.getParameter("currentTournament");
final String tournament;
if(null == null) {
  tournament = (String)application.getAttribute("currentTournament");
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

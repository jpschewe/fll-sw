<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.ScoreStandardization" %>
<%@ page import="fll.Queries" %>
      
<%@ page import="org.w3c.dom.Document" %>

<%
Queries.ensureTournamentTeamsPopulated(application);
      
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = Queries.getCurrentTournament(connection);
  
ScoreStandardization.standardizeScores(connection, challengeDocument, currentTournament);
ScoreStandardization.updateTeamTotalScores(connection, currentTournament);
final String errorMsg = ScoreStandardization.checkDataConsistency(connection);

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument//@title"/> (Summarize Scores)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument//@title"/> (Summarize Scores)</h1>

<%if(null == errorMsg) {%>
  <a href="index.jsp">Normally you'd be redirected here</a>
  <% response.sendRedirect(response.encodeRedirectURL("index.jsp")); %>
<%} else {%>
<p><font color='red'><%=errorMsg%></font></p>
<%}%>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

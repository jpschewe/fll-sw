<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Queries" %>
<%@ page import="fll.Team" %>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%@ page import="java.sql.Connection" %>
  
<%@ page import="org.w3c.dom.Document" %>
  
<%
Queries.ensureTournamentTeamsPopulated(application);

final Map tournamentTeams = (Map)application.getAttribute("tournamentTeams");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final String currentTournament = (String)application.getAttribute("currentTournament");
final Connection connection = (Connection)application.getAttribute("connection");
%>
  
<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Team Playoff check)</title>
  </head>

  <body background="../images/bricks1.gif" bgcolor="#ffffff" topmargin='4'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Team Playoff check)</h1>
      
      <p>Teams with less runs than seeding rounds.  This will not show teams
      that have no runs.
        <ul>
<%
final List less = Queries.getTeamsNeedingSeedingRuns(connection, currentTournament, tournamentTeams);
final Iterator lessIter = less.iterator();
while(lessIter.hasNext()) {
  final Team team = (Team)lessIter.next();
  out.println("<li>" + team.getTeamName() + "(" + team.getTeamNumber() + ")</li>");
}
%>
        </ul>
      </p>

      <p>Teams with more runs than seeding rounds:
        <ul>
<%
final List more = Queries.getTeamsWithExtraRuns(connection, currentTournament, tournamentTeams);
final Iterator moreIter = more.iterator();
while(moreIter.hasNext()) {
  final Team team = (Team)moreIter.next();
  out.println("<li>" + team.getTeamName() + "(" + team.getTeamNumber() + ")</li>");
}
%>
        </ul>
      </p>      

<%@ include file="../WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

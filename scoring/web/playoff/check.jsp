<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Queries" %>
<%@ page import="fll.Team" %>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%@ page import="java.sql.Connection" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
final Map tournamentTeams = Queries.getTournamentTeams(connection);
final String division = request.getParameter("division");
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Team Playoff check)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/></h1>
    <h2>Team Playoff check
    <%if("__all__".equals(division)) {%>
      [All divisions]
    <%} else {%>
      [Division: <%=division%>
    <%} %></h2>
      <p>Teams with less runs than seeding rounds. This will not show teams
      that have no runs.
        <ul>
<%
final List less = Queries.getTeamsNeedingSeedingRuns(connection, tournamentTeams, division);
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
final List more = Queries.getTeamsWithExtraRuns(connection, tournamentTeams, division);
final Iterator moreIter = more.iterator();
while(moreIter.hasNext()) {
  final Team team = (Team)moreIter.next();
  out.println("<li>" + team.getTeamName() + "(" + team.getTeamNumber() + ")</li>");
}
%>
        </ul>
      </p>
      <p><a href="index.jsp">Back to Playoff menu</a></p>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

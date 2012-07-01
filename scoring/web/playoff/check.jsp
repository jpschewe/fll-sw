<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.Team"%>

<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Map"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="javax.sql.DataSource"%>

<%
  final DataSource datasource = ApplicationAttributes.getDataSource();
  final Connection connection = datasource.getConnection();
  final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
  final String division = request.getParameter("division");
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Team Playoff check</title>
</head>

<body>
 <h1>
  <x:out select="$challengeDocument/fll/@title" />
 </h1>
 <h2>
  Team Playoff check
  <%
   if ("__all__".equals(division)) {
 %>
  [All divisions]
  <%
   } else {
 %>
  [Division:
  <%=division%>]
  <%
   }
 %>
 </h2>

 <%
   if (Queries.isPlayoffDataInitialized(connection, division)) {
 %>
 <p>Playoffs have already been initialized for this division.</p>
 <%
   } else {
 %>
 <p>Teams with fewer runs than seeding rounds. Teams with no runs
  are excluded from this check.</p>
 <ul>
  <%
    final List<Team> less = Queries.getTeamsNeedingSeedingRuns(connection, tournamentTeams, division, true);
      final Iterator<Team> lessIter = less.iterator();
      if (lessIter.hasNext()) {
        while (lessIter.hasNext()) {
          final Team team = lessIter.next();
          out.println("<li>"
              + team.getTeamName() + "(" + team.getTeamNumber() + ")</li>");
        }
      } else {
        out.println("<i id='no_teams_fewer'>No teams have fewer runs than seeding rounds.</i>");
      }
  %>
 </ul>

 <p>Teams with more runs than seeding rounds:</p>
 <ul>
  <%
    final List<Team> more = Queries.getTeamsWithExtraRuns(connection, tournamentTeams, division, true);
      final Iterator<Team> moreIter = more.iterator();
      if (moreIter.hasNext()) {
        while (moreIter.hasNext()) {
          final Team team = moreIter.next();
          out.println("<li>"
              + team.getTeamName() + "(" + team.getTeamNumber() + ")</li>");
        }
      } else {
        out.println("<i id='no_teams_more'>No teams have more runs than seeding rounds.</i>");
      }
  %>
 </ul>
 <%
   }
 %>
 <p>
  <a href="index.jsp">Back to Playoff menu</a>
 </p>


</body>
</html>

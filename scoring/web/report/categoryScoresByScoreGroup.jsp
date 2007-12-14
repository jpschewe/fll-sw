<!-- display final scores for each category and score group -->

<%@ include file="/WEB-INF/jspf/init.jspf" %>
<%@ page import="fll.db.Queries" %>
      
<%@ page import="java.sql.Connection" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
final String tournamentReq = request.getParameter("currentTournament");
final String tournament;
if(tournamentReq == null) {
  tournament = Queries.getCurrentTournament(connection);
} else {
  tournament = tournamentReq;
}
pageContext.setAttribute("currentTournament", tournament);
pageContext.setAttribute("divisions", Queries.getDivisions(connection));

%>

<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Categorized Scores)</title>
  </head>

  <body>

    <h1>FLL Categorized Score Summary by score group</h1>
    <p>Categories with multiple judges will have two scores for each team, 
    one for each judge, although the scores are the same.  This is a limitation
    of how the report is generated and may be fixed in a later release.</p> 
    <hr/>
    <h2>Tournament: <c:out value="${currentTournament}"/></h2>
    <c:forEach items="${divisions}" var="division">
      <x:forEach select="$challengeDocument/fll/subjectiveCategory">
        <x:set var="category" select="string(./@name)"/>
        <sql:query var="judges" dataSource="${datasource}">
          SELECT DISTINCT ${category}.Judge FROM ${category}, current_tournament_teams
            WHERE ${category}.TeamNumber = current_tournament_teams.TeamNumber
            AND ${category}.Tournament = '${currentTournament}'
            AND current_tournament_teams.event_division = '${division}'
        </sql:query>
        <c:forEach var="judgeRow" items="${judges.rows}">
          <h3><x:out select="./@title"/> Division: ${division} Score Group: ${judgeRow.Judge}</h3>
          <table border='0'>
            <tr><th colspan='3'>Team # / Organization / Team Name</th><th>Scaled Score</th></tr>
            <sql:query var="scores" dataSource="${datasource}">
              SELECT
                 Teams.TeamNumber
                ,Teams.Organization
                ,Teams.TeamName
                ,FinalScores.${category}
                FROM Teams, FinalScores, ${category}
                WHERE Teams.TeamNumber = FinalScores.TeamNumber
                AND Teams.TeamNumber = ${category}.TeamNumber
                AND ${category}.Tournament = '${currentTournament}'
                AND FinalScores.Tournament = '${currentTournament}'
                AND ${category}.Judge = '${judgeRow.Judge}'
                AND ${category}.ComputedTotal IS NOT NULL
                ORDER BY FinalScores.${category} DESC
            </sql:query>
            <c:forEach var="row" items="${scores.rowsByIndex}">
              <tr>
                <td><c:out value="${row[0]}"/></td>
                <td><c:out value="${row[1]}"/></td>
                <td><c:out value="${row[2]}"/></td>
                <td><fmt:formatNumber value="${row[3]}" maxFractionDigits="2" minFractionDigits="2"/></td>
              </tr>
              </tr>
            </c:forEach>
            <tr><td colspan='5'><hr</td></tr>
          </table>
        </c:forEach>  <!-- end foreach judge -->
      </x:forEach> <!-- end foreach category -->
    </c:forEach> <!-- end foreach division -->
  </body>
</html>

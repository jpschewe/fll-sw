<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>
      
<%@ page import="java.sql.Connection" %>

<%@ page import="org.w3c.dom.Document" %>
  
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

    <h1>FLL Categorized Score Summary by judge</h1>
    <hr/>
    <h2>Tournament: <c:out value="${currentTournament}"/></h2>
    <c:forEach items="${divisions}" var="division">
      <x:forEach select="$challengeDocument/fll/subjectiveCategory">
        <x:set var="category" select="string(./@name)"/>
        <sql:query var="judges" dataSource="${datasource}">
          SELECT DISTINCT ${category}.Judge FROM ${category}, current_tournament_teams
            WHERE ${category}.TeamNumber = current_tournament_teams.TeamNumber
            AND ${category}.Tournament = '<c:out value="${currentTournament}"/>'
            AND current_tournament_teams.event_division = '<c:out value="${division}"/>'
        </sql:query>
        <c:forEach var="judgeRow" items="${judges.rows}">
          <h3><x:out select="./@title"/> Division: <c:out value="${division}"/> Score Group: <c:out value="${judgeRow.Judge}"/></h3>
          <table border='0'>
            <tr><th colspan='3'>Team # / Organization / Team Name</th><th>Raw Score</th><th>Scaled Score</th></tr>
            <sql:query var="scores" dataSource="${datasource}">
              SELECT
                 Teams.TeamNumber
                ,Teams.Organization
                ,Teams.TeamName
                ,${category}.ComputedTotal
                ,${category}.StandardizedScore
                FROM Teams, ${category}
                WHERE Teams.TeamNumber = ${category}.TeamNumber
                AND Tournament = '<c:out value="${currentTournament}"/>'
                AND Judge = '<c:out value="${judgeRow.Judge}"/>'
                AND ${category}.ComputedTotal IS NOT NULL
                ORDER BY ${category}.ComputedTotal DESC
            </sql:query>
            <c:forEach var="row" items="${scores.rowsByIndex}">
              <tr>
                <td><c:out value="${row[0]}"/></td>
                <td><c:out value="${row[1]}"/></td>
                <td><c:out value="${row[2]}"/></td>
                <td><c:out value="${row[3]}"/></td>
                <td><fmt:formatNumber value="${row[4]}" maxFractionDigits="2" minFractionDigits="2"/></td>
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

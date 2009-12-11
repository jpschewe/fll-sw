<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.xml.WinnerType" %>
<%@ page import="fll.xml.XMLUtils" %>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="org.w3c.dom.Document" %>
      
<%@ page import="java.sql.Connection" %>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
final String tournamentReq = request.getParameter("currentTournament");
final String tournament;
if(tournamentReq == null) {
  tournament = Queries.getCurrentTournament(connection);
} else {
  tournament = tournamentReq;
}

final Document challengeDocument = (Document)application.getAttribute(ApplicationAttributes.CHALLENGE_DOCUMENT);;
final WinnerType winnerCriteria = XMLUtils.getWinnerCriteria(challengeDocument);          
final String ascDesc = WinnerType.HIGH == winnerCriteria ? "DESC" : "ASC";

pageContext.setAttribute("ascDesc", ascDesc);
pageContext.setAttribute("currentTournament", tournament);
pageContext.setAttribute("divisions", Queries.getDivisions(connection));
%>


<%@page import="fll.web.ApplicationAttributes"%><html>
  <head>
    <title>Categorized Scores</title>
  </head>

  <body>

    <h1>FLL Categorized Score Summary</h1>
    <hr/>
    <h2>Tournament: <c:out value="${currentTournament}"/></h2>
    <c:forEach items="${divisions}" var="division">
      <x:forEach select="$challengeDocument/fll/subjectiveCategory">
        <h3><x:out select="./@title"/> Division: <c:out value="${division}"/></h3>
        <table border='0'>
          <tr><th colspan='3'>Team # / Organization / Team Name</th><th>Raw Score</th><th>Scaled Score</th></tr>
          <sql:query var="scores" dataSource="${datasource}">
            SELECT
               Teams.TeamNumber
              ,Teams.Organization
              ,Teams.TeamName
              ,FinalScores.<x:out select="./@name"/>
              FROM Teams, FinalScores, TournamentTeams
              WHERE Teams.TeamNumber = FinalScores.TeamNumber
              AND TournamentTeams.TeamNumber = Teams.TeamNumber
              AND TournamentTeams.Tournament = FinalScores.Tournament
              AND TournamentTeams.event_division = '<c:out value="${division}"/>'
              AND FinalScores.Tournament = '<c:out value="${currentTournament}"/>'
              ORDER BY FinalScores.<x:out select="./@name"/> ${ascDesc}
          </sql:query>
          <c:forEach var="row" items="${scores.rowsByIndex}">
            <tr>
              <td><c:out value="${row[0]}"/></td>
              <td><c:out value="${row[1]}"/></td>
              <td><c:out value="${row[2]}"/></td>
              <td>
                <sql:query var="rawRows" dataSource="${datasource}">
                  SELECT ComputedTotal
                    FROM <x:out select="./@name"/>
                    WHERE TeamNumber = <c:out value="${row[0]}"/>
                    AND Tournament = '<c:out value="${currentTournament}"/>'
                    ORDER BY ComputedTotal DESC
                </sql:query>
                <c:set var="first" value="true"/>
                <c:forEach var="rawRow" items="${rawRows.rowsByIndex}">
                    <c:if test="${not first}">, </c:if>
                  <c:if test="${rawRow[0] != null}">
                    <c:if test="${first}"><c:set var="first" value="false"/></c:if>
                    <c:out value="${rawRow[0]}"/>
                  </c:if>
                </c:forEach>
              </td>
              <td><fmt:formatNumber value="${row[3]}" maxFractionDigits="2" minFractionDigits="2"/></td>
            </tr>
          </c:forEach>
          <tr><td colspan='5'><hr/></td></tr>
        </table>
      </x:forEach> <!-- end foreach category -->
    </c:forEach> <!-- end foreach division -->
          

  </body>
</html>

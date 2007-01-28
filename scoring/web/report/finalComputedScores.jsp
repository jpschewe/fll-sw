<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.report.FinalComputedScores" %>
<%@ page import="fll.db.Queries" %>
      
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

pageContext.setAttribute("currentTournament", tournament);
pageContext.setAttribute("divisions", Queries.getDivisions(connection));
%>

<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Final Computed Scores)</title>
    <style type="text/css">
    td {font-size: 12px}
    th {font-size: 12px}
    .warn { color: red; font-weight: bold }
    </style>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Final Computed Scores)</h1>

    <x:set select="count($challengeDocument/fll/subjectiveCategory)" var="subjectiveCategoryCount"/>
          
    <h1>FLL Final Scores for <c:out value="${currentTournament}"/></h1>
    <hr/>
    <c:forEach items="${divisions}" var="division">
      <h2><x:out select="./@title"/> Division: <c:out value="${division}"/></h2>
      <table border='0'>
        <tr>
          <th>Organization / Team # / Team Name<br/>Weight</th>
          <th>&nbsp;</th>
          <!-- categories -->
          <x:forEach select="$challengeDocument/fll/subjectiveCategory">
            <x:set var="weight" select="number(./@weight)"/>
            <c:if test="${weight > 0.0}">
              <th align='center'>
                <x:out select="./@title"/><br/>
                <fmt:formatNumber value="${weight}" maxFractionDigits="4" minFractionDigits="1"/>
              </th>
            </c:if>
          </x:forEach> <!-- end foreach category -->
          <x:forEach select="$challengeDocument/fll/Performance">
            <x:set var="weight" select="number(./@weight)"/>
            <c:if test="${weight > 0.0}">
              <th align='center'>
                Performance<br/>
                <fmt:formatNumber value="${weight}" maxFractionDigits="4" minFractionDigits="1"/>
              </th>
            </c:if>            
          </x:forEach> <!-- end foreach performance element -->
          <th align='center'>Overall Score</th>
        </tr>
        <tr><td colspan='<c:out value="${subjectiveCategoryCount + 4}"/>'><hr/></td></tr>
                  
        <!-- all scores -->
        <sql:query var="scoreRows" dataSource="${datasource}">
          SELECT Teams.Organization
            ,Teams.TeamName
            ,Teams.TeamNumber
            ,FinalScores.OverallScore
            ,FinalScores.performance
            <x:forEach select="$challengeDocument/fll/subjectiveCategory">
              ,FinalScores.<x:out select="./@name"/>
            </x:forEach>
            FROM Teams,FinalScores,current_tournament_teams
            WHERE FinalScores.TeamNumber = Teams.TeamNumber
            AND current_tournament_teams.TeamNumber = Teams.TeamNumber
            AND FinalScores.Tournament = '<c:out value="${currentTournament}"/>'
            AND current_tournament_teams.event_division = '<c:out value="${division}"/>'
            ORDER BY FinalScores.OverallScore DESC, Teams.TeamNumber
        </sql:query>
        <c:forEach items="${scoreRows.rowsByIndex}" var="scoreRow">

          <!-- raw -->
          <tr>
            <td><c:out value="${scoreRow[0]}"/></td>
            <td align='right'>Raw: </td>
            <x:forEach select="$challengeDocument/fll/subjectiveCategory">
              <sql:query var="rawRows" dataSource="${datasource}">
                SELECT ComputedTotal
                  FROM <x:out select="./@name"/>
                  WHERE Teamnumber = <c:out value="${scoreRow[2]}"/>
                  AND Tournament = '<c:out value="${currentTournament}"/>'
                  ORDER BY ComputedTotal DESC
              </sql:query>
              <c:set var="scoreSeen" value="false"/>
              <c:forEach var="rawRow" items="${rawRows.rowsByIndex}">
                <c:if test="${rawRow[0] != null}">
                  <c:choose>
                    <c:when test="${scoreSeen}">, </c:when>
                    <c:otherwise>
                      <c:set var="scoreSeen" value="true"/>
                      <td align='center'>
                    </c:otherwise>
                  </c:choose>
                  <c:out value="${rawRow[0]}"/>
                </c:if> <!-- end if score not null -->
              </c:forEach> <!-- end foreach subjective category -->
              <c:if test="${not scoreSeen}"><td align='center' class='warn'>No Score</c:if>
              </td>
            </x:forEach>
            <!-- performance -->
            <sql:query var="performanceRows" dataSource="${datasource}">
              SELECT score
                FROM performance_seeding_max
                WHERE teamnumber = <c:out value="${scoreRow[2]}"/>
                AND Tournament = '<c:out value="${currentTournament}"/>'
            </sql:query>
            <c:set var="rawPerformance" value="${performanceRows.rowsByIndex[0][0]}"/>
            <c:choose>
              <c:when test="${rawPerformance != null}">
                <td align='center'><c:out value="${rawPerformance}"/></td>
              </c:when>
              <c:otherwise>
                <td align='center' class='warn'>No Score</td>
              </c:otherwise>
            </c:choose>            
            <td align='center'>&nbsp;</td>
          </tr>

          <!-- scaled -->
          <tr>
            <td><c:out value="${scoreRow[2]}"/> <c:out value="${scoreRow[1]}"/></td>
            <td align='right'>Scaled: </td>
            <c:forEach var="subIndex" begin="5" end="${5 + subjectiveCategoryCount - 1}">
              <c:choose>
                <c:when test="${scoreRow[subIndex] != null}">
                  <td align='center'><fmt:formatNumber value="${scoreRow[subIndex]}" maxFractionDigits="2" minFractionDigits="2"/></td>
                </c:when>
                <c:otherwise>
                  <td align='center' class='warn'>No Score</td>
                </c:otherwise>
              </c:choose>
            </c:forEach> <!-- end foreach subjective category -->
            <!-- performance -->
            <c:choose>
              <c:when test="${scoreRow[4] != null}">
                <td align='center'><fmt:formatNumber value="${scoreRow[4]}" maxFractionDigits="2" minFractionDigits="2"/></td>
              </c:when>
              <c:otherwise>
                <td align='center' class='warn'>No Score</td>
              </c:otherwise>
            </c:choose>
            <!-- overall score -->
            <td align='center'><fmt:formatNumber value="${scoreRow[3]}" maxFractionDigits="2" minFractionDigits="2"/></td>
          </tr>
                    
          <tr><td colspan='<c:out value="${subjectiveCategoryCount + 4}"/>'><hr/></td></tr>
        </c:forEach> <!-- end foreach score -->
                  
      </table>
    </c:forEach> <!-- end foreach division -->
          

  </body>
</html>

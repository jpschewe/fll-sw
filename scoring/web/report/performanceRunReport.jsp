<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.ApplicationAttributes"%>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>
  
<%
  	final DataSource datasource = ApplicationAttributes.getDataSource();
  final Connection connection = datasource.getConnection();
  pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
  pageContext.setAttribute("divisions", Queries.getEventDivisions(connection));
  %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Performance Run <c:out value="${param.RunNumber}"/></title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Performance Run <c:out value="${param.RunNumber}"/>)</h1>

    <c:if test="${empty param.RunNumber}">
      <font color='red'>You must specify a run number!</font>
    </c:if>
    <c:if test="${not empty param.RunNumber}">
      <c:forEach var="division" items="${divisions}">
        <h2>Division <c:out value="${division}"/></h2>
        <table border='1'>
          <tr>
           <th>Team Number </th>
           <th>Team Name </th>
           <th>Organization </th>
           <th>Score</th>
          </tr>
          <sql:query var="result" dataSource="jdbc/FLLDB">
            SELECT Teams.TeamNumber,Teams.TeamName,Teams.Organization,Performance.ComputedTotal,Performance.NoShow
                     FROM Teams,Performance,current_tournament_teams
                     WHERE Performance.RunNumber = <c:out value="${param.RunNumber}"/>
                       AND Teams.TeamNumber = Performance.TeamNumber
                       AND current_tournament_teams.TeamNumber = Teams.TeamNumber
                       AND Performance.Tournament = <c:out value="${tournament}"/>
                       AND current_tournament_teams.event_division  = '<c:out value="${division}"/>'
                       ORDER BY ComputedTotal DESC
          </sql:query>
          <c:forEach items="${result.rows}" var="row">
            <tr>
              <td><c:out value="${row.TeamNumber}"/></td>
              <td><c:out value="${row.TeamName}"/></td>
              <td><c:out value="${row.Organization}"/></td>
              <c:if test="${row.NoShow == True}" var="test">
                <td>No Show</td>
              </c:if>
              <c:if test="${row.NoShow != True}">
                <td><c:out value="${row.ComputedTotal}"/></td>
              </c:if>
            </tr>
          </c:forEach>
        </table>
      </c:forEach>
    </c:if>
      

  </body>
</html>

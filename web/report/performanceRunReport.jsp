<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Queries" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
pageContext.setAttribute("divisions", Queries.getDivisions(connection)); 
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Performance Run <c:out value="${param.RunNumber}"/>)</title>
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
           <th>Score</th>
          </tr>
          <sql:query var="result" dataSource="${datasource}">
            SELECT Teams.TeamNumber,Teams.TeamName,Performance.ComputedTotal
                     FROM Teams,Performance
                     WHERE Performance.RunNumber = <c:out value="${param.RunNumber}"/>
                       AND Teams.TeamNumber = Performance.TeamNumber
                       AND Performance.Tournament = '<c:out value="${tournament}"/>'
                       AND Teams.Division  = '<c:out value="${division}"/>'
                       ORDER BY ComputedTotal DESC
          </sql:query>
          <c:forEach items="${result.rows}" var="row">
            <tr>
              <td><c:out value="${row.TeamNumber}"/></td>
              <td><c:out value="${row.TeamName}"/></td>
              <td><c:out value="${row.ComputedTotal}"/></td>
            </tr>
          </c:forEach>
        </table>
      </c:forEach>
    </c:if>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

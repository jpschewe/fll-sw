<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
%>

<%@ page import="org.w3c.dom.Document" %>
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument//@title"/> (Team <c:out value="${param.TeamNumber}"/> Performance Scores)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument//@title"/> (Team <c:out value="${param.TeamNumber}"/> Performance Scores)</h1>

    <sql:query var="result" dataSource="${datasource}">
      SELECT RunNumber, ComputedTotal
        FROM Performance
        WHERE TeamNumber = <c:out value="${param.TeamNumber}"/>
          AND Performance.Tournament = '<c:out value="${tournament}"/>'
        ORDER By RunNumber
    </sql:query>
    <table border='1'>
      <tr>
        <th>Run Number</th>
        <th>Score</th>
      </tr>
      <c:forEach items="${result.rows}" var="row">
        <tr>
          <td><c:out value="${row.RunNumber}"/></td>
          <td><c:out value="${row.ComputedTotal}"/></td>
        </tr>
      </c:forEach>
    </table>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

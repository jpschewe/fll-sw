<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="org.w3c.dom.Document" %>

<%
final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
pageContext.setAttribute("tournament", Queries.getCurrentTournament(connection));
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Team <c:out value="${param.TeamNumber}"/> Performance Scores</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Team <c:out value="${param.TeamNumber}"/> Performance Scores)</h1>

    <sql:query var="result" dataSource="${datasource}">
      SELECT RunNumber, ComputedTotal, NoShow
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
          <c:if test="${row.NoShow == True}" var="test">
            <td>No Show</td>
          </c:if>
          <c:if test="${row.NoShow != True}">
            <td><c:out value="${row.ComputedTotal}"/></td>
          </c:if>
        </tr>
      </c:forEach>
    </table>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.Queries"%>

<%@ page import="java.sql.Connection"%>
<%@ page import="fll.web.ApplicationAttributes"%>

<%
      final Connection connection = (Connection) application.getAttribute(ApplicationAttributes.CONNECTION);
      pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
%>

<html>
<head>
<title><x:out select="$challengeDocument/fll/@title" />
(Categorized Scores)</title>
</head>

<body>

<h1>FLL Unverified Runs</h1>
<hr />
<h2>Tournament: <c:out value="${currentTournament}" /></h2>
<table border='0'>
 <tr>
  <th>Team #</th>
  <th>Run Number</th>
 </tr>
 <sql:query var="result" dataSource="${datasource}">
   SELECT
     TeamNumber
    ,RunNumber
     FROM Performance
     WHERE Verified <> TRUE 
       AND Tournament = '<c:out value="${currentTournament}" />'
       ORDER BY RunNumber
 </sql:query>
 <c:forEach var="row" items="${result.rowsByIndex}">
  <tr>
   <td><c:out value="${row[0]}" /></td>
   <td><c:out value="${row[1]}" /></td>
  </tr>
 </c:forEach>
</table>

</body>
</html>

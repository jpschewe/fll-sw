<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.Tournament"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.SessionAttributes" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>
<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();

      pageContext.setAttribute("regions", Queries.getRegions(connection));
      pageContext.setAttribute("tournaments", Tournament.getTournaments(connection));
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title>Tournament Initialization</title>
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Tournament
Initialization)</h1>

<p>This page allows you to change the tournament for a group of
teams based on the region that they're in. Any previous tournament
assignments for the selected teams will be removed</p>

<form name="form" action="verifyTournamentInitialization.jsp"
 method="post">
<table border='1'>
 <c:forEach var="region" items="${regions}">
  <tr>
   <td><c:out value="${region}" /></td>
   <td><select name='<c:out value="${region}"/>'>
    <option value='nochange'>No Change</option>
    <c:forEach var="tournament" items="${tournaments}">
     <c:choose>
      <c:when test="${tournament.name == region}">
       <option value='${tournament.name}' selected><c:out value="${tournament.name}" /></option>
      </c:when>
      <c:otherwise>
       <option value='${tournament.name}'><c:out value="${tournament.name}" /></option>
      </c:otherwise>
     </c:choose>
    </c:forEach>
   </select></td>
  </tr>
 </c:forEach>
</table>

<input type='submit' value='Submit' /></form>


</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.Queries"%>
<%@ page import="java.sql.Connection" %>
<%
      final Connection connection = (Connection) application.getAttribute("connection");

      pageContext.setAttribute("regions", Queries.getRegions(connection));
      pageContext.setAttribute("tournamentNames", Queries.getTournamentNames(connection));
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
<title><x:out select="$challengeDocument/fll/@title" />
(Tournament Initialization)</title>
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
    <c:forEach var="tournament" items="${tournamentNames}">
     <c:choose>
      <c:when test="${tournament == region}">
       <option selected><c:out value="${tournament}" /></option>
      </c:when>
      <c:otherwise>
       <option><c:out value="${tournament}" /></option>
      </c:otherwise>
     </c:choose>
    </c:forEach>
   </select></td>
  </tr>
 </c:forEach>
</table>

<input type='submit' value='Submit' /></form>

<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>

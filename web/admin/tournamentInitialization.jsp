<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<%@ page import="fll.Queries" %>
      
<%
final Connection connection = (Connection)application.getAttribute("connection");

pageContext.setAttribute("regions", Queries.getRegions(connection));
pageContext.setAttribute("tournamentNames", Queries.getTournamentNames(connection));
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument//@title"/> (Tournament Initialization)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument//@title"/> (Tournament Initialization)</h1>

    <p>This page allows you to change the tournament for a group of teams
    based on the region that they're in.  Any previous tournament
    assignements for the selected teams will be removed</p>
      
    <form name="form" action="verifyTournamentInitialization.jsp" method="post">
      <table border='1'>
        <c:forEach var="region" items="${regions}">
          <tr>
            <td><c:out value="${region}" /></td>
            <td><select name='<c:out value="${region}"/>'>
              <option value='nochange'>No Change</option>
              <c:forEach var="tournament" items="${tournamentNames}">
                <option><c:out value="${tournament}" /></option>
              </c:forEach>
            </select></td>
          </tr>
        </c:forEach>
      </table border='1'>
    
      <input type='submit' value='Submit' />
    </form>

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Queries" %>
<%@ page import="fll.Utilities" %>
      
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
      
<%
final Connection connection = (Connection)application.getAttribute("connection");
%>

<c:if test="${not empty param.submit}">
  <c:forEach var="team" items="${paramValues.advance}">
    <%
      Queries.advanceTeam(connection, Integer.valueOf(pageContext.getAttribute("team").toString()).intValue());
    %>
  </c:forEach>
</c:if>
      
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Select Team)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Advance Teams)</h1>

      <form action="advanceTeams.jsp" method="get">
        <table border='1'>
          <tr>
            <th>&nbsp;</th>
            <th>Team Number</th>
            <th>Team Name</th>
            <th>Current Tournament</th>
          </tr>

          <%
          final Statement stmt = connection.createStatement();
          final ResultSet rs = stmt.executeQuery("SELECT TeamNumber,TeamName FROM Teams ORDER BY TeamNumber ASC");
          while(rs.next()) {
            final int teamNumber = rs.getInt(1);
            final String teamName = rs.getString(2);
          %>
            <tr>
              <td><input type="checkbox" name="advance" value='<%=teamNumber%>' /></td>
              <td><%=teamNumber%></td>
              <td><%=teamName%></td>
              <td><%=Queries.getTeamCurrentTournament(connection, teamNumber)%></td>
            </tr>
          <%
          }
          Utilities.closeResultSet(rs);
          Utilities.closeStatement(stmt);
          %>

        </table>

        <input type='submit' name='submit' value='Advance Selected Teams' />
      </form>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

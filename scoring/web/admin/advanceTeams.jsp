<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.db.Queries" %>

<%@ page import="fll.web.SessionAttributes" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>
      
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="javax.sql.DataSource" %>
      
<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
%>

<c:if test="${not empty param.submit}">
  <c:forEach var="team" items="${paramValues.advance}">
    <%
      Queries.advanceTeam(connection, Integer.valueOf(pageContext.getAttribute("team").toString()).intValue());
    %>
  </c:forEach>
</c:if>
      

  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Advance Teams</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Advance Teams)</h1>

      <form action="advanceTeams.jsp" method="POST">
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
          SQLFunctions.closeResultSet(rs);
          SQLFunctions.closeStatement(stmt);
          %>

        </table>

        <input type='submit' name='submit' value='Advance Selected Teams' />
      </form>
      

  </body>
</html>

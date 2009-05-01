<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>    
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>

<%
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();

pageContext.setAttribute("regions", Queries.getRegions(connection));
%>

<c:if test='${"true" == param.verified}'>
  <c:forEach var="parameter" items="${paramValues}">
    <c:if test='${"nochange" != parameter.value[0] && "verified" != parameter.key && "submit" != parameter.key}'>
      <sql:update dataSource="${datasource}">
        DELETE FROM TournamentTeams WHERE TeamNumber IN ( SELECT TeamNumber FROM Teams WHERE Region = '<c:out value="${parameter.key}"/>' )
      </sql:update>
      <sql:update dataSource="${datasource}">
        INSERT INTO TournamentTeams (TeamNumber, Tournament, event_division) SELECT Teams.TeamNumber, '<c:out value="${parameter.value[0]}"/>', Teams.Division FROM Teams WHERE Teams.Region = '<c:out value="${parameter.key}"/>'
      </sql:update>
    </c:if>
  </c:forEach>
  
  <c:redirect url="index.jsp" >
    <c:param name="message">
      Successfully initialized tournament for teams based on region.
    </c:param>
  </c:redirect>
</c:if>
      
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Verify Tournament Initialization)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Verify Tournament Initialization)</h1>

    <p>The following teams will have their tournament changed if you continue:</p>

    <form name="verify" action="verifyTournamentInitialization.jsp" method="post">
      <table border='1'>
        <tr><th>Team Number</th><th>Team Name</th><th>Region</th><th>New Tournament</th></tr>
              
        <c:forEach var="parameter" items="${paramValues}">
          <input type='hidden'
                 name='<c:out value="${parameter.key}" />'
                 value='<c:out value="${parameter.value[0]}" />'
                 />
          <c:if test='${"nochange" != parameter.value[0]}'>
            <sql:query var="result" dataSource="${datasource}">
              SELECT DISTINCT Teams.TeamNumber AS TeamNumber, Teams.TeamName AS TeamName, Teams.Region AS Region
                FROM Teams,TournamentTeams
                WHERE Teams.Region = '<c:out value="${parameter.key}"/>'
                AND TournamentTeams.TeamNumber = Teams.TeamNumber
                ORDER BY Teams.TeamNumber
            </sql:query>
            <c:forEach items="${result.rows}" var="row">
              <tr>
                <td><c:out value="${row.TeamNumber}" /></td>
                <td><c:out value="${row.TeamName}" /></td>
                <td><c:out value="${row.Region}" /></td>
                <td><c:out value="${param[row.Region]}"/></td>
              </tr>
            </c:forEach>
          </c:if>
        </c:forEach>
      </table>
            
      <input type='hidden' name='verified' value='true' />
      <input type='submit' name='submit' value='Continue' />
    </form>


<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

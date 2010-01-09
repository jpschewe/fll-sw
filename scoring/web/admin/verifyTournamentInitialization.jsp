<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Verify Tournament Initialization</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Verify Tournament Initialization)</h1>

    <p>The following teams will have their tournament changed if you continue:</p>

    <form name="verify" action="CommitTournamentInitialization" method="post">
      <table border='1'>
        <tr><th>Team Number</th><th>Team Name</th><th>Region</th><th>New Tournament</th></tr>
              
        <c:forEach var="parameter" items="${paramValues}">
          <input type='hidden'
                 name='<c:out value="${parameter.key}" />'
                 value='<c:out value="${parameter.value[0]}" />'
                 />
          <c:set var="decodedTournament">
            ${fn:replace(parameter.value[0], "&amp;", "&")}
          </c:set>
          <c:if test='${"nochange" != parameter.value[0]}'>
            <sql:query var="result" dataSource="${datasource}">
              SELECT DISTINCT Teams.TeamNumber AS TeamNumber, Teams.TeamName AS TeamName, Teams.Region AS Region
                FROM Teams
                WHERE Teams.Region = '${decodedTournament}'
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



  </body>
</html>

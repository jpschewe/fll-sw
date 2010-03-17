<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Verify Advancing Teams</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Verify Advancing Teams)</h1>

<p>The following teams will be advanced to the next tournament.</p>
      <form action="AdvanceTeams" method="POST">
        <table border='1'>
          <tr>
            <th>Team Number</th>
            <th>Team Name</th>
            <th>Current Tournament</th>
            <th>Next Tournament</th>
          </tr>
          <c:forEach items="${advancingTeams}" var="team">
          <tr>
          <td>${team.teamNumber }</td>
          <td>${team.teamName}</td>
          <td>${currentTournament[team.teamNumber] }</td>
          <td>${nextTournament[team.teamNumber] }</td>
          </tr>
          </c:forEach>		
        </table>

        <input type='submit' name='submit' value='Advance Selected Teams' />
      </form>
      

  </body>
</html>

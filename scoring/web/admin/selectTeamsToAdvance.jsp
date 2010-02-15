<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Advance Teams</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Advance Teams)</h1>

      <form action="GatherTeamAdvancementData" method="POST">
        <table border='1'>
          <tr>
            <th>&nbsp;</th>
            <th>Team Number</th>
            <th>Team Name</th>
            <th>Current Tournament</th>
            <th>Next Tournament</th>
          </tr>
          <c:forEach items="${advancingTeams}" var="team">
          <tr>
          <td><input type="checkbox" name="advance" value='${teamNum }'/></td>
          <td>${team.teamNumber }</td>
          <td>${team.teamName}</td>
          <td>${currentTournament[team.teamNumber] }</td>
          <td>${nextTournament[team.teamNumber] }</td>
          </c:forEach>
		</tr>
        </table>

        <input type='submit' name='submit' value='Advance Selected Teams' />
      </form>
      

  </body>
</html>

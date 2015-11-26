<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@page import="fll.web.admin.GatherTeamData"%>

<%
  GatherTeamData.populateContext(request, application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />
<script type='text/javascript'>
  //confirm deleting a team
  function confirmDeleteTeam() {
    return confirm("Are you sure you want to delete team ${teamNumber}?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
  }

  // confirm removing team from tournament
  function confirmRemoveFromTournament(tournamentName, checkboxId) {
    if (document.getElementById(checkboxId).checked == true) {
      return false;
    } else {
      var confirmed = confirm("Are you sure you want to remove team ${teamNumber} from "
          + tournamentName
          + "?  Any data associated with that team and this tournament ("
          + tournamentName
          + ") will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
      if (confirmed == true) {
        return true;
      } else {
        document.getElementById(checkboxId).checked = true;
      }
    }
  }
</script>

<c:choose>
  <c:when test="${addTeam }">
    <title>Add Team</title>
  </c:when>
  <c:otherwise>
    <title>Edit Team</title>
  </c:otherwise>
</c:choose>

</head>

<body>
  <c:choose>
    <c:when test="${addTeam }">
      <h1>Add Team</h1>
    </c:when>
    <c:otherwise>
      <h1>Edit Team</h1>
    </c:otherwise>
  </c:choose>

  <form
    action="CommitTeam"
    method="post"
    name="editTeam">
    <input
      type='hidden'
      name='addTeam'
      value='${addTeam }' />

    <table border='1'>
      <tr>
        <td>Team Number (required)</td>

        <c:choose>
          <c:when test="${addTeam}">
            <td><input
              type='text'
              name='teamNumber'
              value='${teamNumber}'></td>
          </c:when>
          <c:otherwise>
            <td><input
              type='hidden'
              name='teamNumber'
              value='${teamNumber}'>${teamNumber}</td>
          </c:otherwise>
        </c:choose>
      </tr>

      <tr>
        <td>Team Name</td>
        <td><input
          type='text'
          name='teamName'
          size='64'
          value='${teamName}'></td>
      </tr>
      <tr>
        <td>Organization</td>
        <td><input
          type='text'
          name='organization'
          size='64'
          value='${organization }'></td>
      </tr>

      <!--  specify division -->
      <tr>
        <td>Division (required)</td>
        <td><c:forEach
            items="${divisions}"
            var="possibleDivision">
            <input
              type='radio'
              name='division'
              value='${possibleDivision}'
              id='${possibleDivision}'
              <c:if test="${division == possibleDivision}">
       checked='true'
     </c:if>
              <c:if test="${playoffsInitialized}">
       disabled='disabled'
     </c:if> />
            <label for='${possibleDivision}'>${possibleDivision}</label>
          </c:forEach> <input
          type='radio'
          id='division_text_choice'
          name='division'
          value='text'
          <c:if test="${playoffsInitialized}">
       disabled='disabled'
   </c:if> />
          <input
          type='text'
          name='division_text'
          <c:if test="${playoffsInitialized}">
       disabled='disabled'
   </c:if> />
          <c:if test="${playoffsInitialized}">
            <input
              type='hidden'
              name='division'
              value='${division}' />
            <!-- Browsers don't seem to submit the disabled form data, so I add this such that the edit still goes through -->
          </c:if></td>
      </tr>
      <!-- end specify division -->

      <!-- tournaments -->
      <tr>
        <td>Tournament(s)</td>
        <td><c:forEach
            items='${tournaments}'
            var='tournament'>

            <label> <!-- check if team is in this tournament -->
              <c:set
                var="contains"
                value="false" /> <c:forEach
                var="tid"
                items="${teamTournamentIDs}">
                <c:if test="${tid eq tournament.tournamentID}">
                  <c:set
                    var="contains"
                    value="true" />
                </c:if>
              </c:forEach> <!-- decide if tournament should be checked --> <c:choose>
                <c:when test="${contains}">
                  <input
                    type='checkbox'
                    name='tournaments'
                    value='${tournament.tournamentID}'
                    id='tournament_${tournament.tournamentID}'
                    checked
                    onchange="confirmRemoveFromTournament('${tournament.name}', 'tournament_${tournament.tournamentID}')" />
                </c:when>
                <c:otherwise>
                  <input
                    type='checkbox'
                    name='tournaments'
                    value='${tournament.tournamentID}'
                    id='tournament_${tournament.tournamentID}' />
                </c:otherwise>
              </c:choose> ${tournament.name}
            </label>
            <br />

          </c:forEach></td>
      </tr>
      <!--  end tournaments -->

    </table>

    <c:choose>
      <c:when test="${addTeam}">
        <input
          type='submit'
          name='commit'
          value='Commit'>
      </c:when>
      <c:otherwise>
        <input
          type='submit'
          name='commit'
          value='Commit'>
      </c:otherwise>
    </c:choose>

    <c:if test="${not addTeam}">
      <c:choose>
        <c:when test="${inPlayoffs}">
          <p>Teams cannot be deleted once they are in the playoff
            data table for a tournament.</p>
        </c:when>
        <c:otherwise>
          <input
            type='submit'
            name='delete'
            value='Delete Team'
            onclick='return confirmDeleteTeam()'>
        </c:otherwise>
      </c:choose>
    </c:if>
  </form>

  <form
    action="index.jsp"
    method='post'>
    <input
      type='submit'
      value='Cancel'>
  </form>


</body>
</html>

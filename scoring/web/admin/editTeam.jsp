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

<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>

<script type='text/javascript'>

/**
 * Check if a value exists in a select element.
 *
 * @param select a jquery object for a select element
 * @param value the value to check against the select
 */
function valueExistsInSelect(select, value) {
  var exists = false;
  $(select).each(function(){
      if (this.value == value) {
          exists = true;
          return false;
      }
  });
  return exists;
}

$(document).ready(function() {

<%-- listeners checkboxes --%>
<c:forEach items="${tournaments}" var="tournament">

// initialize tournament checkbox
<c:choose>
<c:when test="${teamInTournament[tournament.tournamentID]}">
$("#tournament_${tournament.tournamentID}").prop("checked", true);
$("#event_division_${tournament.tournamentID }").prop("disabled", false);
$("#judging_station_${tournament.tournamentID }").prop("disabled", false);    
</c:when>
<c:otherwise>
$("#tournament_${tournament.tournamentID}").prop("checked", false);
$("#event_division_${tournament.tournamentID }").prop("disabled", true);
$("#judging_station_${tournament.tournamentID }").prop("disabled", true);    
</c:otherwise>
</c:choose>


$("#tournament_${tournament.tournamentID}").change(function() {
  if($("#tournament_${tournament.tournamentID}").prop("checked") == true) {
    $("#event_division_${tournament.tournamentID }").prop("disabled", false);
    $("#judging_station_${tournament.tournamentID }").prop("disabled", false);    
  } else {
    <c:if test="${teamInTournament[tournament.tournamentID]}">
    var confirmed = confirm("Are you sure you want to remove team ${teamNumber} from ${tournament.name}?"
        + " Any data associated with that team and this tournament (${tournament.name}) will be removed from the database, including any scores that have been entered."
        + " You also need to download the files for subjective score entry again."
        + " It is not advisable to do this while the tournament that the team is in is running.");
    if (confirmed == true) {
      </c:if>
      $("#event_division_${tournament.tournamentID }").prop("disabled", true);
      $("#judging_station_${tournament.tournamentID }").prop("disabled", true);    
      <c:if test="${teamInTournament[tournament.tournamentID]}">
    } else {
      this.checked = true;
    }
</c:if>        
  }
});

</c:forEach> <%-- listeners for checkboxes --%>

});

//confirm deleting a team
  function confirmDeleteTeam() {
    return confirm("Are you sure you want to delete team ${teamNumber}?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
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
        <td>

          <table border="1">
            <tr>
              <th>Tournament</th>
              <th>Event Division</th>
              <th>Judging Station</th>
            </tr>

            <c:forEach
              items='${tournaments}'
              var='tournament'>

              <tr>
                <td>
                  <!--  tournament name --> <label> <input
                    type='checkbox'
                    name='tournaments'
                    value='${tournament.tournamentID}'
                    id='tournament_${tournament.tournamentID}' />
                    ${tournament.name}
                </label>
                </td>

                <td>
                  <!--  event division --> <select
                  name="event_division_${tournament.tournamentID }"
                  id="event_division_${tournament.tournamentID }">
                    <c:forEach
                      items="${tournamentEventDivisions[tournament.tournamentID] }"
                      var="event_division">

                      <c:choose>
                        <c:when
                          test="${currentEventDivisions[tournament.tournamentID] eq event_division}">
                          <option
                            value="${event_division }"
                            selected>${event_division }</option>
                        </c:when>
                        <c:otherwise>
                          <option value="${event_division }">${event_division }</option>
                        </c:otherwise>
                      </c:choose>

                    </c:forEach>
                </select>
                </td>

                <td>
                  <!--  judging station --> <select
                  name="judging_station_${tournament.tournamentID }"
                  id="judging_station_${tournament.tournamentID }">
                    <c:forEach
                      items="${tournamentJudgingStations[tournament.tournamentID] }"
                      var="judging_station">

                      <c:choose>
                        <c:when
                          test="${currentJudgingStations[tournament.tournamentID] eq judging_station}">
                          <option
                            value="${judging_station }"
                            selected>${judging_station }</option>
                        </c:when>
                        <c:otherwise>
                          <option value="${judging_station }">${judging_station }</option>
                        </c:otherwise>
                      </c:choose>

                    </c:forEach>
                </select>
                </td>


              </tr>

            </c:forEach>

          </table>
        </td>
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

<%@ include file="/WEB-INF/jspf/init.jspf"%>
<%@page import="fll.web.admin.GatherTeamData"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
GatherTeamData.populateContext(request, application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'>

/**
 * Check if a value exists in a select element.
 *
 * @param select a jquery object for a select element
 * @param value the value to check against the select
 */
function valueExistsInSelect(select, value) {
  let exists = false;
  for(option of select.options) {
      if (option.value == value) {
          exists = true;
          break;
      }
  }
  return exists;
}

document.addEventListener("DOMContentLoaded", function() {

<c:forEach items="${tournaments}" var="tournament">

<c:choose>
<c:when test="${playoffsInitialized[tournament.tournamentID]}">
document.getElementById("tournament_${tournament.tournamentID}").disabled = true;
document.getElementById("event_division_${tournament.tournamentID }").disabled = true;
document.getElementById("judging_station_${tournament.tournamentID }").disabled = true;

<c:choose>
<c:when test="${teamInTournament[tournament.tournamentID]}">
document.getElementById("tournament_${tournament.tournamentID}").checked = true;
</c:when>
<c:otherwise>
document.getElementById("tournament_${tournament.tournamentID}").checked = false;
</c:otherwise>
</c:choose>


</c:when>
<c:otherwise>

// initialize tournament checkbox
<c:choose>
<c:when test="${teamInTournament[tournament.tournamentID]}">
document.getElementById("tournament_${tournament.tournamentID}").checked = true;
document.getElementById("event_division_${tournament.tournamentID }").disabled = false;
document.getElementById("judging_station_${tournament.tournamentID }").disabled = false;    
</c:when>
<c:otherwise>
document.getElementById("tournament_${tournament.tournamentID}").checked = false;
document.getElementById("event_division_${tournament.tournamentID }").disabled = true;
document.getElementById("judging_station_${tournament.tournamentID }").disabled = true;    
</c:otherwise>
</c:choose>


document.getElementById("tournament_${tournament.tournamentID}").addEventListener("change", function() {
  if(document.getElementById("tournament_${tournament.tournamentID}").checked) {
    document.getElementById("event_division_${tournament.tournamentID }").disabled = false;
    document.getElementById("judging_station_${tournament.tournamentID }").disabled = false;    
  } else {
    <c:if test="${teamInTournament[tournament.tournamentID]}">
    const confirmed = confirm("Are you sure you want to remove team ${teamNumber} from ${tournament.name}?"
        + " Any data associated with that team and this tournament (${tournament.name}) will be removed from the database, including any scores that have been entered."
        + " You also need to download the files for subjective score entry again."
        + " It is not advisable to do this while the tournament that the team is in is running.");
    if (confirmed == true) {
      </c:if>
      document.getElementById("event_division_${tournament.tournamentID }").disabled = true;
      document.getElementById("judging_station_${tournament.tournamentID }").disabled = true;    
      <c:if test="${teamInTournament[tournament.tournamentID]}">
    } else {
      this.checked = true;
    }
</c:if>        
  }
});

document.getElementById("event_division_${tournament.tournamentID}").addEventListener("change", function() {
  const thisSelect = document.getElementById("event_division_${tournament.tournamentID}");
  const otherSelect = document.getElementById("judging_station_${tournament.tournamentID}");
  if(valueExistsInSelect(otherSelect, thisSelect.value)) {
    otherSelect.value = thisSelect.value;
  }  
});

document.getElementById("judging_station_${tournament.tournamentID}").addEventListener("change", function() {
  const thisSelect = document.getElementById("judging_station_${tournament.tournamentID}");
  const otherSelect = document.getElementById("event_division_${tournament.tournamentID}");
  if(valueExistsInSelect(otherSelect, thisSelect.value)) {
    otherSelect.value = thisSelect.value;
  }  
});

</c:otherwise> <%-- playoffs not initialized --%>
</c:choose>

</c:forEach> <%-- foreach tournament --%>

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

    <p>Note that teams cannot be added to or removed from a
        tournament that has head to head initialized. Also teams cannot
        be deleted that are involved in any initialized head to head
        brackets.</p>

    <form action="CommitTeam" method="post" name="editTeam">
        <input type='hidden' name='addTeam' value='${addTeam }' />

        <table border='1'>
            <tr>
                <td>Team Number (required)</td>

                <c:choose>
                    <c:when test="${addTeam}">
                        <td>
                            <input type='text' name='teamNumber'
                                value='${teamNumber}'>
                        </td>
                    </c:when>
                    <c:otherwise>
                        <td>
                            <input type='hidden' name='teamNumber'
                                value='${teamNumber}'>${teamNumber}</td>
                    </c:otherwise>
                </c:choose>
            </tr>

            <tr>
                <td>Team Name</td>
                <td>
                    <input type='text' name='teamName' size='64'
                        value='${teamNameEscaped}'>
                </td>
            </tr>
            <tr>
                <td>Organization</td>
                <td>
                    <input type='text' name='organization' size='64'
                        value='${organizationEscaped}'>
                </td>
            </tr>

            <!-- tournaments -->
            <tr>
                <td>Assign Tournaments</td>
                <td>

                    <table border="1">
                        <tr>
                            <th>Tournament</th>
                            <th>Award Group</th>
                            <th>Judging Group</th>
                            <th>Wave</th>
                        </tr>

                        <c:forEach items='${tournaments}'
                            var='tournament'>

                            <tr>
                                <td>
                                    <!--  tournament name -->
                                    <label>
                                        <input type='checkbox'
                                            name='tournament_${tournament.tournamentID}'
                                            value='true'
                                            id='tournament_${tournament.tournamentID}' />
                                        ${tournament.name}
                                    </label>
                                </td>

                                <td>
                                    <!--  award group -->
                                    <select
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
                                                    <option
                                                        value="${event_division }">${event_division }</option>
                                                </c:otherwise>
                                            </c:choose>

                                        </c:forEach>
                                    </select>
                                </td>

                                <td>
                                    <!--  judging group -->
                                    <select
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
                                                    <option
                                                        value="${judging_station }">${judging_station }</option>
                                                </c:otherwise>
                                            </c:choose>

                                        </c:forEach>
                                    </select>
                                </td>

                                <td>
                                    <!--  wave -->
                                    <select
                                        name="wave_${tournament.tournamentID }"
                                        id="wave_${tournament.tournamentID }">
                                        <c:forEach
                                            items="${tournamentWaves[tournament.tournamentID] }"
                                            var="wave">

                                            <c:choose>
                                                <c:when
                                                    test="${currentWaves[tournament.tournamentID] eq wave}">
                                                    <option
                                                        value="${wave}"
                                                        selected>${wave}</option>
                                                </c:when>
                                                <c:otherwise>
                                                    <option
                                                        value="${wave}">${wave}</option>
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
                <input type='submit' name='commit' value='Commit'>
            </c:when>
            <c:otherwise>
                <input type='submit' name='commit' value='Commit'>
            </c:otherwise>
        </c:choose>

        <c:if test="${not addTeam}">
            <c:choose>
                <c:when test="${inPlayoffs}">
                    <p>Teams cannot be deleted once they are in the
                        head to head data table for any tournament.</p>
                </c:when>
                <c:otherwise>
                    <input type='submit' name='delete'
                        value='Delete Team'
                        onclick='return confirmDeleteTeam()'>
                </c:otherwise>
            </c:choose>
        </c:if>

        <input type='submit' name='cancel' value='Cancel' />

    </form>

</body>
</html>

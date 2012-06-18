<%--
  This page is used for editing and adding teams.  The session attribute
  addTeam is set when this page is being used to add a team.
  --%>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<script type='text/javascript'>
//confirm deleting a team
function confirmDeleteTeam() {
  return confirm("Are you sure you want to delete team ${teamNumber}?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}
        
//confirm demoting a team
function confirmDemoteTeam() {
  return confirm("Are you sure you want to demote team ${teamNumber}?  Any data associated with that team and for the current tournament (${teamCurrentTournament.name}) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}

//confirm changing the current tournament
function confirmChangeTournament() {
 <c:choose>
  <c:when test="${empty teamPrevTournament}">
    if(document.editTeam.currentTournament.value != "${teamCurrentTournament.tournamentID}") {
      return confirm("Are you sure you want to change the tournament for team ${teamNumber}?  Any data associated with that team and for the current tournament (${teamCurrentTournament.name}) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
    } else {
      return true;
    }
  </c:when>
  <c:otherwise>
    return true;
  </c:otherwise>
  </c:choose>
}
        
</script>  
    <title>Add/Edit Team</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Add/Edit Team)</h1>

    <form action="CommitTeam" method="post" name="editTeam">
    <table border='1'>          
      <tr>
        <td>Team Number (required)</td>
            
        <c:choose>
          <c:when test="${addTeam}">
            <td><input type='text' name='teamNumber' value='${teamNumber}'></td>
          </c:when>
          <c:otherwise>
            <td><input type='hidden' name='teamNumber' value='${teamNumber}'>${teamNumber}</td>
          </c:otherwise>
        </c:choose>
      </tr>
          
      <tr>
        <td>Team Name</td>
        <td><input type='text' name='teamName' size='64' value='${teamName}'></td>
      </tr>
      <tr>
        <td>Organization</td>
        <td><input type='text' name='organization' size='64' value='${organization }'></td>
      </tr>
      <tr>
        <td>Division (required)</td>
        <td>
          <c:forEach items="${divisions}" var="possibleDivision">
    <c:choose>
     <c:when test="${division == possibleDivision}">
      <input type='radio' name='division' value='${possibleDivision}' checked />
     </c:when>
     <c:otherwise>
      <input type='radio' name='division' value='${possibleDivision}' />
     </c:otherwise>
    </c:choose>
    ${possibleDivision}
   </c:forEach> 
   <input type='radio' id='division_text_choice' name='division' value='text' /> 
   <input type='text' name='division_text' />
        </td>
      </tr>

      <tr>
        <td>Current Tournament</td>
        <td>
        <c:choose>
        <c:when test="${empty teamPrevTournament}">
          <c:choose>
          <c:when test="${playoffsInitialized}">
            <p>Playoffs are initialized for the team's current tournament, the current tournament may not be modified now.</p>
          </c:when>
          <c:otherwise>
            <select id='currentTournamentSelect' name='currentTournament'>
            <c:forEach items='${tournaments}' var='tournament'>
              <c:choose>
              <c:when test="${tournament.tournamentID == teamCurrentTournament.tournamentID }">
                <option value='${tournament.tournamentID }' selected>${tournament.name }</option>
              </c:when>
              <c:otherwise>
                <option value='${tournament.tournamentID }'>${tournament.name }</option>
              </c:otherwise>
              </c:choose>
            </c:forEach>
            </select>
          </c:otherwise>
          </c:choose>
        </c:when>
        <c:otherwise>
          ${teamCurrentTournament.name }
        </c:otherwise>
        </c:choose>
        </td>
      </tr>            
    </table>
    
    <c:choose>
      <c:when test="${addTeam}">
        <input type='submit' name='commit' value='Commit'>      
      </c:when>
      <c:otherwise>
        <input type='submit' name='commit' value='Commit' onclick='return confirmChangeTournament()'>
      </c:otherwise>
    </c:choose>
            
    <c:if test="${not empty teamCurrentTournament.nextTournament}">
      <input type='submit' name='advance' value='Advance Team To Next Tournament'>
    </c:if>

    <c:if test="${not empty teamPrevTournament}">
      <input type='submit' name='demote' value='Demote Team To Previous Tournament' onclick='return confirmDemoteTeam()'>
	</c:if>
	        
    <c:if test="${not addTeam}">
      <c:choose>        
        <c:when test="${inPlayoffs}">
          <p>Teams cannot be deleted once they are in the playoff data table for tournament.</p>
        </c:when>
        <c:otherwise>
          <input type='submit' name='delete' value='Delete Team' onclick='return confirmDeleteTeam()'>
        </c:otherwise>
      </c:choose>
    </c:if>
    </form>
      
    <form action="index.jsp" method='post'>
      <input type='submit' value='Cancel'>
    </form>
      
    
  </body>
</html>

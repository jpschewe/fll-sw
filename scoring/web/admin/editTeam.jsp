<%--
  This page is used for editing and adding teams.  The request parameter
  addTeam is set when this page is being used to add a team.
  --%>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.SessionAttributes" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="java.text.NumberFormat" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<script language='javascript'>
//confirm deleting a team
function confirmDeleteTeam() {
  return confirm("Are you sure you want to delete team ${teamNumber}?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}
        
//confirm demoting a team
function confirmDemoteTeam() {
  return confirm("Are you sure you want to demote team ${teamNumber}?  Any data associated with that team and for the current tournament (${teamCurrentTournamentName}) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}

//confirm changing the current tournament
function confirmChangeTournament() {
  <c:if test="${null == teamPrevTournament}" var="test">
    if(document.editTeam.currentTournament.value != "${teamCurrentTournament}") {
      return confirm("Are you sure you want to change the tournament for team ${teamNumber}?  Any data associated with that team and for the current tournament (${teamCurrentTournamentName}) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
    } else {
      return true;
    }
  </c:if>
  <c:if test="${not test}">
    return true;
  </c:if>
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
        <td>Region</td>
        <td><input type='text' name='region' value='${region }'></td>
      </tr>
      <tr>
        <td>Division (required)</td>
        <td><input type='text' name='division' value='${division }'></td>
      </tr>

        <tr>
          <td>Current Tournament</td>
          <td>
          <c:choose>
          <c:when test="${empty teamPrevTournament}">
            <select name='currentTournament'>
            <c:forEach items='${tournaments}' var='mapEntry'>
              <c:choose>
                <c:when test="${mapEntry.key == teamCurrentTournament }">
                  <option value='${mapEntry.key }' selected>${mapEntry.value }</option>
                </c:when>
                <c:otherwise>
                  <option value='${mapEntry.key }'>${mapEntry.value }</option>
                </c:otherwise>
              </c:choose>
            </c:forEach>
            </select>
          </c:when>
          <c:otherwise>
            ${teamCurrentTournamentName }
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
            
    <c:if test="${not empty teamNextTournament}">
      <input type='submit' name='advance' value='Advance Team To Next Tournament'>
    </c:if>

    <c:if test="${not empty teamPrevTournament}">
      <input type='submit' name='demote' value='Demote Team To Previous Tournament' onclick='return confirmDemoteTeam()'>
	</c:if>
	                
    <c:if test="${not addTeam}">
      <input type='submit' name='delete' value='Delete Team' onclick='return confirmDeleteTeam()'>
    </c:if>

    </form>
      
    <form action="index.jsp" method='post'>
      <input type='submit' value='Cancel'>
    </form>
      
    
  </body>
</html>

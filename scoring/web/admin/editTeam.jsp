<%--
  This page is used for editing and adding teams.  The request parameter
  addTeam is set when this page is being used to add a team.
  --%>
  
<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%@ page import="fll.db.Queries" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page import="java.text.NumberFormat" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<script language='javascript'>
function init() {  
<%
final Connection connection = (Connection)application.getAttribute("connection");
        
final List tournamentNames = Queries.getTournamentNames(connection);

int teamNumber = -1;
String currentTournament = null;
String prevTournament = null;
String nextTournament = null;
%>
<c:if test="${empty param.addTeam}">
<%
  teamNumber = NumberFormat.getInstance().parse(request.getParameter("TeamNumber")).intValue();
        
  currentTournament = Queries.getTeamCurrentTournament(connection, teamNumber);
  prevTournament = Queries.getTeamPrevTournament(connection, teamNumber, currentTournament);
  nextTournament = Queries.getNextTournament(connection, currentTournament);
          
  final Statement stmt = connection.createStatement();
  final ResultSet rs = stmt.executeQuery("SELECT TeamNumber,TeamName,Organization,Region,Division FROM Teams WHERE TeamNumber = " + teamNumber);
  rs.next();
  final String teamName = rs.getString(2);
  final String organization = rs.getString(3);
  final String region = rs.getString(4);
  final String division = rs.getString(5);
  SQLFunctions.closeResultSet(rs);
  SQLFunctions.closeStatement(stmt);
%>
  document.editTeam.teamName.value = "<%= teamName %>";
  document.editTeam.organization.value = "<%= organization == null ? "" : organization %>";
  document.editTeam.region.value = "<%= region == null ? "" : region %>";
  document.editTeam.division.value = "<%= division == null ? "" : division %>";
</c:if>
} //end init

//confirm deleting a team
function confirmDeleteTeam() {
  return confirm("Are you sure you want to delete team <%=teamNumber%>?  Any data associated with that team will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}
        
//confirm demoting a team
function confirmDemoteTeam() {
  return confirm("Are you sure you want to demote team <%=teamNumber%>?  Any data associated with that team and for the current tournament (<%=currentTournament%>) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
}

//confirm changing the current tournament
function confirmChangeTournament() {
  <c:if test="${null == prevTournament}" var="test">
    if(document.editTeam.currentTournament.value != "<%=currentTournament%>") {
      return confirm("Are you sure you want to change the tournament for team <%=teamNumber%>?  Any data associated with that team and for the current tournament (<%=currentTournament%>) will be removed from the database, including any scores that have been entered.  You also need to download the files for subjective score entry again.  It is not advisable to do this while the tournament that the team is in is running.");
    } else {
      return true;
    }
  </c:if>
  <c:if test="${not test}">
    return true;
  </c:if>
}
        
</script>  
    <title><x:out select="$challengeDocument/fll/@title"/> (Edit Team)</title>
  </head>

  <body onload='init()'>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Edit Team)</h1>

    <form action="CommitTeam" method="post" name="editTeam">
    <c:if test="${not empty param.addTeam}">
      <input type='hidden' name='addTeam' value='<c:out value="${param.addTeam}" />'>
    </c:if>
        
    <table border='1'>
          
      <tr>
        <td>Team Number (required)</td>
            
        <c:choose>
          <c:when test="${empty param.addTeam}">
            <td><input type='hidden' name='teamNumber' value='<%=teamNumber%>'><%=teamNumber%></td>
          </c:when>
          <c:otherwise>
            <td><input type='text' name='teamNumber'></td>
          </c:otherwise>
        </c:choose>
      </tr>
          
      <tr>
        <td>Team Name</td>
        <td><input type='text' name='teamName' size='64'></td>
      </tr>
      <tr>
        <td>Organization</td>
        <td><input type='text' name='organization' size='64'></td>
      </tr>
      <tr>
        <td>Region</td>
        <td><input type='text' name='region'></td>
      </tr>
      <tr>
        <td>Division (required)</td>
        <td><input type='text' name='division'></td>
      </tr>

      <c:if test="${empty param.addTeam}">
        <tr>
          <td>Current Tournament</td>

          <td>
          <%
          if(null == prevTournament) {
          %>
            <select name='currentTournament'>
              <%
              final Iterator iter = tournamentNames.iterator();
              while(iter.hasNext()) {
              final String tournamentName = (String)iter.next();
              if(tournamentName.equals(currentTournament)) {
              %>
                  <option value='<%=tournamentName%>' selected>
              <%
              } else {
              %>
                  <option value='<%=tournamentName%>'>
              <%
              }//end else
              out.println(tournamentName);
              %>
                </option>
              <%
              }//end while
              %>
            </select>
          <%
          } else {
          %>
            <%=currentTournament%>
          <%
          }
          %>
          </td>
        </tr>
      </c:if>
            
    </table>
    <c:if test="${not empty param.addTeam}" var="addTeam">
    <input type='submit' name='commit' value='Commit'>
    </c:if>
    <c:if test="${not addTeam}">
      <input type='submit' name='commit' value='Commit' onclick='return confirmChangeTournament()'>
    </c:if>
            
    <c:if test="${empty param.addTeam}">
    <%
    if(null != nextTournament) {
    %>
      <input type='submit' name='advance' value='Advance Team To Next Tournament'>
    <%
    }
    %>
    </c:if>

    <%
    if(null != prevTournament) {
    %>
      <input type='submit' name='demote' value='Demote Team To Previous Tournament' onclick='return confirmDemoteTeam()'>
    <%
    }
    %>
                
    <c:if test="${empty param.addTeam}">
      <input type='submit' name='delete' value='Delete Team' onclick='return confirmDeleteTeam()'>
    </c:if>

    </form>
      
    <form action="index.jsp" method='post'>
      <input type='submit' value='Cancel'>
    </form>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>    
  </body>
</html>

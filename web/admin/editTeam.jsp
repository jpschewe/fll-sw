<%@ page errorPage="../errorHandler.jsp" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
  
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
  
<%@ page import="org.w3c.dom.Document" %>

<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page import="java.text.NumberFormat" %>

<html>
  <head>
<script language='javascript'>
function init() {  
<%
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final Connection connection = (Connection)application.getAttribute("connection");
final List tournamentNames = Queries.getTournamentNames(connection);

  
final String addTeam = request.getParameter("addTeam");
final String currentTournament;
final String entryTournament;
final String howFoundOut;
final int teamNumber;
if(!"1".equals(addTeam)) {
  teamNumber = NumberFormat.getInstance().parse(request.getParameter("teamNumber")).intValue();
  final Statement stmt = connection.createStatement();
  final ResultSet rs = stmt.executeQuery("SELECT TeamNumber,TeamName,Organization,Coach,Email,Phone,City,EntryTournament,Division,NumBoys,NumGirls,NumMedals,HowFoundOut,CurrentTournament FROM Teams WHERE TeamNumber = " + teamNumber);
  rs.next();
  final String teamName = rs.getString(2);
  final String organization = rs.getString(3);
  final String coach = rs.getString(4);
  final String email = rs.getString(5);
  final String phone = rs.getString(6);
  final String city = rs.getString(7);
  entryTournament = rs.getString(8);
  final String division = rs.getString(9);
  final String numBoys = rs.getString(10);
  final String numGirls = rs.getString(11);
  final String numMedals = rs.getString(12);
  howFoundOut = rs.getString(13);
  currentTournament = rs.getString(14);
  Utilities.closeResultSet(rs);
  Utilities.closeStatement(stmt);
%>
  document.editTeam.teamName.value = "<%=teamName%>";
  document.editTeam.organization.value = "<%=organization%>";
  document.editTeam.coach.value = "<%=coach%>";
  document.editTeam.email.value = "<%=email%>";
  document.editTeam.phone.value = "<%=phone%>";
  document.editTeam.city.value = "<%=city%>";
  document.editTeam.division.value = "<%=division%>";
  document.editTeam.numBoys.value = "<%=numBoys%>";
  document.editTeam.numGirls.value = "<%=numGirls%>";
  document.editTeam.numMedals.value = "<%=numMedals%>";
<%
} else {
  teamNumber = -1;
  currentTournament = "";
  entryTournament = "";
  howFoundOut = "";
}
%>
}
</script>  
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Team)</title>
  </head>

  <body background='<c:url value="/images/bricks1.gif"/>' bgcolor='#ffffff' topmargin='4' onload='init()'>
    <h1><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Edit Team)</h1>

    <form action="commitTeam.jsp" method="POST" name="editTeam">
    <input type='hidden' name='addTeam' value='<%=addTeam%>'>
    <table border='1'>
      <tr>
        <td>Team Number (required)</td>
<%if(!"1".equals(addTeam)) {%>
        <td><input type='hidden' name='teamNumber' value='<%=teamNumber%>'><%=teamNumber%></td>
<%} else {%>
        <td><input type='text' name='teamNumber'></td>
<%}%>
      </tr>
      <tr>
        <td>Team Name</td>
        <td><input type='text' name='teamName'></td>
      </tr>
      <tr>
        <td>Organziation</td>
        <td><input type='text' name='organization'></td>
      </tr>
      <tr>
        <td>Coach</td>
        <td><input type='text' name='coach'></td>
      </tr>
      <tr>
        <td>Email</td>
        <td><input type='text' name='email'></td>
      </tr>
      <tr>
        <td>Phone</td>
        <td><input type='text' name='phone'></td>
      </tr>
      <tr>
        <td>City</td>
        <td><input type='text' name='city'></td>
      </tr>
      <tr>
        <td>Entry Tournament (required)</td>
        <td>
          <select name='entryTournament'>
          <%
          Iterator tournamentsIter = tournamentNames.iterator();
          while(tournamentsIter.hasNext()) {
            final String tournamentName = (String)tournamentsIter.next();
            out.print("<option value='" + tournamentName + "'");
            if(entryTournament.equals(tournamentName)) {
              out.print(" selected");
            }
            out.println(">" + tournamentName + "</option>");
          }
          %>
          </select>
        </td>
      </tr>
      <tr>
        <td>Current Tournament (required)</td>
        <td>
          <select name='currentTournament'>
          <%
          tournamentsIter = tournamentNames.iterator();
          while(tournamentsIter.hasNext()) {
            final String tournamentName = (String)tournamentsIter.next();
            out.print("<option value='" + tournamentName + "'");
            if(currentTournament.equals(tournamentName)) {
              out.print(" selected");
            }
            out.println(">" + tournamentName + "</option>");
          }
          %>
          </select>
        </td>
      </tr>
      <tr>
        <td>Division (required)</td>
        <td><input type='text' name='division'></td>
      </tr>
      <tr>
        <td>Number of Boys</td>
        <td><input type='text' name='numBoys'></td>
      </tr>
      <tr>
        <td>Number of Girls</td>
        <td><input type='text' name='numGirls'></td>
      </tr>
      <tr>
        <td>Number of Medals</td>
        <td><input type='text' name='numMedals'></td>
      </tr>
      <tr>
        <td>How found out about FLL</td>
        <td><textarea name='howFoundOut' cols='20' rows='3'><%=howFoundOut%></textarea></td>
      </tr>
<%if(!"1".equals(addTeam)) {%>
      <tr>
        <td>Delete team</td>
        <td><input type='checkbox' name='deleteTeam' value='1'></td>
      </tr>
<%}%>
    </table>
    <input type='submit' value='Commit'>
    </form>
    <form action="index.jsp" method='post'>
    <input type='submit' value='Cancel'>
    </form>
      
<%@ include file="/WEB-INF/jspf/footer.jspf" %>    
  </body>
</html>

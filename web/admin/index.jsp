<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<%@ page import="fll.Queries" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page import="java.text.NumberFormat" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
final StringBuffer message = new StringBuffer();
final String messageReq = request.getParameter("message");
if(null != messageReq) {
  message.append("<i>");
  message.append(messageReq);
  message.append("</i><br>");
}

final String currentTournamentParam = request.getParameter("currentTournament");
if(null != currentTournamentParam && !"".equals(currentTournamentParam)) {
  if(!Queries.setCurrentTournament(connection, currentTournamentParam)) {
    response.sendRedirect(response.encodeRedirectURL("tournaments.jsp?unknownTournament=" + currentTournamentParam));
  } else {
    message.append("<i id='success'>Tournament changed to " + currentTournamentParam + "</i><br>");
  }
}

if(null != request.getParameter("changeSeedingRounds")) {
  final String newSeedingRoundsStr = request.getParameter("seedingRounds");
  final int newSeedingRounds = NumberFormat.getInstance().parse(newSeedingRoundsStr).intValue();
  Queries.setNumSeedingRounds(connection, newSeedingRounds);
  message.append("<i id='success'>Changed number of seeding arounds to " + newSeedingRounds + "</i><br>");
}

if(null != request.getParameter("addTournamentsForRegions")) {
  Queries.insertTournamentsForRegions(connection);
  message.append("<i id='success'>Successfully added tournaments for regions</i>");
}
        
final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
final String currentTournament = Queries.getCurrentTournament(connection);
        
%>
      
<html>
  <head>
    <title><x:out select="$challengeDocument/fll/@title"/> (Administration)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Administration)</h1>

    <p><%=message.toString()%></p>
        
    <p>Before tournament day:</p>
    <ol>
      <li>
        <form id='uploadTeams' ACTION='<c:url value="uploadTeams.jsp"/>' METHOD="POST" ENCTYPE="multipart/form-data">
          Upload the datafile for teams.  The filter functionality provided
          here is very basic and has very limited feedback.  It's suggested
          that you edit the input file before upload to contain only the teams
          for your tournament(s).
          <input type="file" size="32" name="file1">
          <input type="submit" value="Upload">
        </form>
      </li>

      <li><a href='<c:url value="index.jsp">
                     <c:param name="addTournamentsForRegions" value="1" />
                   </c:url>'>
          Add tournaments for all Regions</a></li>

      <li><a href='<c:url value="tournamentInitialization.jsp"/>'>Initialize tournament teams by region</a></li>

      <li><a href='<c:url value="tournaments.jsp"/>'>Edit Tournaments</a></li>

      <li>
        <form id='currentTournament' action='<c:url value="index.jsp"/>' method="post">
          Current Tournament: <select name='currentTournament'>
              <%
              final Statement stmt = connection.createStatement();
              final ResultSet rs = stmt.executeQuery("Select Name,Location from Tournaments ORDER BY Name");
              while(rs.next()) {
                final String tournament = rs.getString(1);
                final String location = rs.getString(2);
                out.print("<option value='" + tournament + "'");
                if(currentTournament.equals(tournament)) {
                  out.print(" selected");
                }
                out.println(">" + location + " [ " + tournament + " ]</option>");
              }
              rs.close();
              stmt.close();
              %>
              </select>
          <input type='submit' value='Change tournament'>
        </form>
      </li>
          
      <li><a href='<c:url value="judges.jsp"/>'>Assign Judges</a></li>
      
      <li><a href='<c:url value="tables.jsp"/>'>Assign Table Labels</a> (for scoresheet printing during playoffs)</li>

      <li><form id='changeSeedingRounds' action='<c:url value="index.jsp"/>' method='post'>Select the number of seeding runs.
          <select name='seedingRounds'>
<%
for(int i=1; i<=10; i++) {
  out.print("<option value='" + i + "'");
  if(numSeedingRounds == i) {
    out.print(" selected");
  }
  out.println(">" + i + "</option>");
}
%>
          </select>
          <input type='submit' name='changeSeedingRounds' value='Commit'>
          </form>
       </li>
          
    </ol>

    <p>Tournament day:</p>
    <ol>
      <li><a href='<c:url value="editTeam.jsp">
                     <c:param name="addTeam" value="1"/>
                   </c:url>'>Add a team</a></li>
          
      <li><a href='<c:url value="select_team.jsp"/>'>Edit team data</a></li>
            
      <li><a href='<c:url value="/getfile.jsp">
                     <c:param name="filename" value="subjective.zip"/>
                   </c:url>'>Download the datafile for subjective score entry.</a>  Should be downloaded after each subjective score upload to lessen chance of data loss due to overwrite. </li>
      <li>
        <form name='uploadSubjective' ACTION='<c:url value="uploadSubjectiveData.jsp"/>' METHOD="POST" ENCTYPE="multipart/form-data">
          Upload the datafile for subjective scores.
          <input type="file" size="32" name="file1">
          <input type="submit" value="Upload">
        </form>
      </li>

      <li><a href='remoteControl.jsp'>Remote control of display</a></li>
    </ol>

    <p>After the tournament</p>
    <ul>
      <li><a href="advanceTeams.jsp">Advance teams</a></li>
    </ul>
          
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
  
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>

<%@ page import="java.sql.Connection" %>
  
<%
Queries.ensureTournamentTeamsPopulated(application);

final Connection connection = (Connection)application.getAttribute("connection");
      
final Map tournamentTeams = (Map)Queries.getTournamentTeams(connection);
final String currentTournament = Queries.getCurrentTournament(connection);
final List divisions = Queries.getDivisions(connection);
final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
  
if(null != request.getParameter("division")) {
  application.setAttribute("playoffDivision", request.getParameter("division"));
}
if(null != request.getParameter("runNumber")) {
  application.setAttribute("playoffRunNumber", Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("runNumber")));
}
  
if(null == application.getAttribute("playoffDivision")) {
  application.setAttribute("playoffDivision", divisions.get(0));
}
if(null == application.getAttribute("playoffRunNumber")) {
  application.setAttribute("playoffRunNumber", new Integer(numSeedingRounds + 1));
}

final String playoffDivision = (String)application.getAttribute("playoffDivision");
final int playoffRunNumber = ((Number)application.getAttribute("playoffRunNumber")).intValue();
%>
  
<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff's)</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Playoff menu)</h1>
      <ol>
        <li>First you should check to make sure all of the teams have the
        correct number of rounds.  You can use <a href="check.jsp">this
        page</a> to check that.  If any teams show up on this page you'll want
        to try and fix that first.</li>

        <li>
          <B>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br>
          <form action='main.jsp' method='get'>
            Goto the bracket page for division <select name='division'>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
<option value='<%=div%>'><%=div%></option>
<%
}
}
%>
            </select>
            <input type='submit' value='Goto Playoffs'>
              (requires Internet Explorer)
          </form>               
        </li>

        <li>
          <form action='adminbrackets.jsp' method='get'>
            Goto the admin/printable bracket page for division <select name='division'>
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
%>
<option value='<%=div%>'><%=div%></option>
<%
}
}
%>
            </select>
            <input type='submit' value='Goto Playoffs'>
          </form>               
        </li>

        <li><a href="remoteMain.jsp">Goto remotely controled brackets</a></li>

        <li>
          <form action='index.jsp' method='post'>
            Select the division and run number to display in the remote brackets.
            Division: <select name='division'> 
<%
{
final Iterator divisionIter = divisions.iterator();
while(divisionIter.hasNext()) {
  final String div = (String)divisionIter.next();
  out.print("<option value='" + div + "'");
  if(playoffDivision.equals(div)) {
    out.print(" selected");
  }
  out.println(">" + div + "</option>");
}
}
%>
            </select>
            Run number (playoff round): <select name='runNumber'>
<%
final int numTeams = tournamentTeams.size();
for(int numRounds = 1; /*numTeams > Math.pow(2, numRounds)*/ numRounds < 10; numRounds++) {
  out.print("<option value='" + (numRounds + numSeedingRounds) + "'");
  if(playoffRunNumber == (numRounds + numSeedingRounds)) {
    out.print(" selected");
  }
  out.println(">" + (numRounds + numSeedingRounds) + " (" + numRounds + ")</option>");
}
%>
            </select>

            <input type='submit' value='Change values'>
          </form>
        </li>
      </ol>
    </p>



<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

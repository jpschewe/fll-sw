<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.Map" %>
      
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
      
<%
final Connection connection = (Connection)application.getAttribute("connection");
      
final List divisions = Queries.getDivisions(connection);
final Map tournamentTeams = (Map)Queries.getTournamentTeams(connection);
final int numSeedingRounds = Queries.getNumSeedingRounds(connection);

if(null != request.getParameter("division")) {
  application.setAttribute("playoffDivision", request.getParameter("division"));
}
if(null != request.getParameter("runNumber")) {
  application.setAttribute("playoffRunNumber", Utilities.NUMBER_FORMAT_INSTANCE.parse(request.getParameter("runNumber")));
}
  
if(null == application.getAttribute("playoffDivision") && !divisions.isEmpty()) {
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
    <title><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <c:if test="${empty displayPage}">
    <c:set var='displayPage' value='welcome' scope='application'/>
  </c:if>
          
  <c:if test="${not empty param.remotePage}">
    <c:set var='displayPage' value='${param.remotePage}' scope='application'/>
  </c:if>
          
  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Display Controller)</h1>

    <p>This page is used to control what page is currently visible on the display screen</p>

    <form name='remote' action='remoteControl.jsp' method='post'>
      <c:if test='${displayPage == "welcome"}'  var='welcomePage'>
        Welcome <input type='radio' name='remotePage' value='welcome' checked/><br/>
      </c:if>
      <c:if test='${not welcomePage}'>
        Welcome <input type='radio' name='remotePage' value='welcome' /><br/>
      </c:if>

      <c:if test='${displayPage == "scoreboard"}'  var='scoreboardPage'>
        Scoreboard <input type='radio' name='remotePage' value='scoreboard' checked/><br/>
      </c:if>
      <c:if test='${not scoreboardPage}'>
        Scoreboard <input type='radio' name='remotePage' value='scoreboard' /><br/>
      </c:if>

      
      <c:if test='${displayPage == "playoffs"}'  var='playoffsPage'>
        Playoffs <input type='radio' name='remotePage' value='playoffs' checked/>
      </c:if>
      <c:if test='${not playoffsPage}'>
        Playoffs <input type='radio' name='remotePage' value='playoffs' />
      </c:if>
      <b>WARNING: Do not select brackets until all seeding runs have been recorded!</b><br/>
            
      <input type='submit' value='Change Display Page'/>
    </form>

          <form action='remoteControl.jsp' method='post'>
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
            

<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

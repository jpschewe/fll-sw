
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.Utilities" %>
<%@ page import="fll.web.playoff.Playoff" %>
  
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>
  
<%
/*
  application parameters
  playoffDivision - String for the division
  playoffRunNumber - int for the run number, counted since the very first performance round
*/
  
Queries.ensureTournamentTeamsPopulated(application);

final Map tournamentTeams = (Map)application.getAttribute("tournamentTeams");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final String currentTournament = (String)application.getAttribute("currentTournament");
final Connection connection = (Connection)application.getAttribute("connection");

if(null == application.getAttribute("playoffDivision")) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}
final String divisionStr = (String)application.getAttribute("playoffDivision");

final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
final int runNumber;  
//check application for runNumber
if(null == application.getAttribute("playoffRunNumber")) {
  //use seeding round + 1
  runNumber = numSeedingRounds + 1;
} else {
  runNumber = ((Number)application.getAttribute("playoffRunNumber")).intValue();
}

final List currentRound;
final Number sessionRunNumber = (Number)session.getAttribute("runNumber");
if(null == sessionRunNumber || sessionRunNumber.intValue() != runNumber) {
  int tempRunNumber;
  List tempCurrentRound;
  if(null == sessionRunNumber
     || sessionRunNumber.intValue() > runNumber) {
    tempRunNumber = numSeedingRounds + 1;
    tempCurrentRound = Playoff.buildInitialBracketOrder(connection, currentTournament, divisionStr, tournamentTeams);
  } else {
    tempRunNumber = sessionRunNumber.intValue();
    tempCurrentRound = (List)session.getAttribute("currentRound");
  }
  
  while(tempRunNumber < runNumber) {
    final List newCurrentRound = new LinkedList();
    final Iterator prevIter = tempCurrentRound.iterator();
    while(prevIter.hasNext()) {
      final Team teamA = (Team)prevIter.next();
      final Team teamB = (Team)prevIter.next();
      final Team winner = Playoff.pickWinner(connection, challengeDocument, currentTournament, teamA, teamB, tempRunNumber);
      newCurrentRound.add(winner);
    }
    tempCurrentRound = newCurrentRound;
    tempRunNumber++;
  }
  currentRound = tempCurrentRound;
  session.setAttribute("currentRound", currentRound);
  session.setAttribute("runNumber", new Integer(runNumber));
} else {
  currentRound = (List)session.getAttribute("currentRound");
}

%>

<html>
  <head>
    <title><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Playoff Brackets) Division: <%=divisionStr%> Run Number: <%=runNumber%></title>
  </head>
  <style type='text/css'>
      TD.Leaf {color:#ffffff;font-family:Arial;background-color:#000000}
      TD.Bridge {background-color:#808080}
      FONT {font-family:Arial}
      FONT.TeamNum {color:#ff8080;font-weight:bold}
      FONT.TeamName {color:#ffffff;font-weight:bold}
      FONT.TeamScore {color:#ffffff;font-weight:bold;font-size:10pt}
      FONT.TIE {color:#ff0000;font-weight:bold}
  </style>

<!-- stuff for automatic scrolling -->
<script language="JavaScript">
function myHeight() {
  return (document.all.dummy.offsetHeight-300);
}

function myScroll() {
  documentYposition += scrollAmount;
  window.scroll(0,documentYposition);
  if (documentYposition > documentLength) {
    window.clearInterval(IntervalRef);
<%
  final String reloadURL = response.encodeURL("remoteControlBrackets.jsp");
%>
      
    location.href='<%=reloadURL%>';
  }
}

function start() {
    documentLength = myHeight();
    //myScroll();
    IntervalRef = window.setInterval('myScroll()',iInterval);
}

var iInterval = 30;
var IntervalRef;
var documentLength;
var scrollAmount = 2;    // scroll by 100 pixels each time
var documentYposition = 0;
</script>
<!-- end stuff for automatic scrolling -->
    
  <body background="../images/bricks1.gif" bgcolor="#ffffff" topmargin='4' onload='start()'>
    <!-- dummy tag and some blank lines for scolling -->
    <div id="dummy" style="position:absolute"><br><br><br><br><br><br><br><br><br><br><br><br><br>

    <h2><%=challengeDocument.getDocumentElement().getAttribute("title")%> (Playoff Brackets Division: <%=divisionStr%> Run Number: <%=runNumber%>)</h2>
      <form action='brackets.jsp' method='get'>
        <input type='hidden' name='runNumber' value='<%=runNumber%>'>
        <input type='hidden' name='division' value='<%=divisionStr%>'>

<%if(currentRound.size() > 2) {%>
        <input type='submit' name='nextRound' value='Next Round (<%=runNumber+1%>)'><br><br>
<%}%>
                                          
      <table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
<%
if(currentRound.size() > 1) { //need at least 2 teams
final Iterator currentIter = currentRound.iterator();
for(int index=0; currentIter.hasNext(); index++) {
  final boolean evenBracket = ( ( (index/2) * 2) == index );
  final Team teamA = (Team)currentIter.next();
  final Team teamB = (Team)currentIter.next();
  final Team winner = Playoff.pickWinner(connection, challengeDocument, currentTournament, teamA, teamB, runNumber);
%>
        <tr> <!-- row 1 -->
          <td class='Leaf' width='200'>
            <%=Playoff.getDisplayString(connection, currentTournament, runNumber, teamA)%>
          </td>
            
          <!-- connect teamA and teamB -->
          <td class='Bridge' rowspan='5' width='10'>&nbsp;</td>

          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <td width='10'>&nbsp;</td>
<%} else {%>
          <!-- skip column for bracket-bracket bar -->
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>
        
        <tr> <!-- row 2 -->
          <td width='200'>&nbsp;</td>
          <!-- skip column for A-B bar -->
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <td width='10'>&nbsp;</td>
<%} else {%>
          <!-- skip column for bracket-bracket bar -->
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>
        
        <tr> <!-- row 3 -->
          <td width='200'><font size='4'>Bracket <%=index+1%></font><br></td>
          <!-- skip column for A-B bar -->
          <td class='Leaf' width='200'>
<%if(currentRound.size() > 2) {%>
            <%=Playoff.getDisplayString(connection, currentTournament, (runNumber+1), winner)%>
<%} else {%>
                &nbsp;
<%}%>
          </td>
              
<%if(evenBracket) {%>
              
<%  if(currentRound.size() > 2) {%>
          <!-- vertical bar down to next bracket -->
          <td rowspan='9' class='Bridge' width='10'>&nbsp;</td>
<%  } else {%>
          <td rowspan='6' width='10'>&nbsp;</td>
<%  }%>
              
<%} else {%>
          <!-- skip column for bracket-bracket bar -->
<%}%>

          <td width='200'>&nbsp;</td>
        </tr>
        
        <tr> <!-- row 4 -->
          <td width='200'>&nbsp;</td>
          <!-- skip column for A-B bar -->
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <!-- skip column for bracket-bracket bar -->
<%} else {%>
          <td width='10'>&nbsp;</td>
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>
        
        <tr> <!-- row 5 -->
          <td class='Leaf' width='200'>
            <%=Playoff.getDisplayString(connection, currentTournament, runNumber, teamB)%>
          </td>
          <!-- skip column for A-B bar -->
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <!-- skip column for bracket-bracket bar -->
<%} else {%>
          <td width='10'>&nbsp;</td>
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>

        <tr> <!-- between row 1 -->
          <td width='200'>&nbsp;</td>
          <td width='10'>&nbsp;</td>
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <!-- skip column for bracket-bracket bar -->
<%} else {%>
          <td width='10'>&nbsp;</td>
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>

        <tr> <!-- between row 2 -->
          <td width='200'>&nbsp;</td>
          <td width='10'>&nbsp;</td>
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <!-- skip column for bracket-bracket bar -->
              
<%  if(currentRound.size() > 2) {%>
          <!-- winner of next bracket -->
          <td class='Leaf' width='200'>&nbsp;</td>
<%  } else {%>
          <td width='200'>&nbsp;</td>
<%  }%>
              
<%} else {%>
          <td width='10'>&nbsp;</td>
          <td width='200'>&nbsp;</td>
<%}%>
        </tr>

        <tr> <!-- between row 3 -->
          <td width='200'>&nbsp;</td>
          <td width='10'>&nbsp;</td>
          <td width='200'>&nbsp;</td>
<%if(evenBracket) {%>
          <!-- skip column for bracket-bracket bar -->
<%} else {%>
          <td width='10'>&nbsp;</td>
<%}%>
          <td width='200'>&nbsp;</td>
        </tr>
<%}%>
      </table>

<%if(currentRound.size() > 2) {%>
        <input type='submit' name='nextRound' value='Next Round (<%=runNumber+1%>)'>
<%
  }
}//end if we have more than 2 teams
%>
      </form>
      <%@ include file="../WEB-INF/jspf/footer.jspf" %>
    </body>
  </html>

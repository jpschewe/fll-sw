<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
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
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = Queries.getCurrentTournament(connection);

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

int tempRunNumber = numSeedingRounds + 1;
List tempCurrentRound = Playoff.buildInitialBracketOrder(connection, divisionStr, tournamentTeams);

while(tempRunNumber < runNumber) {
  final List newCurrentRound = new LinkedList();
  final Iterator prevIter = tempCurrentRound.iterator();
  while(prevIter.hasNext()) {
    final Team teamA = (Team)prevIter.next();
    final Team teamB = (Team)prevIter.next();
    final Team winner = Playoff.pickWinner(connection, challengeDocument, teamA, teamB, tempRunNumber);
    newCurrentRound.add(winner);
  }
  tempCurrentRound = newCurrentRound;
  tempRunNumber++;
}
final List currentRound = tempCurrentRound;

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets) Division: <%=divisionStr%> Run Number: <%=runNumber%></title>
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
<script type="text/javascript">
var scrollTimer;
var scrollAmount = 2;    // scroll by 100 pixels each time
var documentYposition = 0;
//http://www.evolt.org/article/document_body_doctype_switching_and_more/17/30655/index.html
function getScrollPosition() {
  if (window.pageYOffset) {
    return window.pageYOffset
  } else if (document.documentElement && document.documentElement.scrollTop) {
    return document.documentElement.scrollTop
  } else if (document.body) {
    return document.body.scrollTop
  }
}
function myScroll() {
  documentYposition += scrollAmount;
  window.scrollBy(0, scrollAmount);
  if(getScrollPosition()+300 < documentYposition) { //wait 300 pixels until we refresh
    window.clearInterval(scrollTimer);
    window.scroll(0, 0); //scroll back to top and then refresh
<%
  final String reloadURL = response.encodeURL("remoteControlBrackets.jsp");
%>
      
    location.href='<%=reloadURL%>';
  }
}

function start() {
    scrollTimer = window.setInterval('myScroll()',30);
}
</script>
<!-- end stuff for automatic scrolling -->
    
  <body onload='start()'>
    <!-- dummy tag and some blank lines for scolling -->
    <div id="dummy" style="position:absolute"><br><br><br><br><br><br><br><br><br><br><br><br><br>

    <h2><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets Division: <%=divisionStr%> Run Number: <%=runNumber%>)</h2>
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
  final Team winner = Playoff.pickWinner(connection, challengeDocument, teamA, teamB, runNumber);
%>
        <tr> <!-- row 1 -->
          <td class='Leaf' width='200'>
            <%=Playoff.getDisplayString(connection, currentTournament, runNumber, teamA, teamB)%>
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
            <%=Playoff.getDisplayString(connection, currentTournament, (runNumber+1), winner, null)%>
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
            <%=Playoff.getDisplayString(connection, currentTournament, runNumber, teamB, teamA)%>
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
      <%@ include file="/WEB-INF/jspf/footer.jspf" %>
    </body>
  </html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.db.Queries" %>
<%@ page import="fll.web.playoff.BracketData" %>
  
<%@ page import="java.sql.Connection" %>
  
<%
/*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
*/

final Connection connection = (Connection)application.getAttribute("connection");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final String currentTournament = Queries.getCurrentTournament(connection);

if(null == application.getAttribute("playoffDivision")) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}
final String division = (String)application.getAttribute("playoffDivision");

final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection,division);

final int playoffRoundNumber;  
//check application for playoffRoundNumber
if(null == application.getAttribute("playoffRoundNumber")) {
  playoffRoundNumber = 1;
} else {
  playoffRoundNumber = ((Number)application.getAttribute("playoffRoundNumber")).intValue();
}

final BracketData bracketInfo =
  new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber+2, 4, false);

bracketInfo.addBracketLabels(playoffRoundNumber);

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff Round <%=playoffRoundNumber%>, Division <%=division%>)</title>
  </head>
  <style type='text/css'>
      TD.Leaf {color:#ffffff;font-family:Arial;background-color:#000000}
      TD.Bridge {background-color:#808080}
      FONT {font-family:Arial}
      FONT.TeamNumber {color:#ff8080;font-weight:bold}
      FONT.TeamName {color:#ffffff;font-weight:bold}
      FONT.TeamScore {color:#ffffff;font-weight:bold;font-size:10pt}
      FONT.TIE {color:#ff0000;font-weight:bold}
  </style>

<!-- stuff for automatic scrolling -->
<script type="text/javascript">
var scrollTimer;
var scrollAmount = 2;    // scroll by 100 pixels each time
var documentYposition = 0;
var scrollPause = 100; // amount of time, in milliseconds, to pause between scrolls

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
    scrollTimer = window.setInterval('myScroll()',scrollPause);
}
</script>
<!-- end stuff for automatic scrolling -->

  <body onload='start()'>
    <!-- dummy tag and some blank lines for scolling -->
    <div id="dummy" style="position:absolute"><br><br><br><br><br><br><br><br><br><br><br><br><br>

    <h2><x:out select="$challengeDocument/fll/@title"/> (Playoff Round <%=playoffRoundNumber%>, Division <%=division%>)</h2>
      <form action='brackets.jsp' method='get'>
        <input type='hidden' name='playoffRoundNumber' value='<%=playoffRoundNumber%>'>
        <input type='hidden' name='division' value='<%=division%>'>

      <table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
<%
if(playoffRoundNumber <= numPlayoffRounds) {


for(int rowIndex = 1; rowIndex <= bracketInfo.getNumRows(); rowIndex++)
{
%>
        <tr>
<%
  // Get each cell. Insert bridge cells between columns.
  for(int i = bracketInfo.getFirstRound(); i < bracketInfo.getLastRound(); i++)
  {
%>
          <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, i)%>
          <%=bracketInfo.getHtmlBridgeCell(rowIndex,i,BracketData.TopRightCornerStyle.MEET_TOP_OF_CELL)%>
<%
  }
%>
          <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, bracketInfo.getLastRound())%>
        </tr>
<%
}
}//end if we have more than 2 teams
%>
      </form>
      <%@ include file="/WEB-INF/jspf/footer.jspf" %>
    </body>
  </html>

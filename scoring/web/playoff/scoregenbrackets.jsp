<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.web.playoff.BracketData" %>

<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="org.w3c.dom.Document" %>

<%
/*
  Parameters:
    division - String for the division
*/
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
  final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
  final int currentTournament = Queries.getCurrentTournament(connection);

  final String divisionStr = request.getParameter("division");
  if(null == divisionStr) {
    throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
  }

  final String specifiedFirstRound = request.getParameter("firstRound");
  final String specifiedLastRound = request.getParameter("lastRound");
  int firstRound;
  try {
    firstRound = Integer.parseInt(specifiedFirstRound);
  } catch(NumberFormatException nfe) {
    firstRound = 1;
  }

  final int lastColumn = 1 + Queries.getNumPlayoffRounds(connection, divisionStr);

  int lastRound;
  try {
    lastRound = Integer.parseInt(specifiedLastRound);
  } catch(NumberFormatException nfe) {
    lastRound = lastColumn;
  }

  // Sanity check that the last round is valid
  if(lastRound < 2) {
    lastRound = 2;
  }
  if(lastRound > lastColumn) {
    lastRound = lastColumn;
  }
  // Sanity check that the first round is valid
  if(firstRound < 1) {
    firstRound = 1;
  }
  if(firstRound > 1 && firstRound >= lastRound) {
    firstRound = lastRound-1; // force the display of at least 2 rounds
  }

  final BracketData bracketInfo =
    new BracketData(connection, divisionStr, firstRound, lastRound, 4, true, false);

  final int numMatches = bracketInfo.addBracketLabelsAndScoreGenFormElements(connection, currentTournament, divisionStr);
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Playoff Brackets - Division: <%=divisionStr%></title>
  </head>
  <style type='text/css'>
      TD.Leaf {font-family:Arial;border-bottom: solid}
      TD.BridgeBottom {border-left: solid; border-right: solid; border-bottom: solid}
      TD.BridgeMiddle {border-left: solid; border-right: solid}
      TD.BridgeTop {border-bottom: solid}
      FONT {font-family:Arial}
      FONT.TeamNumber {font-weight:bold}
      FONT.TeamName {font-weight:bold}
      FONT.TeamScore {font-weight:bold;font-size:10pt}
      FONT.TIE {color:#ff0000;font-weight:bold}
  </style>

  <body>
    <h2><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets Division: <%=divisionStr%>)</h2>
    <p><a href="index.jsp">Return to Playoff menu</a></p>
      <form name='printScoreSheets' method='post' action='<c:url value="/GetFile"/>' target='_new'>
      <input type='hidden' name='numMatches' value='<%=numMatches %>'/>
      <input type='submit' value='Print scoresheets'/>
      <input type='hidden' name='filename' value='scoreSheet.pdf'/>
      <table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
      <%=bracketInfo.getHtmlHeaderRow()%>
<%  for(int rowIndex = 1; rowIndex <= bracketInfo.getNumRows(); rowIndex++) { %>
        <tr>

<%    // Get each cell. Insert bridge cells between columns.
      for(int i = bracketInfo.getFirstRound(); i < bracketInfo.getLastRound(); i++) { %>
          <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, i)%>
          <%=bracketInfo.getHtmlBridgeCell(rowIndex,i,BracketData.TopRightCornerStyle.MEET_BOTTOM_OF_CELL)%>
<%    } %>
          <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, bracketInfo.getLastRound())%>
        </tr>
<%  } %>
    </table>
    <p><a href="index.jsp">Return to Playoff menu</a></p>
    </form>
    
  </body>
</html>

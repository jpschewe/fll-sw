<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="org.w3c.dom.Document" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
<%@ page import="fll.web.playoff.Playoff" %>
<%@ page import="fll.web.playoff.BracketData" %>
  
<%@ page import="java.util.Map" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Connection" %>
  
<%
/*
  Parameters:
    division - String for the division
*/
  
final Connection connection = (Connection)application.getAttribute("connection");
final Document challengeDocument = (Document)application.getAttribute("challengeDocument");
final String currentTournament = Queries.getCurrentTournament(connection);

final String divisionStr = request.getParameter("division");
if(null == divisionStr) {
  throw new RuntimeException("No division specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
}

final int lastColumn = 1 + Queries.getNumPlayoffRounds(connection, divisionStr);

final BracketData bracketInfo =
  new BracketData(connection, divisionStr, 1, lastColumn, 4);

for(int i = 1; i < lastColumn; i++) {
  bracketInfo.addBracketLabels(i);
}

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets) Division: <%=divisionStr%></title>
  </head>
  <style type='text/css'>
      TD.Leaf {font-family:Arial;border-bottom: solid}
      TD.Bridge {border-left: solid; border-right: solid; border-bottom: solid}
      TD.BridgeTop {border-bottom: solid}
      FONT {font-family:Arial}
      FONT.TeamNumber {font-weight:bold}
      FONT.TeamName {font-weight:bold}
      FONT.TeamScore {font-weight:bold;font-size:10pt}
      FONT.TIE {color:#ff0000;font-weight:bold}
  </style>

  <body>
    <h2><x:out select="$challengeDocument/fll/@title"/> (Playoff Brackets Division: <%=divisionStr%>)</h2>
    <table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
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
    <!--  % Playoff.displayPrintableBrackets(connection, challengeDocument, divisionStr, out); % -->
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

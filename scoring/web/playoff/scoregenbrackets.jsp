<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>

<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="fll.db.TableInformation"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="org.w3c.dom.Document"%>
<%@ page import="java.util.List" %>

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>


<%
/*
Parameters:
division - String for the division
firstRound - Integer first round to display
lastRond - Integer last round to display
*/

fll.web.playoff.ScoregenBrackets.populateContext(application,
					request, pageContext);
%>

<%
			final DataSource datasource = ApplicationAttributes
					.getDataSource(application);
			final Connection connection = datasource.getConnection();
			final int currentTournament = Queries
					.getCurrentTournament(connection);

			final String divisionStr = (String) pageContext
					.getAttribute("division");

			final int firstRound = (Integer) request.getAttribute("firstRound");
			final int lastRound = (Integer) request.getAttribute("lastRound");

			final BracketData bracketInfo = new BracketData(connection,
					divisionStr, firstRound, lastRound, 4, true, false);

			final int numMatches = bracketInfo
					.addBracketLabelsAndScoreGenFormElements(connection,
							currentTournament, divisionStr);
      
      final List<TableInformation> tableInfo = TableInformation.getTournamentTableInformation(connection,
                                                                                              currentTournament,
                                                                                              bracketInfo.getBracketName());
      pageContext.setAttribute("tableInfo", tableInfo);

%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/fll-sw.css'/>" />
<title>${division} - Head to head Bracket</title>

<style type='text/css'>
TD.Leaf {
	font-family: Arial;
	border-bottom: solid
}

TD.BridgeBottom {
	border-left: solid;
	border-right: solid;
	border-bottom: solid
}

TD.BridgeMiddle {
	border-left: solid;
	border-right: solid
}

TD.BridgeTop {
	border-bottom: solid
}

FONT {
	font-family: Arial
}

FONT.TeamNumber {
	font-weight: bold
}

FONT.TeamName {
	font-weight: bold
}

FONT.TeamScore {
	font-weight: bold;
	font-size: 10pt
}

FONT.TIE {
	color: #ff0000;
	font-weight: bold
}
</style>

<script type='text/javascript'>
	function matchTables(selectChanged, selectMatch) {

		<c:forEach items="${tableInfo}" var="info">

		if (selectChanged.val() == "${info.sideA}") {
			if (selectMatch.val() != "${info.sideB}") {
				selectMatch.val("${info.sideB}");
			}
			return;
		} else if (selectChanged.val() == "${info.sideB}") {
			if (selectMatch.val() != "${info.sideA}") {
				selectMatch.val("${info.sideA}");
			}
			return;
		}

		</c:forEach>
	}

	$(document).ready(function() {
<%=bracketInfo.outputTableSyncFunctions()%>
	});
</script>

<script type='text/javascript' src='scoregenbrackets.js'></script>

</head>


<body>
 <h2>Head to head Bracket: ${division }</h2>
 <p>
  <a href="index.jsp">Return to Head to head menu</a>
 </p>
 
 <form name='printScoreSheets' method='post' action='ScoresheetServlet'
  target='_new'>
  <input type='hidden' name='division' value='<%=divisionStr%>' /> <input
   type='hidden' name='numMatches' value='<%=numMatches%>' /> <input
   type='submit' value='Print scoresheets' id='print_scoresheets' onclick='return checkSomethingToPrint()'/> - <b>Print the
   scoresheets for the matches that have their boxes checked.</b>


  <%=bracketInfo.outputBrackets(BracketData.TopRightCornerStyle.MEET_BOTTOM_OF_CELL)%>

  <p>
   <a href="index.jsp">Return to Head to head menu</a>
  </p>
 </form>

</body>
</html>

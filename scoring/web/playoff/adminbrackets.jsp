<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="javax.sql.DataSource"%>

<%
  /*
			Parameters:
			division - String for the division
			*/

			final DataSource datasource = ApplicationAttributes.getDataSource(application);
			final Connection connection = datasource.getConnection();
			final int currentTournament = Queries.getCurrentTournament(connection);

			final String divisionStr = request.getParameter("division");
			if (null == divisionStr) {
				throw new RuntimeException(
						"No playoff bracket specified, please go back to the <a href='index.jsp'>playoff main page</a> and start again.");
			}
			pageContext.setAttribute("bracketName", divisionStr);

			final String specifiedFirstRound = request.getParameter("firstRound");
			final String specifiedLastRound = request.getParameter("lastRound");
			int firstRound;
			try {
				firstRound = Integer.parseInt(specifiedFirstRound);
			} catch (NumberFormatException nfe) {
				firstRound = 1;
			}

			final int lastColumn = 1 + Queries.getNumPlayoffRounds(connection, divisionStr);

			int lastRound;
			try {
				lastRound = Integer.parseInt(specifiedLastRound);
			} catch (NumberFormatException nfe) {
				lastRound = lastColumn;
			}

			// Sanity check that the last round is valid
			if (lastRound < 2) {
				lastRound = 2;
			}
			if (lastRound > lastColumn) {
				lastRound = lastColumn;
			}
			// Sanity check that the first round is valid
			if (firstRound < 1) {
				firstRound = 1;
			}
			if (firstRound > 1 && firstRound >= lastRound) {
				firstRound = lastRound - 1; // force the display of at least 2 rounds
			}

			pageContext.setAttribute("firstRound", firstRound);
			pageContext.setAttribute("lastRound", lastRound);

			final BracketData bracketInfo = new BracketData(connection, divisionStr, firstRound, lastRound, 4, true,
					false);

			for (int i = 1; i < lastColumn; i++) {
				bracketInfo.addBracketLabels(i);
			}
			bracketInfo.addStaticTableLabels();
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<title><%=divisionStr%> Printable Playoff Bracket</title>
</head>

<script type="text/javascript">
  var bracketName = '${bracketName}';
  var bracketIndex = 0; // only a single bracket is displayed
  var firstRound = parseInt("${firstRound}");
  var lastRound = parseInt("${lastRound}");
</script>


<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>
<script
  type='text/javascript'
  src='h2hutils.js'></script>
<script
  type='text/javascript'
  src='adminbrackets.js'></script>

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

.TABLE_ASSIGNMENT {
	font-family: monospace;
	font-size: 85%;
	font-weight: bold;
	background-color: white;
	padding-left: 5%;
	padding-right: 5%
}
</style>

<body>

  <h2>
    Playoff Bracket:
    <%=divisionStr%></h2>

  <%=bracketInfo.outputBrackets(BracketData.TopRightCornerStyle.MEET_BOTTOM_OF_CELL)%>

</body>
</html>

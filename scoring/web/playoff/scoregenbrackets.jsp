<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>

<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="org.w3c.dom.Document"%>

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
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/fll-sw.css'/>" />
<title>${division} Playoff Brackets</title>

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

</head>


<body>
 <h2>Playoff Brackets Division: ${division }</h2>
 <p>
  <a href="index.jsp">Return to Playoff menu</a>
 </p>
 <form name='limit_tables' method='post' action='LimitTableAssignments'>
  You can limit which tables teams are automatically assigned to.<br />

  <input type='hidden' name='division' value='${division }' />
  <input type='hidden' name='firstRound' value='${firstRound }' />
  <input type='hidden' name='lastRound' value='${lastRound }' />

  <c:forEach items="${tableInfo}" var="info">

   <label for='${info.id }'>${info.sideA } / ${info.sideB }</label>

   <c:choose>
    <c:when test="${info.use }">
     <input type="checkbox" name='tables' value='${info.id}' checked />
    </c:when>
    <c:otherwise>
     <input type="checkbox" name='tables' value='${info.id}' />
    </c:otherwise>
   </c:choose>
   <br />

  </c:forEach>

  <input type='submit' value='Limit Tables' />

 </form>


 <form name='printScoreSheets' method='post' action='ScoresheetServlet'
  target='_new'>
  <input type='hidden' name='division' value='<%=divisionStr%>' /> <input
   type='hidden' name='numMatches' value='<%=numMatches%>' /> <input
   type='submit' value='Print scoresheets' id='print_scoresheets' /> - <b>Print the
   scoresheets for the brackets that have their boxes checked.</b>


  <%=bracketInfo.outputBrackets(BracketData.TopRightCornerStyle.MEET_BOTTOM_OF_CELL)%>

  <p>
   <a href="index.jsp">Return to Playoff menu</a>
  </p>
 </form>

</body>
</html>

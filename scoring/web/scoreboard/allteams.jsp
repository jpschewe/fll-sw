<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = (String)application.getAttribute("currentTournament");
final Statement stmt = connection.createStatement();
final String countSQL = "SELECT COUNT(*)"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Teams.Organization, Performance.RunNumber";
ResultSet rs = stmt.executeQuery(countSQL);
rs.next();
final int numRecords = rs.getInt(1);
Utilities.closeResultSet(rs);

final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Region, Teams.Division, Performance.Tournament, Performance.RunNumber, Performance.Bye, Performance.NoShow, Performance.ComputedTotal"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Teams.Organization, Teams.TeamNumber, Performance.RunNumber";
rs = stmt.executeQuery(sql);
%>

<HTML>
<head>
<style>
	FONT {color: #ffffff; font-family: "Arial"}
	TABLE.A {background-color:#000080 }
	TABLE.B {background-color:#0000d0 }
</style>

<script language="JavaScript">
function myHeight() {
  return (document.all.dummy.offsetHeight-300);
}

function myScroll() {
  documentYposition += scrollAmount;
  window.scroll(0,documentYposition);
  if (documentYposition > documentLength) {
    window.clearInterval(IntervalRef);
    location.href='allteams.jsp'
  }
}

function start() {
    documentLength = myHeight();
    //myScroll();
<% if (numRecords >= 4) {  %>
    IntervalRef = window.setInterval('myScroll()',iInterval);
<% } else { %>
    window.setTimeout('location.href="allteams.jsp"',30000);
<% } %>
}

<% if (numRecords >= 4) { %>
var iInterval = 30;
<% } else { %>
var iInterval = 10000;
<% } %>
var IntervalRef;
var documentLength;
var scrollAmount = 2;    // scroll by 100 pixels each time
var documentYposition = 0;
</script>


</head>

<body bgcolor='#000080' onload='start()'>
<div id="dummy" style="position:absolute">
<br><br><br><br><br><br><br><br>
<%@ include file="teams_body.jsp" %>
</div>
</body>
<%
  Utilities.closeResultSet(rs);
  Utilities.closeStatement(stmt);
%>
</HTML>

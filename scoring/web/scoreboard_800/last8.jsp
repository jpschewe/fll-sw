
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = Queries.getCurrentTournament(connection);
final Statement stmt = connection.createStatement();
final String sql = "SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, Teams.Division, Performance.Tournament, Performance.RunNumber, Performance.Bye, Performance.NoShow, Performance.TimeStamp, Performance.ComputedTotal"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " ORDER BY Performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 8";
final ResultSet rs = stmt.executeQuery(sql);
%>
  
<HTML>
<head>
<style>
	FONT {color: #ffffff; font-family: "Arial"}
</style>
<script language=javascript>
	window.setInterval("location.href='last8.jsp'",30000);
</script>
</head>

<body bgcolor='#000080'>
<center>
<table border='0' cellpadding='0' cellspacing='0' width='98%'>
<tr align='center'>
  <td colspan='6'><font size='3'><b>Most Recent Performance Scores</b></font></td>
</tr>
<!--<tr>
  <td colspan='6'><img src='../images/blank.gif' width='1' height='4'></td>
</tr>-->
<tr align='center' valign='middle'>
  <td width='10%'><font size='2'><b>Team&nbsp;Num.</b></font></td>
  <td width='28%'><font size='2'><b>Team&nbsp;Name</b></font></td>
<!--  <td><font size='2'><b><br>Organization</b></font></td> -->
  <td width='5%'><font size='2'><b>Div.</b></font></td>
  <td width='5%'><font size='2'><b>Run</b></font></td>
  <td width='8%'><font size='2'><b>Score</b></font></td>
</tr>
<tr>
  <td colspan='6' align='center'>
<!-- scores here -->
	<table border='1' bordercolor='#aaaaaa' cellpadding='4' cellspacing='0' width='100%'>
	
<%
while(rs.next()) {
%>	
	  <tr align='left'>
	    <td width='10%' align='right'>
	      <font size='3'>
	      <%=rs.getInt("TeamNumber")%>
	      </font>
	    </td>
	    <td width='28%'>
	      <font size='3'>
	      <%
              String teamName = rs.getString("TeamName");
              if(null != teamName && teamName.length() > 20) {
                teamName = teamName.substring(0, 20);
              }
              out.println(teamName + "&nbsp;");
              %>
	      </font>
	    </td>
<!--	    <td>
	      <font size='3'>
	      <%
              String organization = rs.getString("Organization");
              if(null != organization && organization.length() > 35) {
                organization = organization.substring(0, 35);
              }
              out.println(organization + "&nbsp;");
              %>
	      </font>
	    </td> -->
	    <td width='5%' align='right'>
	      <font size='3'>
	      <%=rs.getString("Division")%>
	      </font>
	    </td>
	    <td width='5%' align='right'>
	      <font size='3'>
	      <%=rs.getInt("RunNumber")%>
	      </font>
	    </td>
	    <td width='8%' align='right'>
	      <font size='3'>
              <%if(rs.getBoolean("NoShow")) {%>
                No&nbsp;Show
              <%} else if(rs.getBoolean("Bye")) {%>
                Bye
              <%} else {
                  out.println(rs.getInt("ComputedTotal"));
                }
              %>
	      </font>
	    </td>
	  </tr>
<%
}
%>
	</table>
<!-- end scores -->
  </td>
</tr>
</table>
</center>
</body>
<%
  Utilities.closeResultSet(rs);
  Utilities.closeStatement(stmt);
%>
</HTML>

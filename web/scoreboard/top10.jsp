
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page import="java.util.List" %>
  
<%@ page import="java.text.ParseException" %>
  
<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = (String)application.getAttribute("currentTournament");

final List divisions = Queries.getDivisions(connection);
  
String divisionIndexStr = request.getParameter("divisionIndex");
final int divisionIndex;
if(null == divisionIndexStr) {
  divisionIndex = 0;
} else {
  int divIndex = -1;
  try {
    divIndex = Utilities.NUMBER_FORMAT_INSTANCE.parse(divisionIndexStr).intValue();
  } catch(final ParseException pe) {
    divIndex = 0;
  }
  if(divIndex >= divisions.size() || divIndex < 0) {
    divisionIndex = 0;
  } else {
    divisionIndex = divIndex;
  }
}
final String headerColor;
if((divisionIndex / 2) * 2 == divisionIndex) {
  //even
  headerColor = "#800000";
} else {
  //odd
  headerColor = "#008000";
}
final String division = (String)divisions.get(divisionIndex);
  
final Statement stmt = connection.createStatement();
final String sql = "SELECT Teams.TeamName, Teams.Organization, Performance.TeamNumber, MAX(Performance.ComputedTotal) AS MaxOfComputedScore, Performance.NoShow, Performance.Bye"
  + " FROM Teams,Performance"
  + " WHERE Performance.Tournament = '" + currentTournament + "'"
  + " AND Teams.TeamNumber = Performance.TeamNumber"
  + " AND Performance.RunNumber <= " + Queries.getNumSeedingRounds(connection)
  + " AND Performance.NoShow = 0"
  + " AND Teams.Division = " + division
  + " GROUP BY Performance.TeamNumber"
  + " ORDER BY MaxOfComputedScore DESC LIMIT 10";
final ResultSet rs = stmt.executeQuery(sql);
%>

<HTML>
<head>
<style>
        FONT {color: #ffffff; font-family: "Arial"}
</style>
<script language=javascript>
  window.setInterval("location.href='top10.jsp?divisionIndex=<%=(divisionIndex+1)%>'",30000);
</script>
</head>

<body bgcolor='#000080'>
<center>
<table border='0' cellpadding='0' cellspacing='0' width='98%'>
<tr align='center'>
  <td colspan='5' bgcolor='<%=headerColor%>'><font size='3'><b>Top Ten Performance Scores: Division <%=division%></b></font></td>
</tr>
<tr>
  <td colspan='5'><img src='../images/blank.gif' width='1' height='4'></td>
</tr>
<tr align='center' valign='middle'>
  <td width='7%'><font size='2'><b><br>Rank</b></font></td>
  <td width='10%'><font size='2'><b>Team<br>Num.</b></font></td>
  <td width='28%'><font size='2'><b>Team<br>Name</b></font></td>
  <td><font size='2'><b><br>Organization</b></font></td>
  <td width='8%'><font size='2'><b><br>Score</b></font></td>
</tr>
<tr>
  <td colspan='5' align='center'>
<!-- scores here -->
        <table border='1' bordercolor='#aaaaaa' cellpadding='4' cellspacing='0' width='100%'>
        
<%
                int PrevScore = -1;
                int i = 1;
                int Rank = 0;
                while(rs.next() &&  i <= 10) {
                  final int score = rs.getInt("MaxOfComputedScore");
                  if(score != PrevScore) {
                    Rank = i;
                  }
%>      
          <tr align='left'>
            <td width='7%' align='right'>
              <font size='3'>
              <%=Rank%>
              </font>
            </td>
            <td width='10%' align='right'>
              <font size='3'>
              <%=rs.getInt("TeamNumber")%>
              </font>
            </td>
            <td width='28%'>
              <font size='3'>
	      <%
              String teamName = rs.getString("TeamName");
              if(null != teamName && teamName.length() > 18) {
                teamName = teamName.substring(0, 18);
              }
              out.println(teamName + "&nbsp;");
              %>
              </font>
            </td>
            <td>
              <font size='3'>
	      <%
              String organization = rs.getString("Organization");
              if(null != organization && organization.length() > 32) {
                organization = organization.substring(0, 32);
              }
              out.println(organization + "&nbsp;");
              %>
              </font>
            </td>
            <td width='8%' align='right'>
              <font size='3'>
              <%if(rs.getBoolean("NoShow")) {%>
                No Show
              <%} else if(rs.getBoolean("Bye")) {%>
                Bye
              <%} else {
                  out.println(score);
                }
              %>
              </font>
            </td>
          </tr>
<%
                        PrevScore = score;
                        i++;
                }//end while
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

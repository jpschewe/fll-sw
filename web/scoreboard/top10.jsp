<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.db.Queries" %>
  
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>

<%@ page import="java.util.List" %>
      
<%
final Connection connection = (Connection)application.getAttribute("connection");
final String currentTournament = Queries.getCurrentTournament(connection);

Integer divisionIndexObj = (Integer)session.getAttribute("divisionIndex");
int divisionIndex;
if(null == divisionIndexObj) {
  divisionIndex = 0;
} else {
  divisionIndex = divisionIndexObj.intValue();
}
divisionIndex++;
final List divisions = Queries.getDivisions(connection);
if(divisionIndex >= divisions.size()) {
  divisionIndex = 0;
}
pageContext.setAttribute("headerColor", Queries.getColorForDivisionIndex(divisionIndex));
session.setAttribute("divisionIndex", new Integer(divisionIndex));
pageContext.setAttribute("division", divisions.get(divisionIndex));
%>

<HTML>
<head>
  <style>
    FONT {color: #ffffff; font-family: "Arial"}
  </style>
  <script language=javascript>
    window.setInterval("location.href='top10.jsp'",30000);
  </script>
</head>

<body bgcolor='#000080'>
  <center>
    <table border='0' cellpadding='0' cellspacing='0' width='98%'>
      <tr align='center'>
        <td colspan='5' bgcolor='<c:out value="${headerColor}"/>'>
          <font size='3'>
            <b>Top Ten Performance Scores: Division <c:out value="${division}"/></b>
          </font>
        </td>
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
            final PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamName, Teams.Organization, Teams.TeamNumber, T2.MaxOfComputedScore FROM"
                + " (SELECT TeamNumber, MAX(ComputedTotal) AS MaxOfComputedScore FROM Performance WHERE Tournament = ? "
                + " AND RunNumber <= ? AND NoShow = False AND Bye = False GROUP BY TeamNumber) AS T2"
                + " JOIN Teams ON Teams.TeamNumber = T2.TeamNumber, current_tournament_teams WHERE Teams.TeamNumber = current_tournament_teams.TeamNumber AND current_tournament_teams.event_division = ?"
                + " ORDER BY T2.MaxOfComputedScore DESC LIMIT 10");
            prep.setString(1, currentTournament);
            prep.setInt(2, Queries.getNumSeedingRounds(connection));
            prep.setString(3, (String) pageContext.getAttribute("division"));
            final ResultSet rs = prep.executeQuery();
                
            int prevScore = -1;
            int i = 1;
            int rank = 0;
            while(rs.next()) {
              final int score = rs.getInt("MaxOfComputedScore");
              if(score != prevScore) {
                rank = i;
              }
            %>      
            <tr align='left'>
              <td width='7%' align='right'>
                <font size='3'>
                <%=rank%>
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
                <% out.println(score); %>
                </font>
              </td>
            </tr>
            <%
            prevScore = score;
            i++;
            }//end while next
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
Utilities.closePreparedStatement(prep);
%>

</HTML>

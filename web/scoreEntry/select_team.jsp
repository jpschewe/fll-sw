<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.db.Queries"%>

<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="javax.sql.DataSource" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%
final String lEditFlag = request.getParameter("EditFlag");

final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
final Statement stmt = connection.createStatement();
final ResultSet rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = '" + Queries.getCurrentTournament(connection) + "'");
final int maxRunNumber;
if(rs.next()) {
  maxRunNumber = rs.getInt(1);
} else {
  maxRunNumber = 1;
}
SQLFunctions.closeResultSet(rs);
SQLFunctions.closeStatement(stmt);

%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  <title>Score Entry [Select Team] - <x:out select="$challengeDocument/fll/@title"/></title>

  <style title='local_style' type='text/css'>
   SELECT {line-height:150%; font-size:10pt; font-weight:bold; background:black; color:#e0e0e0; }
   OPTION {color:#e0e0e0; }
   .dark_bg  {font-size:10pt; font-weight:bold; background-color:black; color:#e0e0e0; }
  </style>

<script language='javascript'>

function editFlagBoxClicked() {
  var text = document.getElementById('select_number_text');
  if(document.selectTeam.EditFlag.checked) {
    document.selectTeam.RunNumber.disabled=false;
		text.style.color = "black";
  } else {
    document.selectTeam.RunNumber.disabled=true;
		text.style.color = "gray";
  }
}

</script>
  </head>
  <body onload="editFlagBoxClicked()">

    <form action="scoreEntry.jsp" method="POST" name="selectTeam">

      <!-- top info bar -->
      <table width="100%" border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td align="center" valign="middle" bgcolor="#e0e0e0" colspan='3'>
            <table border="1" cellpadding="0" cellspacing="0" width="100%">
            <tr align="center" valign="middle"><td>

            <table border="0" cellpadding="5" cellspacing="0" width="90%">
              <tr>
                <td valign="middle" align="center">
                  <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/></font><br>
									<font face="Arial" size="2">Score Card Entry and Review Page</font>
                </td>
              </tr>
            </table>

              </td></tr>
              </table>
          </td>
        </tr>
      </table>

      <table> <!-- outer table -->
        <tr><td>

      <table>
        <tr align='left' valign='top'>
          <!-- pick team from a list -->
          <td>
            <br>
            <font face='arial' size='4'>Select team for this scorecard:</font><br>
            <select size='20' name='TeamNumber' ondblclick='selectTeam.submit()'>
              <%
              final Iterator<Team> iter = Queries.getTournamentTeams(connection).values().iterator();
              while(iter.hasNext()) {
                final Team team = iter.next();
                out.print("<option value=");
                out.print(String.valueOf(team.getTeamNumber()));
                out.print(">");
                out.print(String.valueOf(team.getTeamNumber()));
                out.print(" &nbsp;&nbsp;&nbsp;[");
                out.print(team.getTeamName());
                out.print("] ");
                out.print(team.getOrganization());
                out.print("</option>\n");
              }
              %>
            </select>
          </td>
        </tr>
        <tr><td>
        <table border='1'>
        <tr>
           <!-- check to edit -->
          <td align='left' valign='bottom'>
            <input type="checkbox"
                   name='EditFlag'
                   value="1"
                   onclick="editFlagBoxClicked()" />
            <b>Correct or double-check this score</b>
          </td>
        </tr>
        <tr>
           <!-- pick run number -->
          <td align='left'>
            <select name='RunNumber' disabled='true'>
							<option value='0'>Last Run</option>
              <% for(int i=0; i<maxRunNumber; i++) { %>
                <option value='<%=(i+1)%>'><%=(i+1)%></option>
              <% } %>
            </select>
            <b><span id='select_number_text'>Select Run Number for editing</span></b>
          </td>
        </tr>
        </table></td></tr>
        <tr>
          <!-- submit button -->
          <td align='left'>
            <!--<font face='arial' size='4'><b>Submit</b></font>-->
            <input class='dark_bg' type="submit" value="Submit">
          </td>
        </tr>

      </table>

</td></tr>

</table> <!-- outer table -->

    </form>
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Utilities" %>
<%@ page import="fll.Queries" %>
<%@ page import="fll.Team" %>

<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Connection" %>
  
<%
final String lEditFlag = request.getParameter("EditFlag");

final Connection connection = (Connection)application.getAttribute("connection");
final Statement stmt = connection.createStatement();
final ResultSet rs = stmt.executeQuery("SELECT MAX(RunNumber) FROM Performance WHERE Tournament = '" + Queries.getCurrentTournament(connection) + "'");
final int maxRunNumber;
if(rs.next()) {
  maxRunNumber = rs.getInt(1);
} else {
  maxRunNumber = 1;
}
Utilities.closeResultSet(rs);
Utilities.closeStatement(stmt);
  
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  <title><x:out select="$challengeDocument/fll/@title"/> (Select Team)</title>

  <!--<style type='text/css'>
   SELECT {line-height: 150%; font-size: 10pt; font-weight: bold; background-color: black }
   OPTION {color: #e0e0e0;}
   INPUT  {font-size: 10pt; font-weight: bold; background-color: black;color:#e0e0e0 }
  </style>-->
  <style type='text/css'>
   SELECT {line-height:150%; font-size:10pt; font-weight:bold; background:black; color:#e0e0e0; }
   OPTION {color:#e0e0e0; }
   INPUT  {font-size:10pt; font-weight:bold; background-color:black; color:#e0e0e0; }
  </style>

<script language='javascript'>

function editFlagBoxClicked() {
  if(document.selectTeam.EditFlag.checked) {
    document.selectTeam.RunNumber.disabled=false;
  } else {
    document.selectTeam.RunNumber.disabled=true;
  }        
}

</script>
  </head>
  <body>
      
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
                  <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/></font>
                </td>
              </tr>
              <tr align="center">
                <td>
                  <font face="Arial" size="4">Please Select Team:</font>
                </td>
              </tr>
            </table>
        
              </td></tr>
              </table>
          </td>
        </tr>
      </table>
    
      <table>
        <tr align='left' valign='top'>
          <!-- pick team from a list -->            
          <td>
            <br><br>
            <font face='arial' size='4'>Select Team From List:</font><br>
            <select size='20' name='TeamNumber'>
              <%
              final Iterator iter = Queries.getTournamentTeams(connection).values().iterator();
              while(iter.hasNext()) {
                final Team team = (Team)iter.next();
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
        <table border='1'><tr><td>
        <tr>
           <!-- check to edit -->
          <td align='left' valign='bottom'>
            <input type="checkbox"
                   name='EditFlag'
                   value="1"
                   onclick="editFlagBoxClicked()" />
            <b>Edit Scores?</b>
          </td>
        </tr>
        <tr>
           <!-- pick run number -->
          <td align='left'>
            <select name='RunNumber' disabled='true'>
              <% for(int i=0; i<maxRunNumber; i++) { %>
                <option value='<%=(i+1)%>'><%=(i+1)%></option>
              <% } %>
            </select>
            <b>Select Run Number for editing</b>
          </td>
        </tr>
        </td></tr></table>
        <tr>
          <!-- submit button -->            
          <td align='left'>
            <!--<font face='arial' size='4'><b>Submit</b></font>-->
            <input type="submit" value="Submit">
          </td>
        </tr>
        
      </table>
    </form>
    <%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

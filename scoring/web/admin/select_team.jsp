<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.Utilities" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="fll.web.ApplicationAttributes" %>
<%@ page import="javax.sql.DataSource" %>

<%
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
%>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  <title>Edit Team [Select Team]</title>

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

  </head>
  <body>
${message}

<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />
  
    <form action="GatherTeamData" method="POST" name="selectTeam">
      <!-- top info bar -->
      <table width="100%" border="0" cellpadding="0" cellspacing="0">
        <tr>
          <td align="center" valign="middle" bgcolor="#e0e0e0" colspan='3'>
            <table border="1" cellpadding="0" cellspacing="0" width="100%">
              <tr align="center" valign="middle">
                <td>
                  <table border="0" cellpadding="5" cellspacing="0" width="90%">
                    <tr>
                      <td valign="middle" align="center">
                        <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/></font>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </td>
        </tr>
      </table>
      <table>
        <tr align='left' valign='top'>
          <!-- pick team from a list -->
          <td>
            <font face='arial' size='4'>Select Team to Edit From List:</font><br>
            <select size='20' name='teamNumber' ondblclick='selectTeam.submit()'>
              <%
              final Statement stmt = connection.createStatement();
              final ResultSet rs = stmt.executeQuery("SELECT TeamNumber,TeamName,Organization,Division FROM Teams ORDER BY TeamNumber ASC");
              while(rs.next()) {
                final int teamNumber = rs.getInt(1);
                if(!Team.isInternalTeamNumber(teamNumber)) {
                final String teamName = rs.getString(2);
                final String organization = rs.getString(3);
                final String division = rs.getString(4);
                out.print("<option value=");
                out.print(String.valueOf(teamNumber));
                out.print(">");
                out.print(String.valueOf(teamNumber));
                out.print(" &nbsp;&nbsp;&nbsp;[");
                out.print(Utilities.trimString(teamName, Team.MAX_TEAM_NAME_LEN));
                out.print("] ");
                out.print(organization);
                out.print(" (Div ");
                out.print(division);
                out.print(")");
                out.print("</option>\n");
                }
              }
              SQLFunctions.close(rs);
              SQLFunctions.close(stmt);
              %>
            </select>
          </td>
        </tr>

        <tr>
          <td align='center'>
            <input type="submit" value="Submit">
          </td>
        </tr>
      </table>
    </form>

  </body>
</html>

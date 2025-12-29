<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN,SCORING_COORDINATOR"
    allowSetup="false" />

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions"%>
<%@ page import="net.mtu.eggplant.util.StringUtils"%>

<%@ page import="fll.Team"%>
<%@ page import="fll.Utilities"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="javax.sql.DataSource"%>

<c:set var="edit_team_referrer" scope="session">
    <%=request.getHeader("Referer")%>
</c:set>

<%
final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
%>

<!DOCTYPE html>
<html>
<head>
<title>Edit Team [Select Team]</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css" href="edit_select_team.css" />

<script type='text/javascript'
    src='<c:url value="/js/fll-functions.js" />'></script>
<script type='text/javascript' src='edit_select_team.js'></script>

</head>
<body>
    <div class='status-message'>${message}</div>

    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form action="editTeam.jsp" method="POST" name="selectTeam">
        <!-- top info bar -->
        <table width="100%" border="0" cellpadding="0" cellspacing="0">
            <tr>
                <td align="center" valign="middle" bgcolor="#e0e0e0"
                    colspan='3'>
                    <table border="1" cellpadding="0" cellspacing="0"
                        width="100%">
                        <tr align="center" valign="middle">
                            <td>
                                <table border="0" cellpadding="5"
                                    cellspacing="0" width="90%">
                                    <tr>
                                        <td valign="middle"
                                            align="center">
                                            <font face="Arial" size="4">${challengeDescription.title }</font>
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
                    <div>
                        <font face='arial' size='4'>Select Team
                            to Edit From List:</font>
                    </div>
                    <div>
                        <input type='search' placeholder='Search ...'
                            id='team_search' />
                    </div>
                    <select size='20' name='teamNumber' id='teamNumber'
                        ondblclick='selectTeam.submit()'>
                        <%
                        final Statement stmt = connection.createStatement();
                        final ResultSet rs = stmt.executeQuery("SELECT TeamNumber,TeamName,Organization FROM Teams ORDER BY TeamNumber ASC");
                        while (rs.next()) {
                        	final int teamNumber = rs.getInt(1);
                        	if (!Team.isInternalTeamNumber(teamNumber)) {
                        		final String teamName = rs.getString(2);
                        		final String organization = rs.getString(3);
                        		out.print("<option value=");
                        		out.print(String.valueOf(teamNumber));
                        		out.print(">");
                        		out.print(String.valueOf(teamNumber));
                        		out.print(" &nbsp;&nbsp;&nbsp;[");
                        		out.print(StringUtils.trimString(teamName, Team.MAX_TEAM_NAME_LEN));
                        		out.print("] ");
                        		out.print(organization);
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

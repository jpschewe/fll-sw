<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.sql.Connection"%>

<%@ page import="fll.db.Queries"%>

<%
final Connection connection = (Connection)application.getAttribute("connection");
pageContext.setAttribute("currentTournament", Queries.getCurrentTournament(connection));
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
      <tr align='center' valign='middle'>
        <td width='10%'><font size='2'><b>Team&nbsp;Num.</b></font></td>
        <td width='28%'><font size='2'><b>Team&nbsp;Name</b></font></td>
        <td width='5%'><font size='2'><b>Div.</b></font></td>
        <td width='5%'><font size='2'><b>Run</b></font></td>
        <td width='8%'><font size='2'><b>Score</b></font></td>
      </tr>
      <tr>
        <td colspan='6' align='center'>
          <sql:query var="result" dataSource="${datasource}">
            SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, current_tournament_teams.event_division, verified_performance.Tournament, verified_performance.RunNumber, verified_performance.Bye, verified_performance.NoShow, verified_performance.TimeStamp, verified_performance.ComputedTotal
              FROM Teams,verified_performance, current_tournament_teams
              WHERE verified_performance.Tournament = '<c:out value="${currentTournament}"/>'
                AND Teams.TeamNumber = verified_performance.TeamNumber
                AND Teams.TeamNumber = current_tournament_teams.TeamNumber
                AND verified_performance.Bye = False
              ORDER BY verified_performance.TimeStamp DESC, Teams.TeamNumber ASC LIMIT 8
          </sql:query>

          <!-- scores here -->
          <table border='1' bordercolor='#aaaaaa' cellpadding='4' cellspacing='0' width='100%'>
            <c:forEach items="${result.rows}" var="row">
              <tr align='left'>
                <td width='10%' align='right'>
                  <font size='3'><c:out value="${row.TeamNumber}"/></font>
                </td>
                <td width='28%'>
                  <font size='3'>
                  <c:out value="${fn:substring(row.TeamName, 0, 20)}"/>&nbsp;
                  </font>
                </td>
                <td width='5%' align='right'>
                  <font size='3'><c:out value="${row.event_division}"/></font>
                </td>
                <td width='5%' align='right'>
                  <font size='3'><c:out value="${row.RunNumber}"/></font>
                </td>
                <td width='8%' align='right'>
                  <font size='3'>
                    <c:choose>
                      <c:when test="${row.NoShow == true}">
                        No Show
                      </c:when>
                      <c:when test="${row.Bye == true}">
                        Bye
                      </c:when>
                      <c:otherwise>
                        <fmt:formatNumber value="${row.ComputedTotal}" minFractionDigits="2" maxFractionDigits="2"/>
                      </c:otherwise>
                    </c:choose>
                  </font>
                </td>
              </tr>
            </c:forEach>
          </table>
          <!-- end scores -->
        </td>
      </tr>
    </table>
  </center>
</body>
</HTML>

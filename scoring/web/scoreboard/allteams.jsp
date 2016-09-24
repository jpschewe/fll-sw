<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.scoreboard.AllTeams.populateContext(application, session, pageContext);
%>

<%@ page import="java.sql.PreparedStatement"%>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource"%>
<%@ page import="java.io.File"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Iterator"%>

<%@ page import="net.mtu.eggplant.util.sql.SQLFunctions"%>

<%@ page import="fll.Utilities"%>
<%@ page import="fll.db.Queries"%>
<%@ page import="fll.web.SessionAttributes"%>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.db.TournamentParameters"%>


<%
  final DataSource datasource = ApplicationAttributes.getDataSource(application);
			final Connection connection = datasource.getConnection();
			final int currentTournament = Queries.getCurrentTournament(connection);
			final int maxScoreboardRound = TournamentParameters.getMaxScoreboardPerformanceRound(connection,
					currentTournament);

			final PreparedStatement prep = connection.prepareStatement(
					"SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, current_tournament_teams.event_division,"
							+ " verified_performance.Tournament, verified_performance.RunNumber, verified_performance.Bye, verified_performance.NoShow, verified_performance.ComputedTotal"
							+ " FROM Teams,verified_performance,current_tournament_teams"
							+ " WHERE verified_performance.Tournament = ?"
							+ "   AND current_tournament_teams.TeamNumber = Teams.TeamNumber"
							+ "   AND Teams.TeamNumber = verified_performance.TeamNumber"
							+ "   AND verified_performance.Bye = False" + "   AND verified_performance.RunNumber <= ?"
							+ " ORDER BY Teams.TeamNumber, verified_performance.RunNumber");
			prep.setInt(1, currentTournament);
			prep.setInt(2, maxScoreboardRound);
			final ResultSet rs = prep.executeQuery();
			final List<String> divisions = Queries.getAwardGroups(connection);
%>

<html>
<head>
<link
  rel='stylesheet'
  type='text/css'
  href='../style/base.css' />
<link
  rel='stylesheet'
  type='text/css'
  href='score_style.css' />

<style>
TABLE.A {
	background-color: #000080
}

TABLE.B {
	background-color: #0000d0
}
</style>

<script
  type='text/javascript'
  src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script
  type="text/javascript"
  src="<c:url value='/extlib/jquery.scrollTo-2.1.2.min.js'/>"></script>


<script type="text/javascript">
  function reload() {
    location.reload(true);
  }

  function scrollToBottom() {
    $.scrollTo($("#bottom"), {
      duration : 100000, // FIXME needs to be based on how long the page is
      easing : 'linear',
      onAfter : reload,
    });
  }

  $(document).ready(function() {
    <c:if test="${param.allTeamsScroll}">
    scrollToBottom();
    </c:if>
  });
</script>


</head>

<body class='scoreboard'>
  <br />
  <br />
  <br />
  <br />
  <br />
  <br />
  <br />
  <br />

  <c:set
    var="colorStr"
    value="A" />

  <c:set
    var="teamIndex"
    value="0" />

  <%
    if (rs.next()) {
      boolean done = false;
      while (!done) {
  %>
  <table
    border='0'
    cellpadding='0'
    cellspacing='0'
    width='99%'
    class='<c:out value="${colorStr}" />'>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        height='15'
        width='1' /></td>
    </tr>
    <%
      final String divisionStr = rs.getString("event_division");
          final Iterator<String> divisionIter = divisions.iterator();
          boolean found = false;
          int index = 0;
          while (divisionIter.hasNext()
              && !found) {
            final String div = divisionIter.next();
            if (divisionStr.equals(div)) {
              found = true;
            } else {
              index++;
            }
          }
          final String headerColor = Queries.getColorForIndex(index);
    %>
    <tr
      class='left'
      bgcolor='<%=headerColor%>'>
      <td width='25%'>&nbsp;&nbsp;<%=divisionStr%>&nbsp;&nbsp;
      </td>
      <td class='right'>Team&nbsp;#:&nbsp;<%=rs.getInt("TeamNumber")%>&nbsp;&nbsp;
      </td>
    </tr>
    <tr class='left'>
      <td colspan='2'>&nbsp;&nbsp;<%=rs.getString("Organization")%>
      </td>
    </tr>
    <tr class='left'>
      <td colspan='2'>&nbsp;&nbsp;<%=rs.getString("TeamName")%>
      </td>
    </tr>
    <tr>
      <td colspan='2'>
        <hr
          color='#ffffff'
          width='96%' />
      </td>
    </tr>
    <tr>
      <td colspan='2'>
        <table
          border='0'
          cellpadding='1'
          cellspacing='0'>
          <tr class='center'>
            <td><img
              src='<c:url value="/images/blank.gif"/>'
              height='1'
              width='60' /></td>
            <td>Run #</td>
            <td><img
              src='<c:url value="/images/blank.gif"/>'
              width='20'
              height='1' /></td>
            <td>Score</td>
          </tr>
          <%
            int prevNum = rs.getInt("TeamNumber");
                do {
          %>
          <tr class='right'>
            <td><img
              src='<c:url value="/images/blank.gif"/>'
              height='1'
              width='60' /></td>
            <td><%=rs.getInt("RunNumber")%></td>
            <td><img
              src='<c:url value="/images/blank.gif"/>'
              width='20'
              height='1' /></td>
            <td>
              <%
                if (rs.getBoolean("NoShow")) {
              %> No Show <%
                } else if (rs.getBoolean("Bye")) {
              %> Bye <%
                } else {
                        out.print(Utilities.NUMBER_FORMAT_INSTANCE.format(rs.getDouble("ComputedTotal")));
                      }
                      if (!rs.next()) {
                        done = true;
                      }
                    } while (!done
                        && prevNum == rs.getInt("TeamNumber"));
              %>
            </td>
          </tr>
        </table>
      </td>
    </tr>

    <c:choose>
      <c:when test="${(teamIndex mod teamsBetweenLogos) == 1}">
        <tr style='background-color: white'>
          <td
            width='50%'
            style='vertical-align: middle; color: black'
            class="right">This tournament sponsored by:</td>

          <td
            width='50%'
            style='vertical-align: middle; padding: 3px'
            class="left"><img
            src='../${sponsorLogos[(teamIndex / teamsBetweenLogos) mod fn:length(sponsorLogos)] }' />

          </td>
        </tr>
      </c:when>
      <c:otherwise>
        <tr>
          <td colspan='2'><img
            src='<c:url value="/images/blank.gif"/>'
            width='1'
            height='15' /></td>
        </tr>
      </c:otherwise>
    </c:choose>
  </table>

  <c:choose>
    <c:when test="${'A' == colorStr}">
      <c:set
        var="colorStr"
        value="B" />
    </c:when>
    <c:otherwise>
      <c:set
        var="colorStr"
        value="A" />
    </c:otherwise>
  </c:choose>

  <c:set
    var="teamIndex"
    value="${teamIndex + 1 }" />

  <%
    } //end while(!done)
        //session.setAttribute("lastLogoIndex", lastLogoIndex); // save the last logo displayed so next reload starts with next sponsor
    } else {
      // no scores to display - put a table up with logos in it
    }
  %>

  <table
    border='0'
    cellpadding='0'
    cellspacing='0'
    width='99%'>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
    <tr>
      <td colspan='2'><img
        src='<c:url value="/images/blank.gif"/>'
        width='1'
        height='15' /></td>
    </tr>
  </table>


  <span id="bottom">&nbsp;</span>


</body>
<%
  SQLFunctions.close(rs);
  SQLFunctions.close(prep);
%>
</HTML>

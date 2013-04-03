<%@ include file="/WEB-INF/jspf/init.jspf"%>

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

<%
    fll.web.scoreboard.AllTeams
            .populateContext(request, application, session, pageContext);
%>


<%
  final DataSource datasource = ApplicationAttributes.getDataSource(application);
final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);
  final int maxScoreboardRound = Queries.getMaxScoreboardPerformanceRound(connection, currentTournament);


  final PreparedStatement prep = connection.prepareStatement("SELECT Teams.TeamNumber, Teams.Organization, Teams.TeamName, current_tournament_teams.event_division,"
  + " verified_performance.Tournament, verified_performance.RunNumber, verified_performance.Bye, verified_performance.NoShow, verified_performance.ComputedTotal"
  + " FROM Teams,verified_performance,current_tournament_teams"
  + " WHERE verified_performance.Tournament = ?"
  + "   AND current_tournament_teams.TeamNumber = Teams.TeamNumber"
  + "   AND Teams.TeamNumber = verified_performance.TeamNumber"
  + "   AND verified_performance.RunNumber <= ?"
  + " ORDER BY Teams.Organization, Teams.TeamNumber, verified_performance.RunNumber");
  prep.setInt(1, currentTournament);
  prep.setInt(2, maxScoreboardRound);
  final ResultSet rs = prep.executeQuery();
  final List<String> divisions = Queries.getEventDivisions(connection);
%>

<%
  //All logos shall be located under sponsor_logos in the fll web folder.
String imagePath = application.getRealPath("sponsor_logos");
File[] directories = {new  File(imagePath)};
List<String> logoFiles = new ArrayList<String>();
Utilities.buildGraphicFileList("", directories, logoFiles);

//This varible holds the index of the last image, relative to imagePath
int lastLogoIndex;
final int numLogos = logoFiles.size();
if(numLogos < 1) {
	lastLogoIndex = -1;
} else if(null != session.getAttribute("lastLogoIndex")) {
	lastLogoIndex = ((Integer)session.getAttribute("lastLogoIndex")).intValue();
} else {
	lastLogoIndex = numLogos - 1;
}
%>

<c:set var="thisURL">
 <c:url value="${pageContext.request.servletPath}">
  <c:param name="scroll" value="${param.scroll}" />
 </c:url>
</c:set>

<html>
<head>
<style>
FONT {
	color: #ffffff;
	font-family: "Arial"
}

TABLE.A {
	background-color: #000080
}

TABLE.B {
	background-color: #0000d0
}
</style>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.7.1.min.js'/>"></script>


<script type='text/javascript' src="<c:url value='/scripts/scroll.js'/>"></script>

<script type="text/javascript">
    $(document).ready(function() {
        <c:if test="${allTeamsScroll}">
        startScrolling();
        </c:if>
    });
</script>


</head>

<body bgcolor='#000080'>
 <br />
 <br />
 <br />
 <br />
 <br />
 <br />
 <br />
 <br />

 <c:set var="colorStr" value="A" />

 <%
   if (rs.next()) {
     boolean done = false;
     final int scoresBetweenLogos = 2;
     int currentScoreIndex = 0;
     while (!done) {
 %>
 <table border='0' cellpadding='0' cellspacing='0' width='99%'
  class='<c:out value="${colorStr}" />'>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    height='15' width='1'></td>
  </tr>
  <tr align='left'>
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
         final String headerColor = Queries.getColorForDivisionIndex(index);
   %>
   <td width='25%' bgcolor='<%=headerColor%>'><font size='2'><b>&nbsp;&nbsp;Division:&nbsp;<%=divisionStr%>&nbsp;&nbsp;
    </b></font></td>
   <td align='right'><font size='2'><b>Team&nbsp;#:&nbsp;<%=rs.getInt("TeamNumber")%>&nbsp;&nbsp;
    </b></font></td>
  </tr>
  <tr align='left'>
   <td colspan='2'><font size='4'>&nbsp;&nbsp;<%=rs.getString("Organization")%></font>
   </td>
  </tr>
  <tr align='left'>
   <td colspan='2'><font size='4'>&nbsp;&nbsp;<%=rs.getString("TeamName")%></font>
   </td>
  </tr>
  <tr>
   <td colspan='2'>
    <hr color='#ffffff' width='96%' />
   </td>
  </tr>
  <tr>
   <td colspan='2'>
    <table border='0' cellpadding='1' cellspacing='0'>
     <tr align='center'>
      <td><img src='<c:url value="/images/blank.gif"/>' height='1'
       width='60' /></td>
      <td><font size='4'>Run #</font></td>
      <td><img src='<c:url value="/images/blank.gif"/>' width='20'
       height='1' /></td>
      <td><font size='4'>Score</font></td>
     </tr>
     <%
       int prevNum = rs.getInt("TeamNumber");
           do {
     %>
     <tr align='right'>
      <td><img src='<c:url value="/images/blank.gif"/>' height='1'
       width='60' /></td>
      <td><font size='4'><%=rs.getInt("RunNumber")%></font></td>
      <td><img src='<c:url value="/images/blank.gif"/>' width='20'
       height='1' /></td>
      <td><font size='4'> <%
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
 %></font></td>
     </tr>
    </table>
   </td>
  </tr>
  <%
    currentScoreIndex++;
        if (numLogos > 0
            && (currentScoreIndex == scoresBetweenLogos || done)) {
          currentScoreIndex = 0;
          // display the next logo
  %>
  <tr style='background-color: white'>
   <td width='50%' style='vertical-align: middle; text-align: right'>This
    tournament sponsored by:</td>
   <td width='50%'
    style='vertical-align: middle; text-align: left; padding: 3px'>
    <%
      lastLogoIndex = (lastLogoIndex + 1)
                % numLogos;
            out.print("<img src='../"
                + logoFiles.get(lastLogoIndex) + "'/>");
    %>
   </td>
  </tr>
  <%
    } else {
  %>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <%
    }
  %>
 </table>

 <c:choose>
  <c:when test="${'A' == colorStr}">
   <c:set var="colorStr" value="B" />
  </c:when>
  <c:otherwise>
   <c:set var="colorStr" value="A" />
  </c:otherwise>
 </c:choose>

 <%
   } //end while(!done)
     session.setAttribute("lastLogoIndex", lastLogoIndex); // save the last logo displayed so next reload starts with next sponsor
   } else {
     // no scores to display - put a table up with logos in it
   }
 %>

 <table border='0' cellpadding='0' cellspacing='0' width='99%'>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
  <tr>
   <td colspan='2'><img src='<c:url value="/images/blank.gif"/>'
    width='1' height='15' /></td>
  </tr>
 </table>


</body>
<%
      SQLFunctions.close(rs);
      SQLFunctions.close(prep);
%>
</HTML>

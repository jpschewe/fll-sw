<%@ include file="/WEB-INF/jspf/init.jspf" %>
      
<%@ page import="fll.Team" %>
<%@ page import="fll.Queries" %>
<%@ page import="fll.web.scoreEntry.ScoreEntry" %>
  
<%@ page import="org.w3c.dom.Document" %>
<%@ page import="org.w3c.dom.Element" %>
  
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Map" %>


<%@ page import="java.sql.Connection" %>
  
<%
final String yesColor = "#a0ffa0";
final String noColor = "#ffa0a0";
final String blankColor = "#a0a0a0";

Queries.ensureTournamentTeamsPopulated(application);

final Document challengeDocument = (Document)application.getAttribute("challengeDocument");

final String lTeamNum = request.getParameter("TeamNumber");
if(null == lTeamNum) {
  //FIX error, redirect to error page
  //Response.Redirect("ScoreError.asp?Text=Attempted+to+load+score+entry+page+without+providing+a+team+number.")
  //response.sendError(response.SC_BAD_REQUEST, "Attempted to load score entry page without providing a team number.");
  //out.print("<!-- Error no team number -->");
  //throw exception for now
  throw new RuntimeException("Attempted to load score entry page without providing a team number.");
}

final int teamNumber = Integer.parseInt(lTeamNum);
final Map tournamentTeams = (Map)application.getAttribute("tournamentTeams");
if(!tournamentTeams.containsKey(new Integer(teamNumber))) {
  //FIX error, redirect to error page
  //response.sendError(response.SC_BAD_REQUEST, "Team number selected is not valid.");
  //out.print("<!-- Invalid team number -->");
  //throw exception for now
  throw new RuntimeException("Selected team number is not valid: " + teamNumber);
}
final Team team = (Team)tournamentTeams.get(new Integer(teamNumber));

//the next run the team will be competing in
final Connection connection = (Connection)application.getAttribute("connection");
final int nextRunNumber = Queries.getNextRunNumber(connection, team.getTeamNumber());
  
//what run number we're going to edit/enter  
final int lRunNumber;
if("1".equals(request.getParameter("EditFlag"))) {
  final String runNumberStr = request.getParameter("RunNumber");
  if(null == runNumberStr) {
    throw new RuntimeException("Please choose a run number when editing scores");
  }
  final int runNumber = NumberFormat.getInstance().parse(runNumberStr).intValue();
  if(runNumber >= nextRunNumber) {
    throw new RuntimeException("Team has not yet competed in run " + runNumber + ".  Please choose a valid run number.");
  }
  lRunNumber = runNumber;
} else {
  lRunNumber = nextRunNumber;
}

final String minimumAllowedScoreStr = ((Element)challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0)).getAttribute("minimumScore");
final int minimumAllowedScore = NumberFormat.getInstance().parse(minimumAllowedScoreStr).intValue();

//check if this is the last run a team has completed
pageContext.setAttribute("isLastRun", Boolean.valueOf(lRunNumber == (nextRunNumber - 1)));
%>
  
<html>
  <head>
    <c:if test="${not empty param.EditFlag}" var="editFlag">
      <title><x:out select="$challengeDocument//@title"/> (Score Edit)</title>
    </c:if>
    <c:if test="${not editFlag}">
      <title><x:out select="$challengeDocument//@title"/> (Score Entry)</title>
      <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    </c:if>
      
    <style type='text/css'>
      TD {font-family='arial'}
    </style>

<script language="javascript">
<!-- No Show -->
var gbl_NoShow;
function setNoShow(value) {
  gbl_NoShow = value;
  refresh();
}

function init() {
  // disable text selection
  document.onselectstart=new Function ("return false")

  <c:if test="${editFlag}">
  <%
    ScoreEntry.generateInitForScoreEdit(out, application, challengeDocument, team.getTeamNumber(), lRunNumber);
  %>
  </c:if>
  <c:if test="${not editFlag}">
    gbl_NoShow = 0;
    reset();
  </c:if>
      
  refresh();
}

<!-- reset to default values -->
<%ScoreEntry.generateReset(out, challengeDocument);%>

function refresh() {
  if(gbl_NoShow == 1) {
    reset();
    document.scoreEntry.NoShow[0].checked = true;
  } else {
    document.scoreEntry.NoShow[1].checked = true;
  }        
        
  var score = 0;

  <%ScoreEntry.generateRefreshBody(out, challengeDocument);%>

  //check for minimum total score
  if(score < <%=minimumAllowedScore%>) {
    score = <%=minimumAllowedScore%>;
  }

  document.scoreEntry.totalScore.value = score;
}

<%ScoreEntry.generateIsConsistent(out, challengeDocument);%>
    
function CancelClicked() {
  <c:if test="${editFlag}">
  if (confirm("Cancel and lose changes?") == true) {
  </c:if>
  <c:if test="${not editFlag}">
  if (confirm("Cancel and lose data?") == true) {
  </c:if>
    window.location.href= "select_team.jsp";
  }
}


<%ScoreEntry.generateIncrementMethods(out, challengeDocument);%>
</script>

  </head>

  <c:if test="${editFlag}">
    <body bgcolor="#666666" onload="init()">
  </c:if>
  <c:if test="${not editFlag}">
    <body onload="init()">
  </c:if>
    
    <form action="submit.jsp" method="POST" name="scoreEntry">

      <c:if test="${editFlag}">
        <input type='hidden' name='EditFlag' value='1' readonly>
      </c:if>
      <input type='hidden' name='RunNumber' value='<%=lRunNumber%>' readonly>
      <input type='hidden' name='TeamNumber' value='<%=team.getTeamNumber()%>' readonly>
          
      <table width='600' border="0" cellpadding="0" cellspacing="0">
        <!-- top info bar (team name etc) -->
        <tr>
          <td align="center" valign="middle">
<%if(lRunNumber <= Queries.getNumSeedingRounds(connection)) {%>
            <table border="1" cellpadding="0" cellspacing="0" width="100%" bgcolor='#e0e0e0'>
<%} else {%>
            <table border="1" cellpadding="0" cellspacing="0" width="100%" bgcolor='red'>
<%}%>
              <tr align="center" valign="middle"><td>

                  <table border="0" cellpadding="5" cellspacing="0" width="90%">
                    <tr>
                      <td valign="middle" align="center">
                        <c:if test="${editFlag}">
                          <font face="Arial" size="4"><x:out select="$challengeDocument//@title"/> (Score Edit)</font>
                        </c:if>
                        <c:if test="${not editFlag}">
                          <font face="Arial" size="4"><x:out select="$challengeDocument//@title"/> (Score Entry)</font>
                        </c:if>
                      </td>
                    </tr>
                    <tr align="center">
                      <td>
                        <font face="Arial" size="4" color='#0000ff'>#<%=team.getTeamNumber()%>&nbsp;<%=team.getOrganization()%>&nbsp;<%=team.getTeamName()%>&nbsp;--&nbsp;Run Number:&nbsp;<%=lRunNumber%></font>
                      </td>
                    </tr>
                  </table>

                </td></tr>
            </table>

          </td></tr>
      </table>
    </td>
    </tr>

      <!-- score entry -->
      <tr>
        <td align="center" valign="middle">

          <table border='1' cellpadding='3' cellspacing='0' width='600' bordercolor='#808080'>
            <tr>
              <td colspan='2'>
                <font size='4'><u>Goals:</u></font>
              </td>
              <td align='right'>
                <font size='4'><u>Count:</u></font>
              </td>
              <td align='right'>
                <font size='4'><u>Totals:</u></font>
              </td>
            </tr>

<%ScoreEntry.generateScoreEntry(out, challengeDocument);%>
              
            <!-- Total Score -->
            <tr>
              <td colspan='3'>
                <font size='4'></u>Total Score:</u></font>
              </td>
              <td align='right'>
                <input type='text' name='totalScore' size='3' readonly>
              </td>
            </tr>
            <tr>
              <td>
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font size='4' color="red">No Show:</font>
              </td>
              <td>
                <table border='0' cellpadding='0' cellspacing='0' width='150'>
                  <tr align='center'>
                    <td>
                      Yes
                      <input type='radio' name='NoShow' value='1' onclick='setNoShow(1)'>
                      No
                      <input type='radio' name='NoShow' value='0' onclick='setNoShow(0)'>
                    </td>
                  </tr>
                </table>
              </td>
              <td>&nbsp</td>
              <td>&nbsp</td>
            </tr>
            <tr>
              <td colspan='3' align='right'>
                <input type='submit' value='Submit Score' onclick='return confirm("Submit Data -- Are you sure?")'>
                <input type='button' value='Cancel' onclick='CancelClicked()'>
                <c:if test="${editFlag and isLastRun}">
                  <input type='submit' value='Delete Score' name='delete' onclick='return confirm("Are you sure you want to delete this score?")'>
                </c:if>
              </td>
              <td align='right'>
                &nbsp;
              </td>
            </tr>
          </table>

        </td>
      </tr>
    </table>
    </form>
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

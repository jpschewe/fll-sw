<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Team" %>
<%@ page import="fll.web.playoff.Playoff" %>
<%@ page import="fll.web.scoreEntry.ScoreEntry" %>

<%@ page import="org.w3c.dom.Element" %>

<%@ page import="java.util.Map" %>
<%@ page import="fll.web.ApplicationAttributes"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="fll.Utilities"%>

<%@ page import="fll.db.Queries"%>
<%@ page import="java.sql.Connection"%>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="org.w3c.dom.Document"%>

<%
final Document challengeDocument = ApplicationAttributes.getChallengeDocument(application);

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
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
final int numSeedingRounds = Queries.getNumSeedingRounds(connection);
final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
if(!tournamentTeams.containsKey(new Integer(teamNumber))) {
  //FIX error, redirect to error page
  //response.sendError(response.SC_BAD_REQUEST, "Team number selected is not valid.");
  //out.print("<!-- Invalid team number -->");
  //throw exception for now
  throw new RuntimeException("Selected team number is not valid: " + teamNumber);
}
final Team team = (Team)tournamentTeams.get(new Integer(teamNumber));

//the next run the team will be competing in
final int nextRunNumber = Queries.getNextRunNumber(connection, team.getTeamNumber());

//what run number we're going to edit/enter
final int lRunNumber;
if("1".equals(request.getParameter("EditFlag"))) {
  final String runNumberStr = request.getParameter("RunNumber");
  if(null == runNumberStr) {
    throw new RuntimeException("Please choose a run number when editing scores");
  }
  final int runNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();
  if(runNumber == 0) {
    lRunNumber = nextRunNumber - 1;
    if(lRunNumber < 1) {
      throw new RuntimeException("Selected team has no performance score for this tournament.");
    }
  } else {
    if(!Playoff.performanceScoreExists(connection, teamNumber, runNumber)) {
      throw new RuntimeException("Team has not yet competed in run " + runNumber + ".  Please choose a valid run number.");
    }
    lRunNumber = runNumber;
  }
} else {
    if(nextRunNumber > Queries.getNumSeedingRounds(connection))
	{
      if(!Queries.isPlayoffDataInitialized(connection, Queries.getEventDivision(connection, teamNumber)))
      {
        throw new RuntimeException("Selected team has completed its seeding runs. The playoff brackets"
            + " must be initialized from the playoff page"
            + " before any more scores may be entered for this team (#" + teamNumber + ")."
            + " If you were intending to double check a score, you probably just forgot to check"
            + " the box for doing so. Go <a href='javascript:back()'>back</a> and try again");
      }
	}
  lRunNumber = nextRunNumber;
}
final String roundText;
if(lRunNumber > numSeedingRounds) {
	roundText = "Playoff&nbsp;Round&nbsp;" + (lRunNumber - numSeedingRounds);
} else {
	roundText = "Run&nbsp;Number&nbsp;" + lRunNumber;
}

final String minimumAllowedScoreStr = ((Element)challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0)).getAttribute("minimumScore");
final int minimumAllowedScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(minimumAllowedScoreStr).intValue();

//check if this is the last run a team has completed
final int maxRunCompleted = Queries.getMaxRunNumber(connection, teamNumber);
pageContext.setAttribute("isLastRun", Boolean.valueOf(lRunNumber == maxRunCompleted));

//check if the score being edited is a bye
final String tournament = Queries.getCurrentTournament(connection);
pageContext.setAttribute("isBye", Boolean.valueOf(Queries.isBye(connection, tournament, teamNumber, lRunNumber)));
pageContext.setAttribute("isNoShow", Boolean.valueOf(Queries.isNoShow(connection, tournament, teamNumber, lRunNumber)));
%>

<html>
  <head>
    <c:if test="${not empty param.EditFlag}" var="editFlag">
      <title>Score Edit</title>
    </c:if>
    <c:if test="${not editFlag}">
      <title>Score Entry</title>
      <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    </c:if>

    <style type='text/css'>
      TD {font-family: arial}
    </style>

<script language="javascript">
    <c:if test="${not isBye}">
    <!-- Verified -->
    var Verified;
    function set_Verified(newValue) {
      Verified = newValue;
      refresh();
    }

<!-- No Show -->
var gbl_NoShow;
function setNoShow(value) {
  gbl_NoShow = value;
  refresh();
}
function submit_NoShow() {
 retval = confirm("Are you sure this is a 'No Show'?") 
 if(retval) {
  gbl_NoShow = 1;
  Verified = 1;
  refresh();
 }
 return retval;
}

function init() {
  // disable text selection
  document.onselectstart=new Function ("return false")

  <c:if test="${editFlag}">
  <%
    ScoreEntry.generateInitForScoreEdit(out, session, challengeDocument, team.getTeamNumber(), lRunNumber);
  %>
  </c:if>
  gbl_NoShow = 0;
  <c:if test="${not editFlag}">
        // Always init the special double-check column
    Verified = 0;
    reset();
  </c:if>

  refresh();
}

<!-- reset to default values -->
<%ScoreEntry.generateReset(out, challengeDocument);%>

function refresh() {
  document.scoreEntry.NoShow.value = gbl_NoShow;
  
  var score = 0;

  <%ScoreEntry.generateRefreshBody(out, challengeDocument);%>

  //check for minimum total score
  if(score < <%=minimumAllowedScore%>) {
    score = <%=minimumAllowedScore%>;
  }

  document.scoreEntry.totalScore.value = score;

  check_restrictions();
}

function check_restrictions() {
  var error_found = false;

<%ScoreEntry.generateCheckRestrictionsBody(out, challengeDocument);%>

  if(error_found) {
    document.getElementById("submit").disabled = true;
  } else {
    document.getElementById("submit").disabled = false;
  }
}

<%ScoreEntry.generateIsConsistent(out, challengeDocument);%>


<%ScoreEntry.generateIncrementMethods(out, challengeDocument);%>
</c:if> <!-- end check for bye -->

/**
 * Used to replace text in an element by ID.
 */
function replaceText(sId, sText) {
  var el;
  if(document.getElementById
     && (el = document.getElementById(sId))) {
     if(el.hasChildNodes) { // check for support for has child nodes, which isn't in httpunit, TODO figure out how to make this work for the unit tests as well
      while (el.hasChildNodes()) {
        el.removeChild(el.lastChild);
      }
      el.appendChild(document.createTextNode(sText));
    }
  }
}

/**
 * Called when the cancel button is clicked.
 */
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
/**
 * Called to check verified flag
 */
function verification() {
if (Verified == 1)
{
m = "You are changing a score -- are you sure?";
return m;
}
else 
{
m = "You are submitting a score without verification -- are you sure?";
return m;
}
}
</script>

  </head>

  <c:if test="${editFlag}">
      <c:if test="${isBye}">
        <body bgcolor="yellow">
      </c:if>
      <c:if test="${not isBye}">
        <body bgcolor="yellow" onload="init()">
      </c:if>
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

      <table width='100%' border="0" cellpadding="0" cellspacing="0" align="center">
        <tr>
          <td align="center" valign="middle">
          <!-- top info bar (team name etc) -->
<%if(lRunNumber <= numSeedingRounds) {%>
            <c:if test="${editFlag}">
              <table border="1" cellpadding="0" cellspacing="0" width="100%" bgcolor='yellow'>
            </c:if>
            <c:if test="${not editFlag}">
              <table border="1" cellpadding="0" cellspacing="0" width="100%" bgcolor='#e0e0e0'>
            </c:if>
<%} else {%>
            <table border="1" cellpadding="0" cellspacing="0" width="100%" bgcolor='red'>
<%}%>
              <tr align="center" valign="middle"><td>

                  <table border="0" cellpadding="5" cellspacing="0" width="90%"> <!--  inner box on title -->
                    <tr>
                      <td valign="middle" align="center">
                        <c:if test="${editFlag}">
                          <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/> (Score Edit)</font>
                        </c:if>
                        <c:if test="${not editFlag}">
                          <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/> (Score Entry)</font>
                        </c:if>
                      </td>
                    </tr>
                    <tr align="center">
                      <td>
                        <font face="Arial" size="4" color='#0000ff'>#<%=team.getTeamNumber()%>&nbsp;<%=team.getOrganization()%>&nbsp;<%=team.getTeamName()%>&nbsp;--&nbsp;<%=roundText%></font>
                      </td>
                    </tr>
                  </table> <!--  end inner box on title -->

                </td></tr>
            </table> <!--  end top info bar -->

          </td></tr>


      <!-- score entry -->
      <tr>
        <td align="center" valign="middle">

          <table border='1' cellpadding='3' cellspacing='0' bordercolor='#808080' width="100%">
            <tr>
              <td colspan='2'>
                <font size='4'><u>Goal</u></font>
              </td>
              <td align='right'>
                <font size='4'><u>Count</u></font>
              </td>
              <td align='right'>
                <font size='4'><u>Score</u></font>
              </td>
                <td align='center'>
                  <font size='4'><u>Error Message</u></font>
                </td>
            </tr>

            <c:choose>           
            <c:when test="${isBye}">
              <tr>
                <td colspan='3'><b>Bye Run</b></td>
              </tr>
            </c:when>
            <c:when test="${isNoShow}">
              <tr>
                <td colspan='3'><b>No Show</b></td>
              </tr>
            </c:when>
            <c:otherwise>
              <%ScoreEntry.generateScoreEntry(out, challengeDocument, request);%>

              <!-- Total Score -->
              <tr>
                <td colspan='3'>
                  <font size='4'><u>Total Score:</u></font>
                </td>
                <td align='right'>
                  <input type='text' name='totalScore' size='3' readonly tabindex='-1'>
                </td>
              </tr>
              <input type='hidden' name='NoShow'/>
              <%ScoreEntry.generateVerificationInput(out, challengeDocument, request);%>
            </c:otherwise>
            </c:choose>  <!-- end check for bye -->

            <tr>
              <td colspan='3' align='right'>
                <c:if test="${not isBye and not isNoShow}">
                  <c:if test="${editFlag}">
                    <input type='submit' id='submit' name='submit' value='Submit Score' onclick='return confirm(verification())'>
                  </c:if>
                  <c:if test="${not editFlag}">
                    <input type='submit' id='submit' name='submit' value='Submit Score' onclick='return confirm("Submit Data -- Are you sure?")'>
                  </c:if>
                </c:if>
                <input type='button' value='Cancel' onclick='CancelClicked()'>
                <c:if test="${editFlag and isLastRun}">
                  <input type='submit' name='delete' value='Delete Score' onclick='return confirm("Are you sure you want to delete this score?")'>
                </c:if>
              </td>
              <td colspan='2'>
                <input type='submit' id='submit' name='submit' value='No Show' onclick='return submit_NoShow()'>
              </td>
            </tr>
          </table> <!-- end score entry table  -->

        </td>
      </tr>
    </table> <!-- end table to center everything -->
    </form> <!-- end score entry form -->
<%@ include file="/WEB-INF/jspf/footer.jspf" %>
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="fll.Team" %>
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

// support the unverified runs select box
final String lTeamNum = request.getParameter("TeamNumber");
if(null == lTeamNum) {
  session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Attempted to load score entry page without providing a team number.</p>");
  response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
  return;
}
final int dashIndex = lTeamNum.indexOf('-');
final int teamNumber;
final String runNumberStr;
if(dashIndex > 0) {
  // teamNumber - runNumber
  final String teamStr = lTeamNum.substring(0, dashIndex);
  teamNumber = Integer.parseInt(teamStr);
  runNumberStr = lTeamNum.substring(dashIndex+1);
} else {
  runNumberStr = request.getParameter("RunNumber");
  teamNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(lTeamNum).intValue();	
}
final DataSource datasource = SessionAttributes.getDataSource(session);
final Connection connection = datasource.getConnection();
final int tournament = Queries.getCurrentTournament(connection);
final int numSeedingRounds = Queries.getNumSeedingRounds(connection, tournament);
pageContext.setAttribute("numSeedingRounds", numSeedingRounds);
final Map<Integer, Team> tournamentTeams = Queries.getTournamentTeams(connection);
if(!tournamentTeams.containsKey(new Integer(teamNumber))) {
  throw new RuntimeException("Selected team number is not valid: " + teamNumber);
}
final Team team = tournamentTeams.get(new Integer(teamNumber));
pageContext.setAttribute("team", team);

//the next run the team will be competing in
final int nextRunNumber = Queries.getNextRunNumber(connection, team.getTeamNumber());

//what run number we're going to edit/enter
int lRunNumber;
if("1".equals(request.getParameter("EditFlag"))) {  
  if(null == runNumberStr) {
    session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Please choose a run number when editing scores</p>");
    response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
    return;
  }
  final int runNumber = Utilities.NUMBER_FORMAT_INSTANCE.parse(runNumberStr).intValue();
  if(runNumber == 0) {
    lRunNumber = nextRunNumber - 1;
    if(lRunNumber < 1) {
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Selected team has no performance score for this tournament.</p>");
      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
      return;
    }
  } else {
    if(!Queries.performanceScoreExists(connection, teamNumber, runNumber)) {
      session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Team has not yet competed in run " + runNumber + ".  Please choose a valid run number.</p>");
      response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
      return;
    }
    lRunNumber = runNumber;
  }
} else {
    if(nextRunNumber > numSeedingRounds)
	{
      if(!Queries.isPlayoffDataInitialized(connection, Queries.getEventDivision(connection, teamNumber))) {
        session.setAttribute(SessionAttributes.MESSAGE, "Selected team has completed its seeding runs. The playoff brackets"
            + " must be initialized from the playoff page"
            + " before any more scores may be entered for this team (#" + teamNumber + ")."
            + " If you were intending to double check a score, you probably just forgot to check"
            + " the box for doing so.</p>");
        response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
        return;
      } else if(!Queries.didTeamReachPlayoffRound(connection, nextRunNumber, teamNumber, Queries.getEventDivision(connection, teamNumber))) {
        session.setAttribute(SessionAttributes.MESSAGE, "<p class='error'>Selected team has not advanced to the next playoff round.</p>");
        response.sendRedirect(response.encodeRedirectURL("select_team.jsp"));
        return;
      }
	}
  lRunNumber = nextRunNumber;
}
pageContext.setAttribute("lRunNumber",lRunNumber);

final String roundText;
if(lRunNumber > numSeedingRounds) {
	roundText = "Playoff&nbsp;Round&nbsp;" + (lRunNumber - numSeedingRounds);
} else {
	roundText = "Run&nbsp;Number&nbsp;" + lRunNumber;
}
pageContext.setAttribute("roundText", roundText);

final String minimumAllowedScoreStr = ((Element)challengeDocument.getDocumentElement().getElementsByTagName("Performance").item(0)).getAttribute("minimumScore");
final int minimumAllowedScore = Utilities.NUMBER_FORMAT_INSTANCE.parse(minimumAllowedScoreStr).intValue();
pageContext.setAttribute("minimumAllowedScore", minimumAllowedScore);

//check if this is the last run a team has completed
final int maxRunCompleted = Queries.getMaxRunNumber(connection, teamNumber);
pageContext.setAttribute("isLastRun", Boolean.valueOf(lRunNumber == maxRunCompleted));

//check if the score being edited is a bye
pageContext.setAttribute("isBye", Boolean.valueOf(Queries.isBye(connection, tournament, teamNumber, lRunNumber)));
pageContext.setAttribute("isNoShow", Boolean.valueOf(Queries.isNoShow(connection, tournament, teamNumber, lRunNumber)));

// check if previous run is verified
final boolean previousVerified;
if(lRunNumber > 1) {
  previousVerified = Queries.isVerified(connection, tournament, teamNumber, lRunNumber-1);
} else {
  previousVerified = true;
}
pageContext.setAttribute("previousVerified", previousVerified);

if(lRunNumber <= numSeedingRounds) {
  if("1".equals(request.getParameter("EditFlag"))) {
    pageContext.setAttribute("top_info_color", "yellow");  
  } else {
    pageContext.setAttribute("top_info_color", "#e0e0e0");
  }
} else {
  pageContext.setAttribute("top_info_color", "#00ff00");
}

if("1".equals(request.getParameter("EditFlag"))) {
  pageContext.setAttribute("body_background", "yellow");  
} else {
  pageContext.setAttribute("body_background", "transparent");
}

%>

<html>
  <head>
  <c:choose>
    <c:when test="${1 == param.EditFlag}">
      <title>Score Edit</title>
    </c:when>
    <c:otherwise>
      <title>Score Entry</title>
      <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    </c:otherwise>
    </c:choose>

    <style type='text/css'>
      TD {font-family: arial}
      table#top_info {   
      background-color: ${top_info_color};   
      }
      
      body { 
      background-color: ${body_background};
      }
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

  <c:choose>
  <c:when test="${1 == param.EditFlag}">
  <%
    ScoreEntry.generateInitForScoreEdit(out, session, challengeDocument, team.getTeamNumber(), lRunNumber);
  %>
  </c:when>
  <c:otherwise>
  <%
  ScoreEntry.generateInitForNewScore(out, challengeDocument);
  %>

  </c:otherwise>
  </c:choose>

  refresh();
  
  /* Saves total score for smarter notification popups */
  savedTotalScore = document.scoreEntry.totalScore.value;
}

function refresh() {
  document.scoreEntry.NoShow.value = gbl_NoShow;
  
  var score = 0;

  <%ScoreEntry.generateRefreshBody(out, challengeDocument);%>

  //check for minimum total score
  if(score < ${minimumAllowedScore}) {
    score = ${minimumAllowedScore};
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
     if(el.hasChildNodes) { // check for support for has child nodes, which isn't in httpunit, although using selenium to test this now
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
  <c:choose>	
  <c:when test="${1 == param.EditFlag}">
  if (confirm("Cancel and lose changes?") == true) {
  </c:when>
  <c:otherwise>
  if (confirm("Cancel and lose data?") == true) {
  </c:otherwise>
  </c:choose>
    window.location.href= "select_team.jsp";
  }
}
/**
 * Called to check verified flag
 */
function verification() {
if (Verified == 1)
{
	// Smarter Score Popups
	if (savedTotalScore!=document.scoreEntry.totalScore.value)
	 {
		 m = "You are changing and verifying a score -- are you sure?";
	 }
	 else
	 {
		 m = "You are verifying a score -- are you sure?";
	 }
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

<body onload="init()">
    <form action="submit.jsp" method="POST" name="scoreEntry">

      <c:if test="${1 == param.EditFlag}">
        <input type='hidden' name='EditFlag' value='1' readonly>
      </c:if>
      <input type='hidden' name='RunNumber' value='${lRunNumber}' readonly>
      <input type='hidden' name='TeamNumber' value='${team.teamNumber}' readonly>

      <table width='100%' border="0" cellpadding="0" cellspacing="0" align="center"> <!-- info bar -->
        <tr>
          <td align="center" valign="middle">
          <!-- top info bar (team name etc) -->
            <table id='top_info' border="1" cellpadding="0" cellspacing="0" width="100%">
              <tr align="center" valign="middle"><td>

                  <table border="0" cellpadding="5" cellspacing="0" width="90%"> <!--  inner box on title -->
                    <tr>
                      <td valign="middle" align="center">
                        <c:choose>
                        <c:when test="${1 == param.EditFlag}">
                          <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/> (Score Edit)</font>
                        </c:when>
                        <c:otherwise>
                          <font face="Arial" size="4"><x:out select="$challengeDocument/fll/@title"/> (Score Entry)</font>
                        </c:otherwise>
                        </c:choose>
                      </td>
                    </tr>
                    <tr align="center">
                      <td>
                        <font face="Arial" size="4" color='#0000ff'>#${team.teamNumber}&nbsp;${team.organization}&nbsp;${team.teamName}&nbsp;--&nbsp;${roundText}</font>
                      </td>
                    </tr>
                  </table> <!--  end inner box on title -->

                </td></tr> <!-- team info -->
                
                <c:if test="${not previousVerified }">
                  <!--  warning -->
                  <tr><td bgcolor='red' align='center'>
                  <font face="Arial" size="4">Warning: Previous run for this team has not been verified!</font>                  
                </c:if>
                
            </table> <!--  end info bar -->

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
              <%ScoreEntry.generateScoreEntry(out, challengeDocument);%>

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
              <%ScoreEntry.generateVerificationInput(out);%>
            </c:otherwise>
            </c:choose>  <!-- end check for bye -->

            <tr>
              <td colspan='3' align='right'>
                <c:if test="${not isBye and not isNoShow}">
                  <c:choose>
                  <c:when test="${1 == param.EditFlag}">
                    <input type='submit' id='submit' name='submit' value='Submit Score' onclick='return confirm(verification())'>
                  </c:when>
                  <c:otherwise>
                    <input type='submit' id='submit' name='submit' value='Submit Score' onclick='return confirm("Submit Data -- Are you sure?")'>
                  </c:otherwise>
                  </c:choose>
                </c:if>
                <input type='button' id='cancel' value='Cancel' onclick='CancelClicked()'>
                <c:if test="${1 == param.EditFlag and isLastRun}">
                  <input type='submit' id='delete' name='delete' value='Delete Score' onclick='return confirm("Are you sure you want to delete this score?")'>
                </c:if>
              </td>
                <c:if test="${not isBye and not isNoShow}">
              <td colspan='2'>
                <input type='submit' id='no_show' name='submit' value='No Show' onclick='return submit_NoShow()'>
              </td>
              </c:if>
            </tr>
          </table> <!-- end score entry table  -->

        </td>
      </tr>
    </table> <!-- end table to center everything -->
    </form> <!-- end score entry form -->

  </body>
</html>

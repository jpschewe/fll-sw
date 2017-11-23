<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.scoreEntry.ScoreEntry"%>

<html>
<head>
<c:choose>
  <c:when test="${1 == EditFlag}">
    <title>Score Edit</title>
  </c:when>
  <c:otherwise>
    <title>Score Entry</title>
  </c:otherwise>
</c:choose>

<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />

<style type='text/css'>
TD {
	font-family: arial
}

table#top_info {
	background-color: ${top_info_color};

}
body {
	background-color: ${body_background};
}
table .score-entry {
	width: 100%;
}

#floating-messages {
	position: fixed;
	top: 0px;
	left: 0%;
	z-index: 99;
}

#score-errors {
	font-size: 200%;
	border: 1px solid black;
	padding: 5px;
	background-color: red;
}

#verification-warning {
	font-size: 200%;
	border: 1px solid black;
	padding: 5px;
	background-color: yellow;
}
</style>

<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>

<script type="text/javascript">
    <c:if test="${not isBye}">


<!-- No Show -->
function submit_NoShow() {
 retval = confirm("Are you sure this is a 'No Show'?") 
 if(retval) {
  document.scoreEntry.NoShow.value = true;
  Verified = 1;
  refresh();
 }
 return retval;
}

function init() {
  $("#verification-warning").hide();

  // disable text selection
  document.onselectstart=new Function ("return false")

  <c:choose>
  <c:when test="${1 == EditFlag}">
  <%ScoreEntry.generateInitForScoreEdit(out, application, session);%>
  </c:when>
  <c:otherwise>
  <%ScoreEntry.generateInitForNewScore(out, application);%>

  </c:otherwise>
  </c:choose>

  refresh();
  
  /* Saves total score for smarter notification popups */
  savedTotalScore = document.scoreEntry.totalScore.value;
}

function refresh() { 
  var score = 0;

  <%ScoreEntry.generateRefreshBody(out, application);%>

  //check for minimum total score
  if(score < ${minimumAllowedScore}) {
    score = ${minimumAllowedScore};
  }

  document.scoreEntry.totalScore.value = score;

  check_restrictions();
}

function check_restrictions() {
  var error_found = false;
  $("#score-errors").empty();
  
<%ScoreEntry.generateCheckRestrictionsBody(out, application);%>

  if(error_found) {
    $("#submit").attr('disabled', true);
    $("#score-errors").show();
  } else {
    $("#submit").attr('disabled', false);
    $("#score-errors").hide();
  }
}

<%ScoreEntry.generateIsConsistent(out, application);%>


<%ScoreEntry.generateIncrementMethods(out, application);%>
</c:if> <!-- end check for bye -->

/**
 * Called when the cancel button is clicked.
 */
function CancelClicked() {
  <c:choose>	
  <c:when test="${1 == EditFlag}">
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

$(document).ready(function() {
init();
});

</script>

</head>

<body>

  <div id="floating-messages">
    <div id="score-errors"></div>
    <div id="verification-warning">Are you sure this score has
      been Verified? Normally scores are not verified on the initial
      entry.</div>
  </div>

  <form
    action="submit.jsp"
    method="POST"
    name="scoreEntry">
    <input
      type='hidden'
      name='NoShow'
      value="false" />

    <c:if test="${1 == EditFlag}">
      <input
        type='hidden'
        name='EditFlag'
        value='1'
        readonly>
    </c:if>
    <input
      type='hidden'
      name='RunNumber'
      value='${lRunNumber}'
      readonly> <input
      type='hidden'
      name='TeamNumber'
      value='${team.teamNumber}'
      readonly>

    <table
      width='100%'
      border="0"
      cellpadding="0"
      cellspacing="0"
      align="center">
      <!-- info bar -->
      <tr>
        <td
          align="center"
          valign="middle">
          <!-- top info bar (team name etc) -->
          <table
            id='top_info'
            border="1"
            cellpadding="0"
            cellspacing="0"
            width="100%">
            <tr
              align="center"
              valign="middle">
              <td>

                <table
                  border="0"
                  cellpadding="5"
                  cellspacing="0"
                  width="90%">
                  <!--  inner box on title -->
                  <tr>
                    <td
                      valign="middle"
                      align="center"><c:choose>
                        <c:when test="${1 == EditFlag}">
                          <font
                            face="Arial"
                            size="4">Score Edit</font>
                        </c:when>
                        <c:otherwise>
                          <font
                            face="Arial"
                            size="4">Score Entry</font>
                        </c:otherwise>
                      </c:choose></td>
                  </tr>
                  <tr align="center">
                    <td><font
                      face="Arial"
                      size="4"
                      color='#0000ff'>#${team.teamNumber}&nbsp;${team.organization}&nbsp;${team.teamName}&nbsp;--&nbsp;${roundText}</font>
                    </td>
                  </tr>
                </table> <!--  end inner box on title -->

              </td>
            </tr>
            <!-- team info -->

            <c:if test="${not previousVerified }">
              <!--  warning -->
              <tr>
                <td
                  bgcolor='red'
                  align='center'><font
                  face="Arial"
                  size="4">Warning: Previous run for this team
                    has not been verified!</font>
            </c:if>

          </table> <!--  end info bar -->

        </td>
      </tr>


      <!-- score entry -->
      <tr>
        <td
          align="center"
          valign="middle">

          <table
            class='score-entry'
            border='1'
            bordercolor='#808080'>
            <colgroup>
              <!--  goal -->
              <col width="50%" />
              <!-- buttons -->
              <col width="30%" />
              <!-- count -->
              <col width="10%" />
              <!--  score -->
              <col width="10%" />
            </colgroup>
            <tr>
              <td colspan='2'><font size='4'><u>Goal</u></font></td>
              <td align='right'><font size='4'><u>Count</u></font>
              </td>
              <td align='right'><font size='4'><u>Score</u></font>
              </td>
            </tr>

            <c:choose>
              <c:when test="${isBye}">
                <tr>
                  <td colspan='3'><b>Bye Run</b></td>
                </tr>
              </c:when>
              <c:otherwise>
                <c:if test="${isNoShow}">
                  <tr>
                    <td
                      colspan='5'
                      class='center warning'>Editing a No Show -
                      submitting score will change to a real run</td>
                  </tr>
                </c:if>
                <%
                  ScoreEntry.generateScoreEntry(out, application);
                %>

                <!-- Total Score -->
                <tr>
                  <td colspan='3'><font size='4'><u>Total
                        Score:</u></font></td>
                  <td align='right'><input
                    type='text'
                    name='totalScore'
                    size='3'
                    readonly
                    tabindex='-1'></td>
                </tr>
                <%
                  ScoreEntry.generateVerificationInput(out);
                %>
              </c:otherwise>
            </c:choose>
            <!-- end check for bye -->

            <tr>
              <td
                colspan='3'
                align='right'><c:if test="${not isBye}">
                  <c:choose>
                    <c:when test="${1 == EditFlag}">
                      <input
                        type='submit'
                        id='submit'
                        name='submit'
                        value='Submit Score'
                        onclick='return confirm(verification())'>
                    </c:when>
                    <c:otherwise>
                      <input
                        type='submit'
                        id='submit'
                        name='submit'
                        value='Submit Score'
                        onclick='return confirm("Submit Data -- Are you sure?")'>
                    </c:otherwise>
                  </c:choose>
                </c:if> <input
                type='button'
                id='cancel'
                value='Cancel'
                onclick='CancelClicked()'> <c:if
                  test="${1 == EditFlag and isLastRun}">
                  <input
                    type='submit'
                    id='delete'
                    name='delete'
                    value='Delete Score'
                    onclick='return confirm("Are you sure you want to delete this score?")'>
                </c:if></td>
              <c:if test="${not isBye}">
                <td colspan='2'><input
                  type='submit'
                  id='no_show'
                  name='submit'
                  value='No Show'
                  onclick='return submit_NoShow()'></td>
              </c:if>
            </tr>
          </table> <!-- end score entry table  -->

        </td>
      </tr>
    </table>
    <!-- end table to center everything -->
  </form>
  <!-- end score entry form -->
</body>
</html>

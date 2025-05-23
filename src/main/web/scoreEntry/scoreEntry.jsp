<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.scoreEntry.ScoreEntry"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
if (!fll.web.scoreEntry.ScoreEntry.populateContext(application, request, response, session, pageContext)) {
	return;
}
%>

<html>
<head>
<c:choose>
    <c:when test="${EditFlag}">
        <title>Score Edit</title>
    </c:when>
    <c:otherwise>
        <title>Score Entry</title>
    </c:otherwise>
</c:choose>

<!--  runtype : ${runType} -->
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css" href="scoreEntry.css" />

<c:if test="${EditFlag}">
    <link rel="stylesheet" type="text/css" href="edit-score.css" />
</c:if>

<c:choose>
    <c:when test="${runType == 'PRACTICE'}">
        <link rel="stylesheet" type="text/css"
            href="runType-practice.css" />
    </c:when>
    <c:when test="${runType == 'REGULAR_MATCH_PLAY'}">
        <link rel="stylesheet" type="text/css"
            href="runType-regularMatchPlay.css" />
    </c:when>
    <c:otherwise>
        <link rel="stylesheet" type="text/css" href="runType-other.css" />
    </c:otherwise>
</c:choose>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-storage.js'/>"></script>
<script type='text/javascript' src="scoreEntryModule.js"></script>



<c:if test="${not showScores}">
    <link rel="stylesheet" type="text/css" href="hide-score.css" />
</c:if>

<script type="text/javascript">
const roundText = "${roundText}";
const teamNumber = "${team.teamNumber}";
const runNumber = "${lRunNumber}";

var EditFlag = false;
<c:if test="${EditFlag}">
EditFlag = true;
</c:if>

function init() {
  <%ScoreEntry.generateInit(out, application, pageContext);%>

  refresh();
  
  if(document.scoreEntry.totalScore) {
      /* Saves total score for smarter notification popups */
    savedTotalScore = document.scoreEntry.totalScore.value;
  }
}

function refresh() { 
  var score = 0;

  <%ScoreEntry.generateRefreshBody(out, application, pageContext);%>

  //check for minimum total score
  if(score < ${minimumAllowedScore}) {
    score = ${minimumAllowedScore};
  }

  if(document.scoreEntry.totalScore) {
    document.scoreEntry.totalScore.value = score;
  }

  check_restrictions();
}

function check_restrictions() {
  var error_found = false;
  removeChildren(document.getElementById("score-errors"));
  
<%ScoreEntry.generateCheckRestrictionsBody(out, application, pageContext);%>

  const submitScoreButton = document.getElementById("submit_score")
  if(error_found) {
    if(submitScoreButton) {
      submitScoreButton.disabled = true;
    }
    document.getElementById("score-errors").classList.remove("fll-sw-ui-inactive");
  } else {
    if(submitScoreButton) {
      submitScoreButton.disabled = false;
    }
    document.getElementById("score-errors").classList.add("fll-sw-ui-inactive");
  }
}

<%ScoreEntry.generateIsConsistent(out, application);%>

<%ScoreEntry.generateIncrementMethods(out, application, request, session, pageContext);%>

document.addEventListener('DOMContentLoaded', function() {
  const resetButton = document.getElementById("reset_score");
  if(resetButton) {
      resetButton.addEventListener('click', init);
  }
});

</script>


<script type='text/javascript' src='scoreEntry.js'></script>
</head>

<body>

    <form action="SubmitScoreEntry" method="POST" name="scoreEntry"
        id="scoreEntry" class="fll-sw-ui-body">
        <c:choose>
            <c:when test="${not empty scoreEntrySelectedTable}">
                <input type='hidden' name='tablename'
                    value='${scoreEntrySelectedTable}' />
            </c:when>
            <c:otherwise>
                <input type='hidden' name='tablename' value='ALL' />
            </c:otherwise>
        </c:choose>

        <input type='hidden' id='NoShow' name='NoShow' value="false" />
        <input type='hidden' id='delete' name='delete' value="false" />

        <input type='hidden' name='EditFlag' id='EditFlag'
            value='${EditFlag}' readonly />

        <input type='hidden' name='RunNumber' value='${lRunNumber}'
            readonly>
        <input type='hidden' name='TeamNumber'
            value='${team.teamNumber}' readonly>

        <header>
            <div id="floating-messages">
                <div id="score-errors"></div>
                <div id="verification-warning">Are you sure this
                    score has been Verified? Normally scores are not
                    verified on the initial entry.</div>
            </div>

            <!-- top info bar (team name etc) -->
            <table id='top_info' border="1" cellpadding="0"
                cellspacing="0" width="100%">
                <tr align="center" valign="middle">
                    <td>

                        <table border="0" cellpadding="5"
                            cellspacing="0" width="90%">
                            <!--  inner box on title -->
                            <tr>
                                <td valign="middle" align="center">
                                    <c:choose>
                                        <c:when test="${EditFlag}">
                                            <font face="Arial" size="4">Score
                                                Edit</font>
                                        </c:when>
                                        <c:otherwise>
                                            <font face="Arial" size="4">Score
                                                Entry</font>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                            </tr>
                            <tr align="center">
                                <td>
                                    <font face="Arial" size="4"
                                        color='#0000ff'>

                                        <c:choose>
                                            <c:when
                                                test="${practiceEntryOnly}">
                      Practice
                      </c:when>
                                            <c:otherwise>
                      #${team.teamNumber}&nbsp;${team.organization}&nbsp;${team.teamName}&nbsp;--&nbsp;${roundText}
                      </c:otherwise>
                                        </c:choose>
                                    </font>
                                </td>
                            </tr>
                        </table>
                        <!--  end inner box on title -->

                    </td>
                </tr>
                <!-- team info -->

                <c:if test="${not previousVerified }">
                    <!--  warning -->
                    <tr>
                        <td bgcolor='red' align='center'>
                            <font face="Arial" size="4">Warning:
                                Previous run for this team has not been
                                verified!</font>
                </c:if>

            </table>
            <!--  end info bar -->
        </header>

        <main id='review-mode_container'>
            <div id='review-mode_glasspane'></div>

            <!-- score entry -->
            <table class='score-entry'>
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
                    <td colspan='2'>
                        <font size='4'>
                            <u>Goal</u>
                        </font>
                    </td>
                    <td align='right'>
                        <font size='4'>
                            <u>Count</u>
                        </font>
                    </td>
                    <td align='right' class='score-cell'>
                        <font size='4'>
                            <u>Score</u>
                        </font>
                    </td>
                </tr>

                <c:choose>
                    <c:when test="${isBye}">
                        <tr>
                            <td colspan='3'>
                                <b>Bye Run</b>
                            </td>
                        </tr>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${isNoShow}">
                            <tr>
                                <td colspan='5' class='center warning'>Editing
                                    a No Show - submitting score will
                                    change to a real run</td>
                            </tr>
                        </c:if>
                        <%
                        ScoreEntry.generateScoreEntry(out, application);
                        %>

                        <!-- Total Score -->
                        <tr class='total-score-container'>
                            <td colspan='3' class='total-score-label'>Total
                                Score:</td>
                            <td align='right' class='score-cell'>
                                <input type='text' name='totalScore'
                                    size='3' readonly tabindex='-1'>
                            </td>
                        </tr>
                    </c:otherwise>
                </c:choose>
                <!-- end check for bye -->

            </table>
            <!-- end score entry table  -->
        </main>

        <footer>
            <div class='score-entry buttonbox'>

                <button id='score-entry_toggle-review-mode'
                    type='button' class='fll-sw-button'>Review
                    Toggle</button>

                <%
                ScoreEntry.generateVerificationInput(out, request, session);
                %>
            </div>

            <div class='buttonbox score-entry-buttons'>


                <span id="challenge_revision">Challenge revision:
                    ${challengeDescription.revision}</span>
                <span id="software_version">
                    Software version:
                    <%=fll.Version.getVersion()%></span>

                <span class='float_right'>&nbsp;</span>
                <c:choose>
                    <c:when test="${practiceEntryOnly}">
                        <button type='button' id='reset_score'
                            class='fll-sw-button'>Reset Form</button>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not isBye}">
                            <button type='button' id='submit_score'
                                class='fll-sw-button'>Submit
                                Score</button>
                        </c:if>

                        <button type='button' id='cancel'
                            class='fll-sw-button'>Cancel</button>

                        <c:if test="${canDelete}">
                            <button type='button' id='submit_delete'
                                name='submit_delete'
                                class='fll-sw-button'>Delete
                                Score</button>
                        </c:if>


                        <c:if test="${not isBye}">
                            <button type='button' id='no_show'
                                name='no_show' value='No Show'
                                class='fll-sw-button'>No Show</button>
                        </c:if>

                    </c:otherwise>
                </c:choose>
            </div>
        </footer>

    </form>
    <!-- end score entry form -->


    <div class="fll-sw-ui-dialog fll-sw-ui-inactive" id="yesno-dialog">
        <div>
            <p id='yesno-dialog_text'></p>
            <button type='button' id='yesno-dialog_yes'>Yes</button>
            <button type='button' id='yesno-dialog_no'>No</button>
        </div>
    </div>

</body>
</html>

<%@page import="fll.web.admin.GatherParameterInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.TournamentParameters"%>

<%
  GatherParameterInformation
					.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Parameters</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />

<style>
.content table {
	border-collapse: collapse;
}

.content table, .content th, .content td {
	border: 1px solid black;
}

.content td, .content td {
	text-align: center;
}
</style>

<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>

<!-- functions to displaying and hiding help -->
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }

  function checkParams() {
    var value = $("#gStandardizedMean").val();
    if (!$.isNumeric(value)) {
      alert("Standardized Mean must be a decimal number");
      return false;
    }

    value = $("#gStandardizedSigma").val();
    if (!$.isNumeric(value)) {
      alert("Standardized Sigma must be a decimal number");
      return false;
    }

    return true;
  }

  $(document).ready(function() {
    $("#submit").click(function() {
      return checkParams();
    });
  });
</script>

</head>

<body>

  <div class='content'>

    <h1>Edit All Parameters</h1>

    ${message}
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />


    <p>This page is for advanced users only. Be careful changing
      parameters here.</p>

    <form
      name='edit_parameters'
      action='ChangeParameters'
      method='POST'>
      <h2>Tournament Parameters</h2>
      <p>These parameters are specified per tournament. Each of them
        has a default value that is used if no value is specified for
        the tournament</p>

      <table>

        <tr>
          <th>Parameter</th>
          <th>Default Value <a
            href='javascript:display("DefaultValueHelp")'>[help]</a>
            <div
              id='DefaultValueHelp'
              class='help'
              style='display: none'>
              If a value is not specified for a tournament, this value
              is used. <a href='javascript:hide("DefaultValueHelp")'>[hide]</a>
            </div>
          </th>

          <c:forEach
            items="${tournaments }"
            var="tournament">
            <th>${tournament.name }</th>
          </c:forEach>

        </tr>

        <tr>
          <th>Seeding Rounds <a
            href='javascript:display("SeedingRoundsHelp")'>[help]</a>
            <div
              id='SeedingRoundsHelp'
              class='help'
              style='display: none'>
              This parameter specifies the number of seeding rounds. The
              seeding rounds are used for the performance score in the
              final report and are used to rank teams for the initial
              playoff round. <a
                href='javascript:hide("SeedingRoundsHelp")'>[hide]</a>
            </div>
          </th>

          <td><select name='seeding_rounds_default'>
              <c:forEach
                begin="0"
                end="10"
                var="numRounds">
                <c:choose>
                  <c:when
                    test="${numRounds == numSeedingRounds_default}">
                    <option
                      selected
                      value='${numRounds}'>${numRounds}</option>
                  </c:when>
                  <c:otherwise>
                    <option value='${numRounds}'>${numRounds }</option>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
          </select></td>

          <c:forEach
            items="${tournaments }"
            var="tournament">

            <td><select
              name='seeding_rounds_${tournament.tournamentID }'>
                <c:choose>
                  <c:when
                    test="${empty numSeedingRounds[tournament.tournamentID]}">
                    <option
                      selected
                      value="default">Default</option>
                  </c:when>
                  <c:otherwise>
                    <option value="default">Default</option>
                  </c:otherwise>
                </c:choose>

                <c:forEach
                  begin="0"
                  end="10"
                  var="numRounds">
                  <c:choose>
                    <c:when
                      test="${numRounds == numSeedingRounds[tournament.tournamentID]}">
                      <option
                        selected
                        value='${numRounds}'>${numRounds}</option>
                    </c:when>
                    <c:otherwise>
                      <option value='${numRounds}'>${numRounds }</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
            </select></td>

          </c:forEach>

        </tr>


        <tr>
          <th>Max Scoreboard Round <a
            href='javascript:display("MaxScoreboardRoundHelp")'>[help]</a>
            <div
              id='MaxScoreboardRoundHelp'
              class='help'
              style='display: none'>
              Performance rounds greater than this number will not be
              displayed on the scoreboard. This exists to prevent the
              winners of the head to head from being displayed before
              the awards ceremony. Generally this should be the same as
              the number of seeding rounds. Ideally this would be the
              number of rounds include <a
                href='javascript:hide("MaxScoreboardRoundHelp")'>[hide]</a>
            </div>

          </th>

          <td><select name='max_scoreboard_round_default'>
              <c:forEach
                begin="0"
                end="10"
                var="numRounds">
                <c:choose>
                  <c:when
                    test="${numRounds == maxScoreboardRound_default}">
                    <option
                      selected
                      value='${numRounds}'>${numRounds}</option>
                  </c:when>
                  <c:otherwise>
                    <option value='${numRounds}'>${numRounds }</option>
                  </c:otherwise>
                </c:choose>
              </c:forEach>
          </select></td>

          <c:forEach
            items="${tournaments }"
            var="tournament">

            <td><select
              name='max_scoreboard_round_${tournament.tournamentID }'>
                <c:choose>
                  <c:when
                    test="${empty maxScoreboardRound[tournament.tournamentID]}">
                    <option
                      selected
                      value="default">Default</option>
                  </c:when>
                  <c:otherwise>
                    <option value="default">Default</option>
                  </c:otherwise>
                </c:choose>

                <c:forEach
                  begin="0"
                  end="10"
                  var="numRounds">
                  <c:choose>
                    <c:when
                      test="${numRounds == maxScoreboardRound[tournament.tournamentID]}">
                      <option
                        selected
                        value='${numRounds}'>${numRounds}</option>
                    </c:when>
                    <c:otherwise>
                      <option value='${numRounds}'>${numRounds }</option>
                    </c:otherwise>
                  </c:choose>
                </c:forEach>
            </select></td>

          </c:forEach>

        </tr>


      </table>


      <h2>Global Parameters</h2>
      <p>These parameters are specified globally and apply to all
        tournaments in the database.</p>

      <table>

        <tr>
          <th>Parameter</th>
          <th>Value</th>
        </tr>

        <tr>
          <th>Standardized Mean <a
            href='javascript:display("StandardizedMeanHelp")'>[help]</a>
            <div
              id='StandardizedMeanHelp'
              class='help'
              style='display: none'>
              The mean that we scale the raw mean of all scores in a
              category for a judge to. <a
                href='javascript:hide("StandardizedMeanHelp")'>[hide]</a>
            </div>

          </th>
          <td><input
            type='text'
            value="${gStandardizedMean }"
            id='gStandardizedMean'
            name='gStandardizedMean' /></td>
        </tr>

        <tr>
          <th>Standardized Sigma <a
            href='javascript:display("StandardizedSigmaHelp")'>[help]</a>
            <div
              id='StandardizedSigmaHelp'
              class='help'
              style='display: none'>
              The sigma to use when scaling scores for comparison. <a
                href='javascript:hide("StandardizedSigmaHelp")'>[hide]</a>
            </div>

          </th>
          <td><input
            type='text'
            value="${gStandardizedSigma }"
            id='gStandardizedSigma'
            name='gStandardizedSigma' /></td>
        </tr>

        <tr>
          <th>Division Flip Rate <a
            href='javascript:display("DivisionFlipRateHelp")'>[help]</a>
            <div
              id='DivisionFlipRateHelp'
              class='help'
              style='display: none'>
              The number of seconds between when the scoreboard's "Top
              Division Scores" panel switches which division is shown.
              Default 30 seconds. <a
                href='javascript:hide("DivisionFlipRateHelp")'>[hide]</a>
            </div>
          </th>
          <td><input
            type='text'
            value="${gDivisionFlipRate}"
            id='gDivisionFlipRate'
            name='gDivisionFlipRate' /></td>
        </tr>

        <tr>
          <th>Ranking report displays quartiles</th>
          <td><c:choose>
              <c:when test="${gUseQuartiles }">
                <input
                  type='radio'
                  name='gUseQuartiles'
                  id='gUseQuartiles_yes'
                  value='true'
                  checked />
                <label for='gUseQuartiles_yes'>Yes</label>

                <input
                  type='radio'
                  name='gUseQuartiles'
                  id='gUseQuartiles_no'
                  value='false' />
                <label for='gUseQuartiles_no'>No</label>
              </c:when>
              <c:otherwise>
                <input
                  type='radio'
                  name='gUseQuartiles'
                  id='gUseQuartiles_yes'
                  value='true' />
                <label for='gUseQuartiles_yes'>Yes</label>

                <input
                  type='radio'
                  name='gUseQuartiles'
                  id='gUseQuartiles_no'
                  value='false'
                  checked />
                <label for='gUseQuartiles_no'>No</label>
              </c:otherwise>
            </c:choose></td>
        </tr>


      </table>


      <input
        type='submit'
        value='Save Changes'
        id='submit' />
    </form>

  </div>

</body>
</html>

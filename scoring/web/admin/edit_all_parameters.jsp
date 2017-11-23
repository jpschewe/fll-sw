<%@page import="fll.web.admin.GatherParameterInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.db.TournamentParameters"%>

<%
	GatherParameterInformation.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Parameters</title>
<link rel="stylesheet" type="text/css"
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

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>

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

		<div class='status-message'>${message}</div>
		<%-- clear out the message, so that we don't see it again --%>
		<c:remove var="message" />


		<p>This page is for advanced users only. Be careful changing
			parameters here.</p>

		<form name='edit_parameters' action='ChangeParameters' method='POST'>
			<h2>Tournament Parameters</h2>
			<p>These parameters are specified per tournament. Each of them
				has a default value that is used if no value is specified for the
				tournament</p>

			<table>

				<tr>
					<th>Parameter</th>
					<th>Default Value <a
						href='javascript:display("DefaultValueHelp")'>[help]</a>
						<div id='DefaultValueHelp' class='help' style='display: none'>
							If a value is not specified for a tournament, this value is used.
							<a href='javascript:hide("DefaultValueHelp")'>[hide]</a>
						</div>
					</th>

					<c:forEach items="${tournaments }" var="tournament">
						<th>${tournament.name }</th>
					</c:forEach>

				</tr>

				<tr>
					<th>Seeding Rounds <a
						href='javascript:display("SeedingRoundsHelp")'>[help]</a>
						<div id='SeedingRoundsHelp' class='help' style='display: none'>
							This parameter specifies the number of seeding rounds. The
							seeding rounds are used for the performance score in the final
							report and are used to rank teams for the initial head to head
							round. <a href='javascript:hide("SeedingRoundsHelp")'>[hide]</a>
						</div>
					</th>

					<td><select name='seeding_rounds_default'>
							<c:forEach begin="0" end="10" var="numRounds">
								<c:choose>
									<c:when test="${numRounds == numSeedingRounds_default}">
										<option selected value='${numRounds}'>${numRounds}</option>
									</c:when>
									<c:otherwise>
										<option value='${numRounds}'>${numRounds }</option>
									</c:otherwise>
								</c:choose>
							</c:forEach>
					</select></td>

					<c:forEach items="${tournaments }" var="tournament">

						<td><select name='seeding_rounds_${tournament.tournamentID }'>
								<c:choose>
									<c:when
										test="${empty numSeedingRounds[tournament.tournamentID]}">
										<option selected value="default">Default</option>
									</c:when>
									<c:otherwise>
										<option value="default">Default</option>
									</c:otherwise>
								</c:choose>

								<c:forEach begin="0" end="10" var="numRounds">
									<c:choose>
										<c:when
											test="${numRounds == numSeedingRounds[tournament.tournamentID]}">
											<option selected value='${numRounds}'>${numRounds}</option>
										</c:when>
										<c:otherwise>
											<option value='${numRounds}'>${numRounds }</option>
										</c:otherwise>
									</c:choose>
								</c:forEach>
						</select></td>

					</c:forEach>

				</tr>
				<!--  seeding rounds -->


				<tr>
					<th>Max Scoreboard Round <a
						href='javascript:display("MaxScoreboardRoundHelp")'>[help]</a>
						<div id='MaxScoreboardRoundHelp' class='help'
							style='display: none'>
							Performance rounds greater than this number will not be displayed
							on the scoreboard. This exists to prevent the winners of the head
							to head from being displayed before the awards ceremony.
							Generally this should be the same as the number of seeding
							rounds. Ideally this would be the number of rounds include <a
								href='javascript:hide("MaxScoreboardRoundHelp")'>[hide]</a>
						</div>

					</th>

					<td><select name='max_scoreboard_round_default'>
							<c:forEach begin="0" end="10" var="numRounds">
								<c:choose>
									<c:when test="${numRounds == maxScoreboardRound_default}">
										<option selected value='${numRounds}'>${numRounds}</option>
									</c:when>
									<c:otherwise>
										<option value='${numRounds}'>${numRounds }</option>
									</c:otherwise>
								</c:choose>
							</c:forEach>
					</select></td>

					<c:forEach items="${tournaments }" var="tournament">

						<td><select
							name='max_scoreboard_round_${tournament.tournamentID }'>
								<c:choose>
									<c:when
										test="${empty maxScoreboardRound[tournament.tournamentID]}">
										<option selected value="default">Default</option>
									</c:when>
									<c:otherwise>
										<option value="default">Default</option>
									</c:otherwise>
								</c:choose>

								<c:forEach begin="0" end="10" var="numRounds">
									<c:choose>
										<c:when
											test="${numRounds == maxScoreboardRound[tournament.tournamentID]}">
											<option selected value='${numRounds}'>${numRounds}</option>
										</c:when>
										<c:otherwise>
											<option value='${numRounds}'>${numRounds }</option>
										</c:otherwise>
									</c:choose>
								</c:forEach>
						</select></td>

					</c:forEach>

				</tr>
				<!--  max scoreboard round -->


				<tr>
					<th>Performance Advancement Percentage <a
						href='javascript:display("PerformanceAdvancementPercentageHelp")'>[help]</a>
						<div id='PerformanceAdvancementPercentageHelp' class='help'
							style='display: none'>
							Teams should have a performance score in the top X% of all
							performnace scores to be allowed to advance to the next
							tournament. <a
								href='javascript:hide("PerformanceAdvancementPercentageHelp")'>[hide]</a>
						</div>

					</th>

					<td><input type='text'
						id='performance_advancement_percentage_default'
						name='performance_advancement_percentage_default'
						value='${performanceAdvancementPercentage_default}' /></td>

					<c:forEach items="${tournaments }" var="tournament">

						<td><input type='text'
							id='performance_advancement_percentage_${tournament.tournamentID }'
							name='performance_advancement_percentage_${tournament.tournamentID }'
							value='${performanceAdvancementPercentage[tournament.tournamentID]}' />

						</td>

					</c:forEach>

				</tr>
				<!-- performance advancement percentage -->

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
						<div id='StandardizedMeanHelp' class='help' style='display: none'>
							The mean that we scale the raw mean of all scores in a category
							for a judge to. <a href='javascript:hide("StandardizedMeanHelp")'>[hide]</a>
						</div>

					</th>
					<td><input type='text' value="${gStandardizedMean }"
						id='gStandardizedMean' name='gStandardizedMean' /></td>
				</tr>

				<tr>
					<th>Standardized Sigma <a
						href='javascript:display("StandardizedSigmaHelp")'>[help]</a>
						<div id='StandardizedSigmaHelp' class='help' style='display: none'>
							The sigma to use when scaling scores for comparison. <a
								href='javascript:hide("StandardizedSigmaHelp")'>[hide]</a>
						</div>

					</th>
					<td><input type='text' value="${gStandardizedSigma }"
						id='gStandardizedSigma' name='gStandardizedSigma' /></td>
				</tr>

				<tr>
					<th>Award Group Flip Rate (seconds) <a
						href='javascript:display("DivisionFlipRateHelp")'>[help]</a>
						<div id='DivisionFlipRateHelp' class='help' style='display: none'>
							The number of seconds between when the scoreboard's "Top Scores"
							panel switches which division is shown. Default 30 seconds. <a
								href='javascript:hide("DivisionFlipRateHelp")'>[hide]</a>
						</div>
					</th>
					<td><input type='text' value="${gDivisionFlipRate}"
						id='gDivisionFlipRate' name='gDivisionFlipRate' /></td>
				</tr>

				<tr>
					<th>All teams scroll rate control. <a
						href='javascript:display("gAllTeamsMsPerRowHelp")'>[help]</a>
						<div id='gAllTeamsMsPerRowHelp' class='help' style='display: none'>The
							value is nominally the number of milliseconds to display each row
							of the page.</div>
					</th>

					<td><input type='text' value="${gAllTeamsMsPerRow }"
						id='gAllTeamsMsPerRow' name='gAllTeamsMsPerRow' /></td>
				</tr>

				<tr>
					<th>Head to head scroll rate control. <a
						href='javascript:display("gHeadToHeadMsPerRowHelp")'>[help]</a>
						<div id='gHeadToHeadMsPerRowHelp' class='help'
							style='display: none'>The value is nominally the number of
							milliseconds to display each row of the page. The remote control
							brackets page needs to be refreshed for this parameter to take
							effect.</div>
					</th>

					<td><input type='text' value="${gHeadToHeadMsPerRow }"
						id='gHeadToHeadMsPerRow' name='gHeadToHeadMsPerRow' /></td>
				</tr>

				<tr>
					<th>Should the ranking report display quartiles?</th>
					<td><c:choose>
							<c:when test="${gUseQuartiles }">
								<input type='radio' name='gUseQuartiles' id='gUseQuartiles_yes'
									value='true' checked />
								<label for='gUseQuartiles_yes'>Yes</label>

								<input type='radio' name='gUseQuartiles' id='gUseQuartiles_no'
									value='false' />
								<label for='gUseQuartiles_no'>No</label>
							</c:when>
							<c:otherwise>
								<input type='radio' name='gUseQuartiles' id='gUseQuartiles_yes'
									value='true' />
								<label for='gUseQuartiles_yes'>Yes</label>

								<input type='radio' name='gUseQuartiles' id='gUseQuartiles_no'
									value='false' checked />
								<label for='gUseQuartiles_no'>No</label>
							</c:otherwise>
						</c:choose></td>
				</tr>

				<tr>
					<th colspan="2">FLL Tools integration</th>
				</tr>

				<tr>
					<th>Mhub hostname<a
						href='javascript:display("MhubHostnameHelp")'>[help]</a>
						<div id='MhubHostnameHelp' class='help' style='display: none'>
							The hostname where mhub is running. Clear to specify that mhub is
							not in use. <a href='javascript:hide("MhubHostnameHelp")'>[hide]</a>
						</div>

					</th>
					<td><input type='text' value="${gMhubHostname }"
						id='gMhubHostname' name='gMhubHostname' /></td>
				</tr>

				<tr>
					<th>Mhub port<a href='javascript:display("MhubPortHelp")'>[help]</a>
						<div id='MhubPortHelp' class='help' style='display: none'>
							The port where mhub is running.<a
								href='javascript:hide("MhubPortHelp")'>[hide]</a>
						</div>

					</th>
					<td><input type='text' value="${gMhubPort }" id='gMhubPort'
						name='gMhubPort' /></td>
				</tr>

				<tr>
					<th>Display node<a
						href='javascript:display("MhubDisplayNodeHelp")'>[help]</a>
						<div id='MhubDisplayNodeHelp' class='help' style='display: none'>
							The node to send display messages to. <a
								href='javascript:hide("MhubDisplayNodeHelp")'>[hide]</a>
						</div>

					</th>
					<td><input type='text' value="${gMhubDisplayNode }"
						id='gMhubDisplayNode' name='gMhubDisplayNode' /></td>
				</tr>


			</table>


			<input type='submit' value='Save Changes' id='submit' />
		</form>

	</div>

</body>
</html>

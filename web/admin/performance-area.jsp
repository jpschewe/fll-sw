<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Performance Area</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />


<style>
.completed {
	background-color: #00FF00;
	font-weight: bold;
}
</style>

<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }

  function confirm_subjective() {
    var overwrite = document.getElementById('subjective_overwrite_all').checked;
    if (overwrite) {
      return confirm("Are you sure you want to overwrite all subjective scores?")
    } else {
      return true;
    }
  }
</script>
</head>

<body>
	<h1>${challengeDescription.title }(PerformanceArea)</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<p>This page contains links to the pages used in the performance
		area when a 2 server setup is used. This page assumes that you have
		been given a pre-populated database to load. Most links open up new
		tabs so that this page can be used to follow the workflow.</p>

	<p>
		The current tournament is <b>${tournamentTitle }</b>
	</p>

	<ol>

		<li><a target="_init" href="<c:url value='/setup' />">Database
				setup</a> - Initialize the database first thing in the morning or the
			night before.</li>

		<li>Current tournament is ${currentTournament.description }.
			<form id='currentTournament'
				action="<c:url value='/admin/SetCurrentTournament' />" method="post">
				Change tournament to <select id='currentTournamentSelect'
					name='currentTournament'>
					<c:forEach items="${tournaments }" var="tournament">
						<c:choose>
							<c:when
								test="${tournament.tournamentID == currentTournament.tournamentID}">
								<option selected value='${tournament.tournamentID }'>${tournament.description}
									[ ${tournament.name } ]</option>
							</c:when>
							<c:otherwise>
								<option value='${tournament.tournamentID }'>${tournament.description}
									[ ${tournament.name } ]</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select> <input type='submit' name='change_tournament' value='Submit'>
			</form>
		</li>



		<li><a target="_remote_control" id='remote-control'
			href='remoteControl.jsp'>Remote control of display</a> - used to
			control what is displayed on the screens</li>

		<li><a href="CheckSubjectiveEmpty">Export performance data
				for judges server</a>. This is done once the seeding rounds have been
			complete. If doing finalist scheduling, the initial head to head
			brackets need to be created before doing this export. This file needs
			to goto the judges room.</li>

		<li>Once the seeding rounds have been completed you will need to
			setup the <a href="<c:url value='/playoff' />" target="_h2h">head
				to head brackets</a>.
		</li>


		<li>
			<form name="import-finalist" action="ProcessImportFinalist"
				method="POST" ENCTYPE="multipart/form-data">
				<i>If this tournament does not have finalist judging, skip this
					step.</i> Specify the file that was exported from the judges server
				with the finalist data <input type="file" size="32"
					name="finalistFile" />

				<!-- performance file upload button -->
				<input id='uploadFinalistData' type="submit" value="Upload" />
			</form>
		</li>

		<li><a href='<c:url value="/report/PlayoffReport" />'
			target="_blank">Winners of each head to head bracket</a>. This is
			needed for the awards ceremony.</li>

		<li><a href='database.flldb'>Download the final database</a> -
			send this to the lead computer person</li>


	</ol>

	<h2>Other useful pages</h2>
	<ul>
		<li>
			<form
				action="<c:url value='ChangeScorePageText' />" method='post'>
				Scoring display text: <input type='text' name='ScorePageText'
					value='<c:out value="${ScorePageText}"/>'> <input
					type='submit' value='Change text'> <a
					href='javascript:display("ScorePageTextHelp")'>[help]</a>
				<div id='ScorePageTextHelp' class='help' style='display: none'>
					This text is displayed on the various big screen display pages.
					There is only 1 or 2 lines of space available, so keep it short.
					This can be used to notify participants and spectators of when the
					next break will be over.<a
						href='javascript:hide("ScorePageTextHelp")'>[hide]</a>
				</div>
			</form>
		</li>

		<li><a target="_edit_teams"
			href="<c:url value='/admin/select_team.jsp' />">Edit team data</a> -
			this is where to go for team name and organization changes</li>

		<li><a href="<c:url value='/admin/ScheduleByTeam' />"
			target="_new">Full schedule sorted by team</a></li>

		<li><a
			href="<c:url value='/admin/SubjectiveScheduleByJudgingStation' />"
			target="_new">Subjective schedule sorted by judging group, then
				time</a></li>

		<li><a href="<c:url value='/admin/SubjectiveScheduleByTime' />"
			target="_new">Subjective schedule sorted by time</a></li>

		<li><a href="<c:url value='/admin/PerformanceSchedule' />"
			target="_new">Performance Schedule</a></li>
	</ul>

</body>
</html>

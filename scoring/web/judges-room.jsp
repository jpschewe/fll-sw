<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.MainIndex.populateContext(request, application, pageContext);
	fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Judges room</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>

</head>

<body>
	<h1>Judges room for ${challengeDescription.title }</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<p>
		The current tournament is <b>${tournamentTitle }</b>
	</p>

	<ol>
		<li><a target="_init" href="<c:url value='/setup' />">Database
				setup</a> - Initialize the database first thing in the morning or the
			night before. This has probably already been done.</li>

		<li>Make sure that the correct tournament is selected.
			<ul>
				<li>The current tournament is ${currentTournament.description }.</li>
				<li>
					<form id='currentTournament'
						action="c:url value='/admin/SetCurrentTournament' />"
						method="post">
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

			</ul>
		</li>

		<li>Enter subjective scores. This can be done through the web
			application for judges using electronic devices. For those not using
			electronic devices the scores will be entered through the Subjective
			Java application from the launcher.
			<ul>
				<li>Electronic devices use these links
					<ul>
						<li><a target="_subjective"
							href="<c:url value='/subjective/Auth' />">Standard web
								application</a>. If you get an error that the application cache is
							not supported or that the SSL version must be used. Then use the
							SSL version below.</li>

						<li>SSL web application
							<ol>
								<li>Follow <a
									href="<c:url value='/documentation/installing-ssl-certificates.html' />"
									target='_installing_ssl'>these instructions</a> to get the
									certificate trusted
								</li>

								<li><a href="<c:url value='/fll-sw.crt' />">Security
										certificate</a> for SSL subjective web application access. This is
									referenced in the instructions.</li>

								<li><a target="_subjective"
									href="${baseSslUrl}/subjective/Auth">SSL Subjective Web
										Application</a></li>

							</ol>
						</li>
					</ul>
				</li>
				<li>Java application use these links
					<ul>

						<li><a href="<c:url value='/admin/subjective-data.fll' />">Download
								the data file for subjective score entry.</a> Download this file
							after each subjective score upload to lessen chance of data loss
							due to overwrite.</li>

						<li>
							<form name='uploadSubjective' target="_upload_subjective"
								ACTION='<c:url value="/admin/UploadSubjectiveData"/> '
								METHOD="POST" ENCTYPE="multipart/form-data">
								Upload the data file for subjective scores.

								<!-- file to upload -->
								<input type="file" size="32" name="subjectiveFile" />

								<!-- subjective file upload button -->
								<input id='uploadSubjectiveFile' type="submit" value="Upload" />

							</form>
						</li>

					</ul>
				</li>
			</ul>
		</li>



		<li>
			<form name="import-performance"
				action="<c:url value='/admin/ProcessImportPerformance' />"
				target="_import_performance" method="POST"
				ENCTYPE="multipart/form-data">
				Specify the file that was exported from the performance server <input
					type="file" size="32" name="performanceFile" />

				<!-- performance file upload button -->
				<input id='uploadPerformanceData' type="submit" value="Upload" />
			</form>
		</li>

		<li>At the end of the day <a
			href="<c:url value='/admin/DownloadJudgesDatabase' />">download
				the final judges room database</a> and send it to the head computer
			person
		</li>

	</ol>


	<h2>Server addresses</h2>
	<p>These are the addresses that can be used on the judges
		electronic devices to connect to this server.</p>
	<ul>
		<c:forEach items="${urls}" var="url">
			<li><a href="${url }">${url }</a></li>
		</c:forEach>
	</ul>



</body>
</html>
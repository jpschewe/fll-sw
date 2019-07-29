<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />
<title>Reporting</title>
</head>

<body>
	<h1>Reporting</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<h2>All Tournaments</h2>
	<ol>
		<li><a href="summarizePhase1.jsp">Compute summarized scores</a>.
			This needs to be executed before any reports can be generated. You
			will be returned to this page if there are no errors summarizing
			scores.</li>

		<li><a href="topScoreReport.jsp">Top performance scores</a>. This
			creates a text report of the top seeding round scores by award group.</li>

		<li><a href='FinalComputedScores' target="_blank">Final
				Computed Scores</a>. This is the report that the head judge will want to
			determine which teams advance to the next tournament.</li>

		<li><a href="CategoryScoresByScoreGroup" target="_blank">Categorized
				Scores by Judging Group</a>. This displays the scaled scores for each
			category by judging group. This is useful for checking the winners of
			each category.</li>

		<li><a href="PerformanceScoreReport" target="_blank">Performance
				Score Report</a>. This displays the details of the performance runs for
			each team.</li>

		<li><a href="PlayoffReport" target="_blank">Winners of each
				head to head bracket</a>. This is useful for the awards ceremony.</li>

	</ol>


	<h2>Finalist scheduling</h2>
	<p>This is used at tournaments where there is more than 1 judging
		group in an award group. This is typically the case at a state
		tournament where all teams are competing for first place in each
		category, but there are too many teams for one judge to see.</p>

	<ul>

		<li><a href="non-numeric-nominees.jsp" target="_blank">Enter
				non-numeric nominees</a>. This is used to enter the teams that are up
			for consideration for the non-scored subjective categories. This
			information transfers over to the finalist scheduling web
			application. This is also used in the awards scripts report.</li>

		<li><a href="finalist/load.jsp" target="_blank">Schedule
				Finalists</a>. Before visiting this page, all subjective scores need to
			be uploaded and any head to head brackets that will occur during the
			finalist judging should be created to avoid scheduling conflicts.</li>

		<li>
			<form ACTION='finalist/PrivateFinalistSchedule' METHOD='POST'
				target="_blank">
				<select name='division'>
					<c:forEach var="division" items="${finalistDivisions }">
						<option value='${division }'>${division }</option>
					</c:forEach>
				</select> <input type='submit' value='Private Finalist Schedule (PDF)' />
				This displays the finalist schedule for all categories.
			</form>
		</li>

		<li>
			<form ACTION='finalist/PublicFinalistSchedule' METHOD='POST'
				target="_blank">
				<select name='division'>
					<c:forEach var="division" items="${finalistDivisions }">
						<option value='${division }'>${division }</option>
					</c:forEach>
				</select> <input type='submit' value='Public Finalist Schedule (PDF)' />
				This displays the finalist schedule for public categories.
			</form>
		</li>

		<li>
			<form ACTION='finalist/PublicFinalistDisplaySchedule.jsp'
				METHOD='POST' target="_blank">
				<select name='division'>
					<c:forEach var="division" items="${finalistDivisions }">
						<option value='${division }'>${division }</option>
					</c:forEach>
				</select> <input type='submit' value='Public Finalist Schedule (HTML)' />
				This displays the finalist schedule for public categories.
			</form>
		</li>

		<li><a href="finalist/TeamFinalistSchedule" target="_blank">Finalist
				Schedule for each team</a></li>


	</ul>

	<h2>Other useful reports</h2>
	<p>Some reports that are handy for intermediate reporting and
		checking of the current tournament state.</p>

	<ul>
		<li>
			<form ACTION='performanceRunReport.jsp' METHOD='POST'>
				Show scores for performance run <select name='RunNumber'>
					<c:forEach var="index" begin="1" end="${maxRunNumber}">
						<option value='${index }'>${index }</option>
					</c:forEach>
				</select> <input type='submit' value='Show Scores' />
			</form>
		</li>

		<li>
			<form action='teamPerformanceReport.jsp' method='post'>
				Show performance scores for team <select name='TeamNumber'>
					<c:forEach items="${tournamentTeams}" var="team">
						<option value='<c:out value="${team.teamNumber}"/>'>
							<c:out value="${team.teamNumber}" /> -
							<c:out value="${team.teamName}" />
						</option>
					</c:forEach>
				</select> <input type='submit' value='Show Scores' />
			</form>
		</li>

		<li><a href="unverifiedRuns.jsp">Unverified runs</a>. Unverfied
			performance runs.</li>

		<li><a href="PerformanceScoreDump">CSV file containing all
				performance scores</a>, excluding byes. This is useful to manually
			determine awards for most consistent or most improved robot
			performance.</li>

	</ul>


</body>
</html>

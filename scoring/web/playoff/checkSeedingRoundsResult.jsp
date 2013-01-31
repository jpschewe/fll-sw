<%@ include file="/WEB-INF/jspf/init.jspf"%>


<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
<title>Team Playoff check</title>
</head>

<body>
	<h1>
		Check Seeding Rounds
	</h1>

	${message}
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />


	<h2>Team Playoff check [Division: ${playoff_data.division } ]</h2>

	<p>Teams with fewer runs than seeding rounds. Teams with no runs
		are excluded from this check.</p>
	<ul>
		<c:forEach items="${playoff_data.teamsNeedingSeedingRuns }"
			var="team">
			<li class='warning'>${team.teamName } ( ${team.teamNumber } )</li>
		</c:forEach>
	</ul>
 
	<p>
		<a href="index.jsp">Back to Playoff menu</a>
	</p>


</body>
</html>
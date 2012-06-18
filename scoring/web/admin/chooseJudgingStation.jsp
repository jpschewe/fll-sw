<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Edit Event Division</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
</head>

<body>

	<form method="POST" action="CommitJudgingStation">
		<p>Team ${teamNumber} is assigned to a tournament that is using
			judging stations. You need to specify which judging station this team
			is to be in.</p>

		<select name='judging_station'>
			<c:forEach items="${all_judging_stations }" var="val">

				<option value='${val }'>${val }</option>

			</c:forEach>
		</select> <br /> <input type='submit' name='submit' value='Commit' />

	</form>

</body>
</html>
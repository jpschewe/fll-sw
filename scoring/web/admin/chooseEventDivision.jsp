<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Edit Event Division</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
</head>

<body>

	<form method="POST" action="CommitEventDivision">
		<p>Team ${teamNumber} is assigned to a tournament that is using
			event divisions. You need to specify which <b>event division</b> this team
			is to be in.</p>

		<select name='event_division'>
			<c:forEach items="${all_event_divisions }" var="ediv">

				<option value='${ediv }'>${ediv }</option>

			</c:forEach>
		</select> <br /> <input type='submit' name='submit' value='Commit' />

	</form>

</body>
</html>
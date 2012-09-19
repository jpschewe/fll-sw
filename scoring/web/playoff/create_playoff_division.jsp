<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
<title>Playoff's</title>
</head>

<body>
	<h1>
		<x:out select="$challengeDocument/fll/@title" />
		(Create Playoff Division)
	</h1>

	${message}
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<p>Choose the teams that you want to include in the custom event
		division. This group of teams will compete against each other in a
		single elimination playoff bracket.</p>

	<form method="POST" action="CreatePlayoffDivision">

		<input name="division_name" />

		<table border='1'>

			<tr>
				<th>Select</th>
				<th>Number</th>
				<th>Name</th>
			</tr>
			<c:forEach items="${teams }" var="team">
				<tr>

					<td><input name="selected_team" type="checkbox" value="${team.teamNumber }" /></td>

					<td>${team.teamNumber }</td>

					<td>${team.teamName }</td>

				</tr>
			</c:forEach>
		</table>

		<input type='submit' value='Submit'/>

	</form>

</body>
</html>

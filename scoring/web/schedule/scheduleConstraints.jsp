<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
	<h1>Specify schedule constraints</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<form name="constraints" method='POST'
		action='ProcessScheduleConstraints'>
		<p>Specify the constraints to use for checking the uploaded
			schedule. If you haven't been given any specific values to use, just
			use the defaults.</p>

		<!--  TODO issue:129 need to validate that this is a number -->
		<div>
			Change time duration: <input name="changeTimeDuration"
				value="${uploadScheduleData.schedParams.changetimeMinutes }">
			minutes
		</div>

		<!--  TODO issue:129 need to validate that this is a number -->
		<div>
			Performance change time duration: <input
				name="performanceChangeTimeDuration"
				value="${uploadScheduleData.schedParams.performanceChangetimeMinutes }">
			minutes
		</div>

		<!--  TODO issue:129 need to validate that this is a number -->
		<div>
			Performance duration: <input name="performanceDuration"
				value="${uploadScheduleData.schedParams.performanceMinutes }">
			minutes
		</div>


		<input type="submit" id='submit' />
	</form>

</body>
</html>
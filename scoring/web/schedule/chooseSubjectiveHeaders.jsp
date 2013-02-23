<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />

</head>

<body>
	<h1>
Choose Subjective Headers	</h1>

	${message}
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<form name="choose_headers" method='POST'
		action='ProcessSubjectiveHeaders'>
		<p>Choose the columns that represent subjective judging stations
			in your schedule. You can choose none, meaning you have no subjective
			judging stations.</p>
		<table>
			<tr>
				<th>Category Name</th>
				<th>Subjective Category?</th>
				<th>Duration in Minutes</th>
			</tr>
			<c:forEach items="${uploadSchedule_unusedHeaders }" var="subjHeader"
				varStatus="loopStatus">
				<c:if test="${fn:length(subjHeader) > 0 }">
					<tr>
						<th>${subjHeader}</th>
						<td><input type="checkbox" name="subjectiveHeader"
							value="${loopStatus.index }" id="header_${loopStatus.index}" />
						</td>
						<!--  TODO ticket:94 need to validate that this is a number -->
						<td><input type="text" name="duration_${loopStatus.index }"
							value="${default_duration }" />
						</td>
					</tr>
				</c:if>
			</c:forEach>
		</table>

		<input type="submit" />
	</form>

</body>
</html>
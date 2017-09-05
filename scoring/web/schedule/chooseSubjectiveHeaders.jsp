<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.schedule.ChooseSubjectiveHeaders.populateContext(pageContext);
%>

<html>
<head>
<title>FLL-SW</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
	<h1>Choose Subjective Headers</h1>

	<div class='status-message'>${message}</div>
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<form name="choose_headers" method='POST'
		action='ProcessSubjectiveHeaders'>
		<p>Match the column names from the schedule data file with the
			subjective categories that they contain the schedule for. Also
			specify the number of minutes between judging sessions for each
			category.</p>

		<table border='1'>
			<tr>
				<th>Subjective Category</th>
				<th>Data file column name</th>
				<th>Duration (minutes)</th>
			</tr>
			<c:forEach items="${challengeDescription.subjectiveCategories }"
				var="subcat">
				<tr>

					<td>${subcat.title}</td>

					<td><select name='${subcat.name}:header'>

							<c:forEach items="${uploadScheduleData.unusedHeaders }"
								var="subjHeader" varStatus="loopStatus">
								<c:if test="${fn:length(subjHeader) > 0 }">
									<option value='${loopStatus.index }'>${subjHeader }</option>
								</c:if>
							</c:forEach>
							<!-- foreach data file header -->

					</select></td>

					<!--  TODO issue:129 need to validate that this is a number -->
					<td><input type="text" name="${subcat.name}:duration"
						value="${default_duration}" /></td>

				</tr>
				<!-- row for category -->

			</c:forEach>
			<!--  foreach category -->

		</table>

		<input type="submit" id='submit' />
	</form>

</body>
</html>
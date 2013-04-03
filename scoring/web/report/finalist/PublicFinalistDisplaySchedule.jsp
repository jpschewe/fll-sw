<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	fll.web.report.finalist.PublicFinalistDisplaySchedule
			.populateContext(request, application, session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/base.css'/>" />
<title>Reporting</title>

<script type='text/javascript'
	src="<c:url value='/extlib/jquery-1.7.1.min.js'/>"></script>

<style>
BODY {
	color: #ffffff;
	font-family: "Arial";
	background-color: #000080;
}
</style>

<script type='text/javascript' src="<c:url value='/scripts/scroll.js'/>"></script>

<script type="text/javascript">
	$(document).ready(function() {
		<c:if test="${finalistScheduleScroll}">
		startScrolling();
		</c:if>
	});
</script>

</head>

<body>
	${message}
	<%-- clear out the message, so that we don't see it again --%>
	<c:remove var="message" />

	<h1>Finalist Schedule for Division: ${division }</h1>

	<c:forEach items="${publicCategories }" var="category">
		<h2>Category: ${category }</h2>
		<c:forEach items="${publicSchedules[category] }" var="schedRow">
			<div>
				<c:set var="team" value="${allTeams[schedRow.teamNumber] }" />

				<span><fmt:formatDate value="${schedRow.time }" type="time"
						pattern="hh:mm" /></span> - ${team.teamNumber } - ${team.teamName } -
				${team.organization }
			</div>
		</c:forEach>

	</c:forEach>

</body>
</html>

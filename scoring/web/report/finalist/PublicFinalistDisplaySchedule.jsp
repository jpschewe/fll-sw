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

<style>
BODY {
	color: #ffffff;
	font-family: "Arial";
	background-color: #000080;
}
</style>

<!-- stuff for automatic scrolling -->
<script type="text/javascript">
	var scrollTimer;
	var scrollAmount = 2; // scroll by 100 pixels each time
	var documentYposition = 0;
	var scrollPause = 100; // amount of time, in milliseconds, to pause between scrolls

	//http://www.evolt.org/article/document_body_doctype_switching_and_more/17/30655/index.html
	function getScrollPosition() {
		if (window.pageYOffset) {
			return window.pageYOffset
		} else if (document.documentElement
				&& document.documentElement.scrollTop) {
			return document.documentElement.scrollTop
		} else if (document.body) {
			return document.body.scrollTop
		}
	}

	function myScroll() {
		documentYposition += scrollAmount;
		window.scrollBy(0, scrollAmount);
		if (getScrollPosition() + 300 < documentYposition) { //wait 300 pixels until we refresh
			window.clearInterval(scrollTimer);
			window.scroll(0, 0); //scroll back to top and then refresh
			location.href = location.href;
		}
	}

	function start() {
		<c:if test="${not empty scroll}">
		scrollTimer = window.setInterval('myScroll()', scrollPause);
		</c:if>
	}
</script>

</head>

<body onload='start()'>
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

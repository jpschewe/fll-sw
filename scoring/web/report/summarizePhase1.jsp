<%@page import="fll.web.report.SummarizePhase1"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
	SummarizePhase1.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/fll-sw.css'/>" />
<title>Summarize Scores</title>
</head>

<body>
	<h1>Summarize Scores</h1>

<!--  ${ERROR} -->
	<c:choose>
		<c:when test="${not empty ERROR}">
			<p>There was an error summarizing the scores. The most common
				problem is that there are not enough scores in a judging group. Each
				group must have 2 or more scores that are not no shows.</p>

			<p class='error'>${ERROR}</p>
		</c:when>
		<c:otherwise>

			<p>
				Below is a list of judges found and what categories they scored.<b>Note:</b>
				If there is no schedule loaded and there are multiple judging groups
				for an award group, then the number of teams expected will be too
				high.
			</p>

			<table border='1'>
				<tr>
					<th>Judge</th>
					<th>Category</th>
					<th>Judging Group</th>
					<th>Num Teams Expected</th>
					<th>Num Teams Scored</th>
				</tr>
				<c:forEach items="${judgeSummary}" var="judgeInfo">
					<tr>
						<td>${judgeInfo.judge}</td>
						<td>${judgeInfo.category }</td>
						<td>${judgeInfo.group}</td>
						<td>${judgeInfo.numExpected}</td>
						<td>${judgeInfo.numActual}</td>
					</tr>
				</c:forEach>
			</table>

			<form action="summarizePhase2.jsp">
				<p>
					If this does not look correct, go back to the <a
						href="../index.jsp">main page and correct the scores</a>.
					Otherwise <input type='submit' value="finish" id='finish' /> the
					score summarization.
				</p>
			</form>

		</c:otherwise>
	</c:choose>

</body>
</html>

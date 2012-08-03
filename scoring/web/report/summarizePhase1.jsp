<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />
<title>Summarize Scores</title>
</head>

<body>
	<h1>
		<x:out select="$challengeDocument/fll/@title" />
		(Summarize Scores)
	</h1>

	<p>
		Below is a list of judges found and what categories they scored.<b>Note:</b>
		If there is no schedule loaded and there are multiple judging stations
		for a division, then the number of teams expected will be too high.
	</p>

	<table border='1'>
		<tr>
			<th>Judge</th>
			<th>Category</th>
			<th>Station</th>
			<th>Num Teams Expected</th>
			<th>Num Teams Scored</th>
		</tr>
		<c:forEach items="${judgeSummary}" var="judgeInfo">
			<tr>
				<td>${judgeInfo.judge}</td>
				<td>${judgeInfo.category }</td>
				<td>${judgeInfo.station}</td>
				<td>${judgeInfo.numExpected}</td>
				<td>${judgeInfo.numActual}</td>
			</tr>
		</c:forEach>
	</table>

	<p>
		If these look correct, <a href="summarizePhase2.jsp" id='continue'>continue</a>
		on to the second phase of computing the scores. This page will return
		you to the reporting menu if everything is ok.
	</p>


</body>
</html>

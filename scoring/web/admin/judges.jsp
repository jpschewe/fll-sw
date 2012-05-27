<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.admin.Judges"%>

<html>
<head>
<title>Judge Assignments</title>
<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/style.jsp'/>" />

<script type='text/javascript'>
	//FIXME need to get these values from Java
	var maxIndex = 31;
	
	var divisions = [];
	<c:forEach items="${DIVISIONS}" var="divName">
		divisions.push("${divName}");
	</c:forEach>
		
	var categories = {};
	<c:forEach items="${CATEGORIES}" var="cat">
	  categories["${cat.key}"] = "${cat.value}";
	</c:forEach>
	
</script>

<script type='text/javascript' src='../extlib/jquery-1.7.1.min.js'></script>
<script type='text/javascript' src='judges.js'></script>

</head>

<body>
	<h1>
		<x:out select="$challengeDocument/fll/@title" />
		(Judge Assignments)
	</h1>

	<form action='judges.jsp' method='POST' name='judges'>


		<%
			Judges.generatePage(out, application, request, response);
		%>

		<input type='text' name='num_rows' id='num_rows' value='1' size='10' />
		<button id='add_rows'>Add Rows</button> <br/>

		<input type='submit' name='finished' value='Finished' /><br />

	</form>

</body>
</html>

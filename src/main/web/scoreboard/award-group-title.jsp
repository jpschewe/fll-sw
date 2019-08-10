<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.scoreboard.AwardGroupTitle.populateContext(application, session, pageContext);
%>

<html>
<head>
<meta http-equiv='refresh' content='90' />

<link rel="stylesheet" type="text/css"
	href="<c:url value='/style/base.css'/>" />

</head>
<body bgcolor='#008000'>
	<div class='center bold'>
		<font face='arial' size='3' color='#ffffff'> ${awardGroupTitle}
		</font>
	</div>
</body>
</html>

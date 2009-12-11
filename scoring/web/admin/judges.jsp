<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.admin.Judges"%>

<html>
<head>
<title>Judge Assignments</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1><x:out select="$challengeDocument/fll/@title" /> (Judge
Assignments)</h1>
<%
Judges.generatePage(out, application, request, response);
%>
<%@ include file="/WEB-INF/jspf/footer.jspf"%>
</body>
</html>

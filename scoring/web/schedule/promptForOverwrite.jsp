<%@ include file="/WEB-INF/jspf/init.jspf"%>

<html>
<head>
<title>Upload Schedule</title>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
</head>

<body>
<h1>Upload Schedule</h1>

${message}
<%-- clear out the message, so that we don't see it again --%>
<c:remove var="message" />

<p>
A schedule already exists for the current tournament, should it be overwritten?<br/>
<a href='<c:url value="/schedule/GetSheetNames"/>'>Yes</a>
<a href='<c:url value="/admin/index.jsp"/>'>No</a><br/>
</p>

</body>
</html>

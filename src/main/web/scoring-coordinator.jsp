<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.PageVariables.populateCompletedRunData(application, pageContext);
%>

<html>
<head>
<title>Scoring Coordinator</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Scoring Coordinator - ${challengeDescription.title }</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>



</body>
</html>

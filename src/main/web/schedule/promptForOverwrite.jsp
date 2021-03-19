<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<title>Upload Schedule</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Upload Schedule</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>
        A schedule already exists for the current tournament, should it
        be overwritten?
        <br />
        <a href='<c:url value="/schedule/scheduleConstraints.jsp"/>'>
            <button>Yes</button>
        </a>
        <a href='<c:url value="/admin/index.jsp"/>'>
            <button>No</button>
        </a>
        <br />
    </p>

</body>
</html>

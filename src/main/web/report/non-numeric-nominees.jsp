<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="JUDGE" allowSetup="false" />

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Non-Numeric Nominees</title>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/extlib/js-joda/packages/core/dist/js-joda.min.js'/>"></script>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.css'/>" />
<script type="text/javascript"
    src="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-storage.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/report/finalist/finalist.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/report/finalist/non-numeric_ui.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/report/non-numeric-nominees.js'/>"></script>

</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <h1>Non Numeric Categories</h1>

    <p>This page allows you to select teams that are nominated for
        awards that do not have scores in the database.</p>

    <h2>Overall</h2>
    <p>These categories are awarded for the whole tournament rather
        than per award group.</p>
    <ul id='overall-categories'>
    </ul>

    <h2>
        Award Group:
        <select id='divisions'></select>
    </h2>

    <ul id='categories'>
    </ul>

    <button id="nominees_store">Store Nominees</button>

    <div id="wait-dialog">
        <p id='wait-dialog_text'>Loading data. Please wait...</p>
    </div>
</html>

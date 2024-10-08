<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Non-Numeric Nominees</title>

<script type='text/javascript'
    src="<c:url value='/extlib/js-joda/js-joda.min.js'/>"></script>

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

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <h1>Non Numeric Categories</h1>

    <p>This page allows you to select teams that are nominated for
        awards that do not have scores in the database. This information
        feeds into the finalist scheduling tool and the deliberations
        tool.</p>

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

    <div class="fll-sw-ui-dialog fll-sw-ui-inactive" id="wait-dialog">
        <div>
            <p id='wait-dialog_text'>Loading data. Please wait...</p>
        </div>
    </div>
</html>

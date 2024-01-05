<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
if (fll.web.report.PromptSummarizeScores.checkIfSummaryUpdated(request, response, application, session,
		"/report/deliberation/deliberation_load.jsp")) {
	return;
}
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Deliberation Load</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src="<c:url value='/extlib/js-joda/packages/core/dist/js-joda.min.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript' src="<c:url value='/js/fll-storage.js'/>"></script>

<script type='text/javascript'
    src="<c:url value='/report/finalist/finalist.js'/>"></script>
<script type='text/javascript' src='deliberation_load.js'></script>

</head>

<body>
    <div id='choose_clear'>
        You already have deliberation data for this tournament stored in
        your browser. Would you like to clear the existing existing data
        and load from the database?
        <div>
            <button id='clear'>Yes, load from the database</button>
            This is the most common choice. This will clear any locally
            stored data and reload from the database.
            <i>Note that this will also clear any local finalist
                scheduling data. Make sure you have saved the finalist
                schedule before choosing this options.</i>
        </div>

        <div>
            <button id='keep'>No, just refresh the list of
                teams and the performance scores</button>
            Choose this if you have made changes to deliberations and
            have not saved this information to the database.
        </div>
    </div>

    <div class="fll-sw-ui-dialog fll-sw-ui-inactive" id="wait-dialog">
        <div>
            <div id='wait-dialog_text'>Loading data. Please
                wait...</div>
        </div>
    </div>


</body>
</html>

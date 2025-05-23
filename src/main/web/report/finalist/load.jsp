<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE, REPORT_GENERATOR"
    allowSetup="false" />

<%
fll.ScoreStandardization.computeSummarizedScoresIfNeeded(application);
%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Finalist Schedule Load</title>

<script type='text/javascript' src='../../extlib/js-joda/js-joda.min.js'></script>

<script type='text/javascript' src='../../js/fll-functions.js'></script>
<script type='text/javascript' src='../../js/fll-objects.js'></script>
<script type='text/javascript' src='../../js/fll-storage.js'></script>

<script type='text/javascript' src='finalist.js'></script>

<script type='text/javascript' src='finalist_load.js'></script>

</head>

<body>

    <p>
        You should create all head to head brackets using the <a
            href="<c:url value='/playoff'/>">Head to Head Page</a>
        before entering finalist information.
    </p>

    <div id='choose_clear'>
        You already have finalist data for this tournament stored in
        your browser. Would you like to clear the existing existing data
        and load from the database?
        <div>
            <button id='clear'>Yes, load from the database</button>
            This is the most common choice. This will clear any locally
            stored data and reload from the database.
        </div>

        <div>
            <button id='keep'>No, just refresh the list of
                teams and their scores</button>
            Choose this if you have made changes to finalists and have
            not saved this information to the database.
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

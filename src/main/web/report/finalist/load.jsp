<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<%
	if (fll.web.report.PromptSummarizeScores.checkIfSummaryUpdated(response, application, session,
		"/report/finalist/load.jsp")) {
	return;
}
%>
<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Finalist Schedule Load</title>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/extlib/jquery-json/dist/jquery.json.min.js' />"></script>

<script type='text/javascript'
    src='../../extlib/js-joda/packages/core/dist/js-joda.min.js'></script>

<link rel="stylesheet" type="text/css"
    href='../../extlib/jquery-ui-1.12.1/jquery-ui.min.css' />

<script type="text/javascript"
    src='../../extlib/jquery-ui-1.12.1/jquery-ui.min.js'></script>

<script type='text/javascript' src='../../js/fll-functions.js'></script>
<script type='text/javascript' src='../../js/fll-objects.js'></script>
<script type='text/javascript' src='../../js/fll-storage.js'></script>

<script type='text/javascript' src='finalist.js'></script>

<script type='text/javascript' src='finalist_load.js'></script>

<script type='text/javascript'>
  var _loadingTournament = <%=FinalistLoad.currentTournament(application)%> ;

  function loadData() {
    $("#wait-dialog").dialog("open");

    _log("Loading data");
<%FinalistLoad.outputDivisions(out, application);%>
  
<%FinalistLoad.outputTeamVariables(out, application);%>
  
<%FinalistLoad.outputCategories(out, application);%>
  var championship = $.finalist
        .getCategoryByName($.finalist.CHAMPIONSHIP_NAME);
    if (null == championship) {
      championship = $.finalist.addCategory($.finalist.CHAMPIONSHIP_NAME, true,
          false);
    }
    $.finalist.setCategoryScheduled(championship, true);
<%FinalistLoad.outputCategoryScores(out, application);%>
  
<%FinalistLoad.outputNonNumericNominees(out, application);%>
  
  $.finalist.setTournament(_loadingTournament);

  finalistScheduleLoad.loadUsingApi();
  }
</script>

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
            This is the most common choice.
        </div>

        <div>
            <button id='keep'>No, just refresh the data</button>
            Choose this if you have made changes to finalists and have
            not saved this information to the database.
        </div>
    </div>

    <div id="wait-dialog">
        <p id='wait-dialog_text'>Loading data. Please wait...</p>
    </div>


</body>
</html>

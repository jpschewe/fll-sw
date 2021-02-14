<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Finalist Schedule Load</title>

<script type='text/javascript'
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/extlib/jquery-json/dist/jquery.json.min.js' />"></script>
<script type='text/javascript'
    src="<c:url value='/extlib/jStorage/jstorage.min.js' />"></script>
<script type='text/javascript' src='finalist.js'></script>
<script type='text/javascript'>
  var _loadingTournament =
<%=FinalistLoad.currentTournament(application)%>
  ;

  function clearData() {
    $.finalist.clearAllData();
  }

  function loadData() {
    console.log("Loading data");
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
  
<%FinalistLoad.outputSchedules(out, application);%>
  $.finalist.setTournament(_loadingTournament);
  }

  $(document).ready(function() {
    $("#choose_clear").hide();
    $("#clear").click(function() {
      clearData();
      loadData();
      $("#choose_clear").hide();
      location.href = "params.html";
    });
    $("#keep").click(function() {
      loadData();
      $("#choose_clear").hide();
      location.href = "params.html";
    });

    var allTeams = $.finalist.getAllTeams();
    var tournament = $.finalist.getTournament();

    if (null != allTeams && allTeams.length > 0) {
      if (tournament != _loadingTournament) {
        console.log("Clearing data for old tournament: " + tournament);
        clearData();
        loadData();
        $("#choose_clear").hide();
        location.href = "params.html";
      } else {
        $("#choose_clear").show();
      }
    } else {
      loadData();
      $("#choose_clear").hide();
      location.href = "params.html";
    }
  });
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


</body>
</html>

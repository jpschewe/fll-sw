<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Finalist Schedule Load</title>

<script type='text/javascript' src='../../extlib/jquery-1.7.1.min.js'></script>
<script type='text/javascript' src='../../extlib/jquery.json-2.3.min.js'></script>
<script type='text/javascript' src='../../extlib/jstorage.js'></script>
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
			championship = $.finalist.addCategory($.finalist.CHAMPIONSHIP_NAME, true);
		}
<%FinalistLoad.outputCategoryScores(out, application);%>
	$.finalist.setTournament(_loadingTournament);
	}

	$(document).ready(function() {
		$("#choose_clear").hide();
		$("#clear").click(function() {
			clearData();
			loadData();
			$("#choose_clear").hide();
			location.href="params.html";
		});
		$("#keep").click(function() {
			loadData();
			$("#choose_clear").hide();
			location.href="params.html";
		});

		var allTeams = $.finalist.getAllTeams();
		var tournament = $.finalist.getTournament();

		if (null != allTeams && allTeams.length > 0) {
			if (tournament != _loadingTournament) {
				console.log("Clearing data for old tournament: " + tournament);
				clearData();
				loadData();
				$("#choose_clear").hide();
				location.href="params.html";
			} else {
				$("#choose_clear").show();
			}
		} else {
			loadData();
			$("#choose_clear").hide();
			location.href="params.html";
		}
	});
</script>

</head>

<body>

	<div id='choose_clear'>
		You already have data loaded for this tournament. Would you like to
		clear the existing data and load from scratch?
		<button id='clear'>Yes, clear the data</button>
		<button id='keep'>No, just refresh the data</button>
	</div>


</body>
</html>

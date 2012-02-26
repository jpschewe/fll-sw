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
<%=FinalistLoad.currentTournament(session)%>
	;

	function clearData() {
		$.finalist.clearAllData();
	}

	function loadData() {
		console.log("Loading data");
<%FinalistLoad.outputDivisions(out, session);%>
	
<%FinalistLoad.outputTeamVariables(out, session);%>
	
<%FinalistLoad.outputCategories(out, application);%>

	//FIXME generate this list

		// need judging group for each category for each team
		$.finalist.setCategoryScore(team29, robot_designCategory, 102.5, "D1A");

		// add overall score
		$.finalist.setOverallScore(team29, 572.3, "D1A");

		$.finalist.setTournament(_loadingTournament);
	}

	$(document).ready(function() {
		var numTeamsAutoSelected = $.finalist.getNumTeamsAutoSelected();
		$("#numSelected").val(numTeamsAutoSelected);
		$("#numSelected").change(function() {
			$.finalist.setNumTeamsAutoSelected($(this).val());
		});

		$("#choose_numSelected").hide();
		$("#choose_clear").hide();
		$("#clear").click(function() {
			clearData();
			loadData();
			$("#choose_clear").hide();
			$("#choose_numSelected").show();
		});
		$("#keep").click(function() {
			loadData();
			$("#choose_clear").hide();
			$("#choose_numSelected").show();
		});

		var allTeams = $.finalist.getAllTeams();
		var tournament = $.finalist.getTournament();

		if (null != allTeams && allTeams.length > 0) {
			if (tournament != _loadingTournament) {
				console.log("Clearing data for old tournament: " + tournament);
				clearData();
				loadData();
				$("#choose_clear").hide();
				$("#choose_numSelected").show();
			} else {
				$("#choose_clear").show();
			}
		} else {
			loadData();
			$("#choose_clear").hide();
			$("#choose_numSelected").show();
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

	<form id='choose_numSelected' action='params.html'>
		Please choose the number of teams that should be automatically
		selected from each judging group. You will have an opportunity to
		change these selections. <select id='numSelected'><option
				value='0'>0</option>
			<option value='1'>1</option>
			<option value='2'>2</option>
			<option value='3'>3</option>
			<option value='4'>4</option>
			<option value='5'>5</option></select> <br /> <input type='submit'
			value='Next' />
	</form>


</body>
</html>

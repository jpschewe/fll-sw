<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<title>Non-Numeric Nominees</title>

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>
<script type='text/javascript'
 src='../extlib/jquery.json-2.5-pre.min.js'></script>
<script type='text/javascript' src='../extlib/jstorage-0.4.11.min.js'></script>
<script type='text/javascript' src='../js/fll-objects.js'></script>
<script type='text/javascript' src='finalist/finalist.js'></script>


<script type='text/javascript'>
	function loadData() {
		console.log("Loading data");

		var _loadingTournament =
<%=FinalistLoad.currentTournament(application)%>
	;
		var tournament = $.finalist.getTournament();
		if (tournament != _loadingTournament) {
			console.log("Clearing data for old tournament: " + tournament);
			$.finalist.clearAllData();
		}
<%FinalistLoad.outputDivisions(out, application);%>
	
<%FinalistLoad.outputTeamVariables(out, application);%>
	
<%FinalistLoad.outputNonNumericNominees(out, application);%>
	$.finalist.setTournament(_loadingTournament);

		// make sure the navbar isn't shown
		$("#nominees_content").load(function() {
			$("#nominees_content").contents().find("#navbar").hide();
		});

		$("#nominees_content").attr('src', 'finalist/non-numeric.html');

	}

	function storeNominees() {
		var nonNumericNominees = [];
		$.each($.finalist.getNonNumericCategories(), function(i, category) {
			var teamNumbers = [];
			$.each(category.teams, function(j, team) {
				teamNumbers.push(team);
			}); // foreach team
			var nominees = new NonNumericNominees(category.name, teamNumbers);
			nonNumericNominees.push(nominees);
		}); // foreach category
		$('#non-numeric-nominees_data').val($.toJSON(nonNumericNominees));
	}

	$(document).ready(function() {
		loadData();

		$("#nominees_store").click(function() {
			storeNominees();
			$("#nominees_form").submit();
		});
	});
</script>

</head>

<body>

 <div class='status-message'>${message}</div>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form id='nominees_form' method='POST' action='StoreNonNumericNominees'>

  <input type='hidden' id='non-numeric-nominees_data'
   name='non-numeric-nominees_data' />

  <button id="nominees_store">Store Nominees</button>
 </form>

 <iframe width="100%" height="90%" frameBorder="0" id="nominees_content"> </iframe>
</html>

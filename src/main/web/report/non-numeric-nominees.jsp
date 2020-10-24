<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.report.finalist.FinalistLoad"%>

<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'>
<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<title>Non-Numeric Nominees</title>

<script type='text/javascript' src='../extlib/jquery-1.11.1.min.js'></script>
<script type='text/javascript'
    src='../extlib/jquery.json-2.5-pre.min.js'></script>
<script type='text/javascript' src='../extlib/jstorage-0.4.12/jstorage.min.js'></script>
<script type='text/javascript' src='../js/fll-objects.js'></script>
<script type='text/javascript' src='finalist/finalist.js'></script>
<script type='text/javascript' src='finalist/non-numeric.js'></script>


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
  
<%FinalistLoad.outputCategories(out, application);%>
  
<%FinalistLoad.outputNonNumericNominees(out, application);%>
  $.finalist.setTournament(_loadingTournament);
  }

  function storeNominees() {
    var allNonNumericNominees = $.finalist.prepareNonNumericNomineesToSend();
    $('#non-numeric-nominees_data').val($.toJSON(allNonNumericNominees));
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

    <form id='nominees_form' method='POST'
        action='StoreNonNumericNominees'>

        <input type='hidden' id='non-numeric-nominees_data'
            name='non-numeric-nominees_data' />

        <button id="nominees_store">Store Nominees</button>
    </form>

    <h1>Non Numeric Categories</h1>

    <p>This page allows you to select teams that are nominated for
        awards that do not have scores in the database.</p>

    <p>The checkbox next to the category specifies if the category
        should be scheduled for finalist judging. If this tournament
        does not have finalist judging this checkbox can be ignored.</p>

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
</html>

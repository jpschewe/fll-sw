<%@ page import="fll.web.admin.Tournaments"%>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.Tournaments.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Tournaments</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-1.11.1.min.js' />"></script>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.css' />" />

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-ui-1.12.1/jquery-ui.min.js' />"></script>

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<script type="text/javascript" src="tournaments.js"></script>

<script type="text/javascript">
  const NEW_TOURNAMENT_PREFIX = "${NEW_TOURNAMENT_PREFIX}";
  const KEY_PREFIX = "${KEY_PREFIX}";
  const NAME_PREFIX = "${NAME_PREFIX}";
  const DESCRIPTION_PREFIX = "${DESCRIPTION_PREFIX}";
  const DATE_PREFIX = "${DATE_PREFIX}";
  const LEVEL_PREFIX = "${LEVEL_PREFIX}";
  const currentTournamentId = "${currentTournamentId}";

  /**
   * Add all tournament levels to the specified DOM select element.
   */
  function populateLevelSelect(levelSelect) {
    <c:forEach items="${tournamentLevels}" var="level" varStatus="loop">
    const newOption${loop.index} = document.createElement("option");
    newOption${loop.index}.setAttribute("value", "${level.id}");
    newOption${loop.index}.innerText = "${level.name}";
    levelSelect.appendChild(newOption${loop.index});
    </c:forEach>
  }

  document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("addRow").addEventListener("click", function() {
      addNewTournament();
    });

    setupDatepickers();
    
    <c:forEach items="${tournamentsWithScores}" var="tournamentId">
    tournamentsWithScores.push("${tournamentId}");
    </c:forEach>

    <c:forEach items="${tournaments}" var="tournament">
    addTournament("${tournament.tournamentID}", "${tournament.name}",
        "${tournament.description}", "${tournament.dateString}",
        "${tournament.level.id}");
    </c:forEach>
  });
</script>
</head>

<body>
    <h1>Edit Tournaments</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>Enter the details for the tournaments.</p>
    <ul>
        <li>Date - this field is displayed on some reports and is
            used when sorting the list of tournaments in the select
            tournament menu</li>
        <li>
            Short Name - this field is displayed in the subjective web
            application, score sheets and other locations that have
            limited space.
            <i>Must be unique</i>
            . Clearing this field will delete the tournament.
        </li>
        <li>Long Name - this field is displayed on the reports</li>
        <li>Level - the level of the tournament, may be left empty.
            This field is used in the awards report.</li>
        <li>Next Level - the level of the tournament that teams
            will advance to from this tournament, may be left empty.
            This field is used in the awards report.</li>
    </ul>

    <form action="<c:url value='/admin/Tournaments' />" method="POST"
        name="tournaments">

        <table border="1" id="tournamentsTable">
            <tr>
                <th>Date</th>
                <th>Short Name</th>
                <th>Long Name</th>
                <th>Level</th>
                <th>&nbsp;</th>
            </tr>

        </table>

        <!--  -->
        <button id='addRow' type='button'>Add Row</button>
        <!--  -->
        <input type='submit' name='commit' value='Finished'
            onclick='return checkTournamentNames()' />
    </form>
</body>
</html>

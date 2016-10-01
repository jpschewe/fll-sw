<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.playoff.CreatePlayoffDivision.populateContext(application, pageContext);
%>

<html>
<head>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/fll-sw.css'/>" />
<title>Create Playoff Bracket</title>


<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>

<script type='text/javascript'>
  var numSelected = 0;

  $(document).ready(function() {
    <c:forEach items="${judgingStations }" var="station" varStatus="idx">
    $("#station_select_${idx.count }").change(function() {
      <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
      <c:if test="${team.judgingGroup == station}">
      $("#select_${team.teamNumber}").prop("checked", $(this).is(":checked"));
      </c:if>
      </c:forEach>
      updateTeamsSelected();
    });
    </c:forEach>

    <c:forEach items="${awardGroups }" var="awardGroup" varStatus="idx">
    $("#award_group_select_${idx.count }").change(function() {
      <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
      <c:if test="${team.awardGroup == awardGroup}">
      $("#select_${team.teamNumber}").prop("checked", $(this).is(":checked"));
      </c:if>
      </c:forEach>
      updateTeamsSelected();
    });
    </c:forEach>

    <c:forEach
    items="${playoff_data.tournamentTeamsValues }"
    var="team">
    $("#select_${team.teamNumber }").change(function() {
      if ($(this).is(":checked")) {
        numSelected = numSelected + 1;
      } else {
        numSelected = numSelected - 1;
      }
      $("#numTeams").text(numSelected);
    });
    </c:forEach>
  });

  // called when we don't know how many check boxes just changed and we need to recompute
  function updateTeamsSelected() {
    numSelected = 0;
    <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
    if ($("#select_${team.teamNumber }").is(":checked")) {
      numSelected = numSelected + 1;
    }
    </c:forEach>
    $("#numTeams").text(numSelected);
  }
</script>

</head>

<body>
  <h1>Create Playoff Bracket</h1>

  <div class='status-message'>${message}</div>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <form
    method="POST"
    action="CreatePlayoffDivision">

    <c:forEach
      items="${awardGroups }"
      var="awardGroup"
      varStatus="idx">
      <div>
        <c:if
          test="${not playoff_data.existingBrackets.contains(awardGroup)}">
          <input
            type='submit'
            name='create_award_group_${idx.count}'
            value='Create Head to Head Bracket for Award Group ${awardGroup }' />
          <input
            type='hidden'
            name='award_group_${idx.count}'
            value='${awardGroup}' />
        </c:if>
      </div>
    </c:forEach>

    <c:forEach
      items="${judgingStations }"
      var="station"
      varStatus="idx">
      <div>
        <c:if
          test="${not playoff_data.existingBrackets.contains(station)}">
          <input
            type='submit'
            name='create_judging_group_${idx.count}'
            value='Create Head to Head Bracket for Judging Group ${station }' />
          <input
            type='hidden'
            name='judging_group_${idx.count}'
            value='${station}' />
        </c:if>
      </div>
    </c:forEach>

    <hr />

    <p>Alternatively select the teams that you want to include in
      the playoff bracket and choose a name for the it.</p>

    <div>Select/unselect teams by award group</div>
    <c:forEach
      items="${awardGroups }"
      var="awardGroup"
      varStatus="idx">
      <div>
        <input
          type="checkbox"
          name="award_group_select_${idx.count}"
          id="award_group_select_${idx.count }" /> <label
          for="award_group_select_${idx.count}">${awardGroup }</label>
        <c:if
          test="${not playoff_data.existingBrackets.contains(awardGroup)}">
        </c:if>
      </div>
    </c:forEach>

    <div>Select/unselect teams by judging group</div>
    <c:forEach
      items="${judgingStations }"
      var="station"
      varStatus="idx">
      <div>
        <input
          type="checkbox"
          name="station_select_${idx.count}"
          id="station_select_${idx.count }" /> <label
          for="station_select_${idx.count}">${station }</label>
      </div>
    </c:forEach>

    <label for="division_name">Name: </label><input name="bracket_name" />
    <input
      type='submit'
      name='selected_teams'
      value='Create Playoff Bracket with selected teams' /> Number of
    teams selected: <span id='numTeams'>0</span><br />

    <table border='1'>

      <tr>
        <th>Select</th>
        <th>Number</th>
        <th>Name</th>
        <th>Judging Group</th>
        <th>Award Group</th>
      </tr>
      <c:forEach
        items="${playoff_data.tournamentTeamsValues }"
        var="team">
        <tr>

          <td><input
            name="selected_team"
            id="select_${team.teamNumber }"
            type="checkbox"
            value="${team.teamNumber }" /></td>

          <td>${team.teamNumber }</td>

          <td>${team.teamName }</td>

          <td>${team.judgingGroup }</td>

          <td>${team.awardGroup }</td>

        </tr>
      </c:forEach>
    </table>

  </form>

</body>
</html>

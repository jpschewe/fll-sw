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
  $(document).ready(function() {

    <c:forEach items="${judgingStations }" var="station" varStatus="idx">
    $("#station_select_${idx.count }").change(function() {
      <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
      <c:if test="${team.judgingStation == station}">
      $("#select_${team.teamNumber}").prop("checked", $(this).is(":checked"));
      </c:if>
      </c:forEach>
    });
    </c:forEach>

    <c:forEach items="${awardGroups }" var="awardGroup" varStatus="idx">
    $("#award_group_select_${idx.count }").change(function() {
      <c:forEach items="${playoff_data.tournamentTeamsValues }" var="team">
      <c:if test="${team.eventDivision == awardGroup}">
      $("#select_${team.teamNumber}").prop("checked", $(this).is(":checked"));
      </c:if>
      </c:forEach>
    });
    </c:forEach>
  });
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
            value='Create Playoff Bracket for Award Group ${awardGroup }' />
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
            value='Create Playoff Bracket for Judging Group ${station }' />
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
      value='Create Playoff Bracket with selected teams' /><br />

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

          <td>${team.judgingStation }</td>

          <td>${team.eventDivision }</td>

        </tr>
      </c:forEach>
    </table>

  </form>

</body>
</html>

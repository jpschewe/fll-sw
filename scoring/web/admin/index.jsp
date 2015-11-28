<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Administration</title>
<link
  rel="stylesheet"
  type="text/css"
  href="<c:url value='/style/style.jsp'/>" />
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }
</script>
</head>

<body>
  <h1>${challengeDescription.title }(Administration)</h1>

  ${message}
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <h2>Before tournament day</h2>
  <ol>
    <li>
      <form
        id='uploadTeams'
        ACTION="<c:url value='/UploadSpreadsheet'/>"
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        <c:if test="${teamsUploaded }">
          <span class='bold'>[DONE] </span>
        </c:if>
        Upload the datafile for teams. This file can be tab separated or
        comma separated or an Excel file (xls and xlsx supported). The
        filter functionality provided here is very basic and has very
        limited feedback. It's suggested that you edit the input file
        before upload to contain only the teams for your tournament(s).
        <a href='javascript:display("UploadTeamsHelp")'>[help]</a>
        <div
          id='UploadTeamsHelp'
          class='help'
          style='display: none'>
          Each column of the input file needs to matched against the
          required data for teams. This information includes: team
          number, team name, organization, initial tournament, division.
          The team number must be a number and is required. The other
          columns are not required, but are a good idea to include. You
          will be prompted to pick a column from your data file to match
          against each piece of team data that the software uses. You
          can select the same column for multiple pieces of data. <a
            href='javascript:hide("UploadTeamsHelp")'>[hide]</a>
        </div>

        <input
          type="file"
          size="32"
          id='teams_file'
          name="file"> <input
          type='hidden'
          name='uploadRedirect'
          value="<c:url value='/admin/UploadTeams'/>" /> <input
          type="submit"
          id='upload_teams'
          value="Upload">
      </form>
    </li>


    <li><a href='<c:url value="tournaments.jsp"/>'>Add or Edit
        Tournaments</a> <a href='javascript:display("EditTournamentHelp")'>[help]</a>
      <div
        id='EditTournamentHelp'
        class='help'
        style='display: none'>
        This is an optional step. Use this page to modify the
        tournaments created by team import step above, to assign
        tournament advancement (e.g. teams may advance from regional
        tournaments to the state tournament), or to create new
        tournaments.<br> <a
          href='javascript:hide("EditTournamentHelp")'>[hide]</a>
      </div></li>


    <li><a href='DisplayTournamentAssignments'>Display
        Tournament Assignments</a></li>


    <li>
      <form
        id='currentTournament'
        action='SetCurrentTournament'
        method="post">
        Current Tournament: <select
          id='currentTournamentSelect'
          name='currentTournament'>
          <c:forEach
            items="${tournaments }"
            var="tournament">
            <c:choose>
              <c:when
                test="${tournament.tournamentID == currentTournamentID}">
                <option
                  selected
                  value='${tournament.tournamentID }'>${tournament.location}
                  [ ${tournament.name } ]</option>
              </c:when>
              <c:otherwise>
                <option value='${tournament.tournamentID }'>${tournament.location}
                  [ ${tournament.name } ]</option>
              </c:otherwise>
            </c:choose>
          </c:forEach>
        </select> <input
          type='submit'
          name='change_tournament'
          value='Change tournament'>
      </form>
    </li>


    <li><c:if test="${scheduleUploaded }">
        <span class='bold'>[DONE] </span>
      </c:if> Upload schedule for current tournament. <a
      href='javascript:display("ScheduleHelp")'>[help]</a>
      <div
        id='ScheduleHelp'
        class='help'
        style='display: none'>
        Uploading the schedule isn't required, but if uploaded will be
        used when displaying information in final reports. <a
          href='javascript:hide("ScheduleHelp")'>[hide]</a>
      </div>
      <form
        id='uploadSchedule'
        action='<c:url value="/schedule/UploadSchedule"/>'
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        <input
          type="file"
          size="32"
          name="scheduleFile" /> <input
          type="submit"
          value="Upload Schedule" />
      </form> <c:if test="${scheduleUploaded }">
        <!--  downloads for the schedule -->
        <ul>
          <li><a href="ScheduleByTeam">Full schedule sorted by
              team</a></li>
          <li><a href="SubjectiveScheduleByJudgingStation">Subjective
              schedule sorted by judging station, then time</a></li>
          <li><a href="SubjectiveScheduleByTime">Subjective
              schedule sorted by time</a></li>
          <li><a href="PerformanceSchedule">Performance
              Schedule</a></li>
        </ul>
      </c:if></li>


    <li><a href='edit_event_division.jsp'> Assign event
        divisions to teams in current tournament</a>. <a
      href='javascript:display("EventDivisionHelp")'>[help]</a>
      <div
        id='EventDivisionHelp'
        class='help'
        style='display: none'>
        Typical tournaments have 2 groups of teams competing against
        each other, one for division 1 and one for division 2. If your
        tournament team groupings are not based solely on the division
        of the teams, e.g. you have 2 groups of teams that are all
        division 1, use this page to assign &ldquo;event
        divisions&rdquo; to divide your tournament&rsquo;s teams into
        the groups in which they will be competing.<br> <a
          href='javascript:hide("EventDivisionHelp")'>[hide]</a>
      </div></li>

  </ol>

  <h2>Tournament day</h2>
  <ol>
    <li>
      <form
        action='ChangeScorePageText'
        method='post'>
        Score page text: <input
          type='text'
          name='ScorePageText'
          value='<c:out value="${ScorePageText}"/>'> <input
          type='submit'
          value='Change text'>
      </form>
    </li>


    <li><c:if test="${judgesAssigned }">
        <span class='bold'>[DONE] </span>
      </c:if> <a
      href='<c:url value="GatherJudgeInformation"/>'
      id='assign_judges'>Assign Judges</a></li>


    <li><c:if test="${tablesAssigned}">
        <span class='bold'>[DONE] </span>
      </c:if> <a href='<c:url value="tables.jsp"/>'>Assign Table Labels</a>
      (for scoresheet printing during playoffs)</li>



    <li><a href='editTeam.jsp'>Add a team</a></li>


    <li><a href='<c:url value="select_team.jsp"/>'>Edit team
        data</a></li>


    <li><a href='remoteControl.jsp'>Remote control of display</a></li>

    <li><h3>Subjective Scoring</h3>
      <ul>
        <li><a href="subjective-data.fll">Download the datafile
            for subjective score entry.</a> Should be downloaded after each
          subjective score upload to lessen chance of data loss due to
          overwrite.</li>

        <li><a href='<c:url value="/subjective-app.jar"/>'>Subjective
            Scoring Application</a> (Executable Jar file) - run with "java
          -jar subjective-app.jar"</li>

        <li><a href='<c:url value="/subjective/index.html"/>'>Subjective
            Web application</a></li>


      </ul></li>

    <li>
      <form
        name='uploadSubjective'
        ACTION='<c:url value="UploadSubjectiveData"/>'
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        Upload the datafile for subjective scores. <input
          type="file"
          size="32"
          name="subjectiveFile" /> <input
          type="submit"
          value="Upload" />
      </form>
    </li>


  </ol>

  <h2>After the tournament</h2>
  <ul>
    <li><a href='database.flldb'>Download database</a></li>

  </ul>

  <h2>User Management</h2>
  <ul>
    <li><a href="changePassword.jsp">Change Password</a></li>
    <li><a href="createUsername.jsp">Create User</a>
    <li><a href="removeUser.jsp">Remove User</a>
  </ul>

  <h2>Advanced</h2>
  <p>These links are for advanced users.</p>
  <ul>
    <li><a href="edit_all_parameters.jsp">Edit all parameters</a></li>

    <li><a href="GatherAdvancementData">Advance teams</a></li>

    <li>
      <form
        id='uploadAdvancingTeams'
        ACTION="<c:url value='/UploadSpreadsheet'/>"
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        Upload CSV or Excel of teams to advance <input
          type="file"
          size="32"
          name="file" /> <input
          type='hidden'
          name='uploadRedirect'
          value="<c:url value='/admin/UploadAdvancingTeams'/>" /> <input
          type="submit"
          value="Upload" />
      </form>
    </li>

  </ul>

</body>
</html>

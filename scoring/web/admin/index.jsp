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
  href="<c:url value='/style/fll-sw.css'/>" />


<style>
.completed {
	background-color: #00FF00;
	font-weight: bold;
}
</style>

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

  <div class='status-message'>${message}</div>
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
          <span class='completed'>DONE </span>
        </c:if>
        Upload the datafile for teams. This file can be tab separated or
        comma separated or an Excel file (xls and xlsx supported). <input
          type="file"
          size="32"
          id='teams_file'
          name="file"> <input
          type='hidden'
          name='uploadRedirect'
          value="<c:url value='/admin/UploadTeams'/>" /> <input
          type="submit"
          id='upload_teams'
          value="Upload"> <a
          href='javascript:display("UploadTeamsHelp")'>[help]</a>
        <div
          id='UploadTeamsHelp'
          class='help'
          style='display: none'>
          Each column of the input file needs to matched against the
          required data for teams. The column names are the first row of
          your data file. This information includes: team number, team
          name, organization, initial tournament, award group, judging
          group. The team number must be a number and is required. The
          other columns are not required, but are a good idea to
          include. You will be prompted to pick a column from your data
          file to match against each piece of team data that the
          software uses. You can select the same column for multiple
          pieces of data. <a href='javascript:hide("UploadTeamsHelp")'>[hide]</a>
        </div>

      </form>
    </li>

    <li><a href='editTeam.jsp'>Add a team</a> <a
      href='javascript:display("AddTeamHelp")'>[help]</a>
      <div
        id='AddTeamHelp'
        class='help'
        style='display: none'>
        This can be used to add a team to the software that wasn't added
        through the team upload. This may be used for small tournaments
        where all of the teams aren't known in advance or a team shows
        up for a tournament at the last minute. Special care needs to be
        taken when adding a team to an already running tournament to
        ensure that they have a schedule for where to go when and that
        they get judged properly. In most cases this function should not
        be used at the tournament, but rather before the tournament.<a
          href='javascript:hide("AddTeamHelp")'>[hide]</a>
      </div></li>

    <li><a id='add-edit-tournaments' href='<c:url value="tournaments.jsp"/>'>Add or Edit
        Tournaments</a> <a href='javascript:display("EditTournamentHelp")'>[help]</a>
      <div
        id='EditTournamentHelp'
        class='help'
        style='display: none'>
        This is an optional step. Use this page to modify the
        tournaments created by team upload step above, or to create new
        tournaments.<br> <a
          href='javascript:hide("EditTournamentHelp")'>[hide]</a>
      </div></li>


    <li><a href='DisplayTournamentAssignments'>Display
        Tournament Assignments</a> <a
      href='javascript:display("DisplayTournamentAssignmentsHelp")'>[help]</a>
      <div
        id='DisplayTournamentAssignmentsHelp'
        class='help'
        style='display: none'>
        This is an optional step. Use this page to display what teams
        are assigned to what tournament and what judging groups they are
        in.<br> <a
          href='javascript:hide("DisplayTournamentAssignmentsHelp")'>[hide]</a>
      </div></li>


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
                  value='${tournament.tournamentID }'>${tournament.description}
                  [ ${tournament.name } ]</option>
              </c:when>
              <c:otherwise>
                <option value='${tournament.tournamentID }'>${tournament.description}
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
        <span class='completed'>DONE </span>
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
          id="upload-schedule"
          type="submit"
          value="Upload Schedule" />
      </form> <c:if test="${scheduleUploaded }">
        <!--  downloads for the schedule -->
        <ul>
          <li><a
            href="ScheduleByTeam"
            target="_new">Full schedule sorted by team</a></li>
          <li><a
            href="SubjectiveScheduleByJudgingStation"
            target="_new">Subjective schedule sorted by judging
              group, then time</a></li>
          <li><a
            href="SubjectiveScheduleByTime"
            target="_new">Subjective schedule sorted by time</a></li>
          <li><a
            href="PerformanceSchedule"
            target="_new">Performance Schedule</a></li>
        </ul>
      </c:if></li>


    <li><a id='change-award-groups' href='edit_event_division.jsp'>Change award group
        assignments for the current tournament</a>. <a
      href='javascript:display("EventDivisionHelp")'>[help]</a>
      <div
        id='EventDivisionHelp'
        class='help'
        style='display: none'>
        This information is typically specified in the schedule data
        file. If it needs to be modified you can use this page to change
        the award groups for teams in the current tournament. <br>
        <a href='javascript:hide("EventDivisionHelp")'>[hide]</a>
      </div></li>


    <li><a href='edit_judging_groups.jsp'>Change judging groups
        assignments for the current tournament</a>. <a
      href='javascript:display("JudgingGroupHelp")'>[help]</a>
      <div
        id='JudgingGroupHelp'
        class='help'
        style='display: none'>
        This information is typically specified in the schedule data
        file. If it needs to be modified you can use this page to change
        the judging groups for teams in the current tournament. <br>
        <a href='javascript:hide("JudgingGroupHelp")'>[hide]</a>
      </div></li>

  </ol>

  <h2>Tournament day</h2>
  <ol>
    <li>
      <form
        action='ChangeScorePageText'
        method='post'>
        Scoring display text: <input
          type='text'
          name='ScorePageText'
          value='<c:out value="${ScorePageText}"/>'> <input
          type='submit'
          value='Change text'> <a
          href='javascript:display("ScorePageTextHelp")'>[help]</a>
        <div
          id='ScorePageTextHelp'
          class='help'
          style='display: none'>
          This text is displayed on the various big screen display
          pages. There is only 1 or 2 lines of space available, so keep
          it short. This can be used to notify participants and
          spectators of when the next break will be over.<a
            href='javascript:hide("ScorePageTextHelp")'>[hide]</a>
        </div>
      </form>
    </li>


    <li><c:if test="${judgesAssigned }">
        <span class='completed'>DONE </span>
      </c:if> <a
      href='<c:url value="GatherJudgeInformation"/>'
      id='assign_judges'>Assign Judges</a></li>


    <li><c:if test="${tablesAssigned}">
        <span class='completed'>DONE </span>
      </c:if> <a href='<c:url value="tables.jsp"/>'>Assign Table Labels</a>
      (for scoresheet printing during head to head)</li>



    <li><a href='<c:url value="select_team.jsp"/>'>Edit team
        data</a></li>


    <li><a id='remote-control' href='remoteControl.jsp'>Remote control of display</a></li>

    <li><h3>Subjective Scoring</h3>
      <ul>
        <li><a
          href='<c:url value="/subjective/index.html"/>'
          target="_new">Subjective Web application</a></li>


        <li><a href="subjective-data.fll">Download the datafile
            for subjective score entry.</a> Should be downloaded after each
          subjective score upload to lessen chance of data loss due to
          overwrite.</li>

        <li><a href='<c:url value="/subjective-app.jar"/>'>Subjective
            Scoring Application</a> (Executable Jar file) - run with "java
          -jar subjective-app.jar"</li>

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
              id='uploadSubjectiveFile'
              type="submit"
              value="Upload" />
          </form>
        </li>

      </ul></li>

    <li>Once the seeding rounds have been completed you will need
      to setup the <a
      href="../playoff"
      target="_new">head to head brackets</a>.
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

    <li>
      <form
        id='uploadTeamTournamentAssignments'
        ACTION="<c:url value='/UploadSpreadsheet'/>"
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        Upload CSV or Excel of teams and tournaments to assign them to <input
          type="file"
          size="32"
          name="file" /> <input
          type='hidden'
          name='uploadRedirect'
          value="<c:url value='/admin/UploadTeamTournamentAssignments'/>" />
        <input
          type="submit"
          value="Upload" />
      </form>
    </li>

    <li>
      <form
        id='uploadTeamInformation'
        ACTION="<c:url value='/UploadSpreadsheet'/>"
        METHOD="POST"
        ENCTYPE="multipart/form-data">
        Upload CSV or Excel of updated information for teams <input
          type="file"
          size="32"
          name="file" /> <input
          type='hidden'
          name='uploadRedirect'
          value="<c:url value='/admin/UploadTeamInformation'/>" /> <input
          type="submit"
          value="Upload" />
      </form>
    </li>


  </ul>

</body>
</html>

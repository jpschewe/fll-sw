<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.AdminIndex.populateContext(application, session, pageContext, true);
%>

<html>
<head>
<title>Administration</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />


<style>
.completed {
    background-color: #00FF00;
    font-weight: bold;
}
</style>

<script type="text/javascript" src="index.js"></script>
</head>

<body>
    <h1>${challengeDescription.title }(Administration)</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <h2>Before tournament day</h2>
    <ol>
        <li>
            <a href="<c:url value='/report/awards/index.jsp'/>">Edit
                awards report and awards script properties.</a>
        </li>

        <li>
            <form id='uploadTeams'
                ACTION="<c:url value='/UploadSpreadsheet'/>"
                METHOD="POST" ENCTYPE="multipart/form-data">
                <c:if test="${teamsUploaded }">
                    <span class='completed'>DONE </span>
                </c:if>
                Upload the datafile for teams. This file can be tab
                separated or comma separated or an Excel file (xls and
                xlsx are supported).
                <input type="file" size="32" id='teams_file' name="file">
                <input type='hidden' name='uploadRedirect'
                    value="<c:url value='/admin/UploadTeams'/>" />
                <input type="submit" id='upload_teams' value="Upload">
                <a href='javascript:display("UploadTeamsHelp")'>[help]</a>
                <div id='UploadTeamsHelp' class='help'
                    style='display: none'>
                    Each column of the input file needs to matched
                    against the required data for teams. The column
                    names are the first row of your data file. This
                    information includes: team number, team name,
                    organization, initial tournament, award group,
                    judging group. The team number must be a number and
                    is required. The other columns are not required, but
                    are a good idea to include. You will be prompted to
                    pick a column from your data file to match against
                    each piece of team data that the software uses. You
                    can select the same column for multiple pieces of
                    data. <a href='javascript:hide("UploadTeamsHelp")'>[hide]</a>
                </div>

            </form>
        </li>

        <li>
            <a href='editTeam.jsp'>Add a team</a> <a
                href='javascript:display("AddTeamHelp")'>[help]</a>
            <div id='AddTeamHelp' class='help' style='display: none'>
                This can be used to add a team to the software that
                wasn't added through the team upload. This may be used
                for small tournaments where all of the teams aren't
                known in advance or a team shows up for a tournament at
                the last minute. Special care needs to be taken when
                adding a team to an already running tournament to ensure
                that they have a schedule for where to go when and that
                they get judged properly. In most cases this function
                should not be used at the tournament, but rather before
                the tournament. <a href='javascript:hide("AddTeamHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a id='add-edit-levels'
                href='<c:url value="edit_levels.jsp"/>'>Add or Edit
                Tournament Levels</a> <a
                href='javascript:display("EditTournamentLevelHelp")'>[help]</a>
            <div id='EditTournamentLevelHelp' class='help'
                style='display: none'>
                This is an optional step. Use this page to modify the
                levels that tournaments are assigned to.
                <br>
                <a href='javascript:hide("EditTournamentLevelHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a id='add-edit-tournaments'
                href='<c:url value="tournaments.jsp"/>'>Add or Edit
                Tournaments</a> <a
                href='javascript:display("EditTournamentHelp")'>[help]</a>
            <div id='EditTournamentHelp' class='help'
                style='display: none'>
                This is an optional step. Use this page to modify the
                tournaments created by team upload step above, or to
                create new tournaments.
                <br>
                <a href='javascript:hide("EditTournamentHelp")'>[hide]</a>
            </div>
        </li>


        <li>
            <a href='DisplayTournamentAssignments'>Display
                Tournament Assignments</a> <a
                href='javascript:display("DisplayTournamentAssignmentsHelp")'>[help]</a>
            <div id='DisplayTournamentAssignmentsHelp' class='help'
                style='display: none'>
                This is an optional step. Use this page to display what
                teams are assigned to what tournament and what judging
                groups they are in.
                <br>
                <a
                    href='javascript:hide("DisplayTournamentAssignmentsHelp")'>[hide]</a>
            </div>
        </li>


        <li>
            Current tournament is ${currentTournament.description} on
            ${currentTournament.dateString} [${currentTournament.name}].
            <form id='currentTournament' action='SetCurrentTournament'
                method="post">
                Change tournament to
                <select id='currentTournamentSelect'
                    name='currentTournament'>
                    <c:forEach items="${tournaments }" var="tournament">
                        <c:choose>
                            <c:when
                                test="${tournament.tournamentID == currentTournament.tournamentID}">
                                <option selected
                                    value='${tournament.tournamentID }'>${tournament.dateString}
                                    - ${tournament.description} [
                                    ${tournament.name } ]</option>
                            </c:when>
                            <c:otherwise>
                                <option
                                    value='${tournament.tournamentID }'>${tournament.dateString}
                                    - ${tournament.description} [
                                    ${tournament.name } ]</option>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </select>
                <input type='submit' name='change_tournament'
                    value='Submit'>
            </form>
        </li>

        <li>
            <a href="edit_tournament_parameters.jsp">Edit tournament
                parameters</a> - should not be needed for most tournaments.
        </li>

        <li>
            <a
                href="<c:url value='/report/awards/edit-categories-awarded.jsp'/>">Specify
                which categories are awarded by tournament level</a>
        </li>

        <li>
            <c:if test="${scheduleUploaded }">
                <span class='completed'>DONE </span>
            </c:if>
            Upload schedule for current tournament. <a
                href='javascript:display("ScheduleHelp")'>[help]</a>
            <div id='ScheduleHelp' class='help' style='display: none'>
                Uploading the schedule isn't required, but if uploaded
                will be used when displaying information in final
                reports. Teams can be added directly to the current
                tournament through a schedule as well. Column names are
                not important, you will be given the opportunity to map
                the column names to the fields that the software is
                expecting. A description of the <a
                    href="schedule-format.jsp" target="_blank">schedule
                    format</a> is available. <a
                    href='javascript:hide("ScheduleHelp")'>[hide]</a>
            </div>

            <form id='uploadSchedule'
                action='<c:url value="/UploadSpreadsheet"/>'
                METHOD="POST" ENCTYPE="multipart/form-data">
                <input type="file" size="32" id='scheduleFile'
                    name="file" />
                <input type='hidden' name='uploadRedirect'
                    value="<c:url value='/schedule/CheckPerformanceScoresExist'/>" />
                <input id="upload-schedule" type="submit"
                    value="Upload Schedule" />
            </form>

            <c:if test="${scheduleUploaded }">
                <!--  downloads for the schedule -->
                <ul>
                    <li>
                        <a href="<c:url value='/admin/ScheduleByTeam'/>"
                            target="_blank">Full schedule sorted by
                            team</a>
                    </li>
                    <li>
                        <a
                            href="<c:url value='/admin/ScheduleByWaveAndTeam'/>"
                            target="_blank">Full schedule sorted by
                            wave and team</a>
                    </li>
                    <li>
                        <a href="SubjectiveScheduleByJudgingStation"
                            target="_blank">Subjective schedule
                            split by category and sorted by judging
                            group, then time</a>
                    </li>
                    <li>
                        <a
                            href="<c:url value='/admin/SubjectiveScheduleByCategory'/>"
                            target="_blank">Judging Schedule by
                            Category</a>
                    </li>
                    <li>
                        <a href="SubjectiveScheduleByTime"
                            target="_blank">Judging Schedule</a>
                    </li>
                    <li>
                        <a href="PerformanceSchedule" target="_blank">Performance
                            Schedule</a>
                    </li>
                    <li>
                        <a href="PerformanceSchedulePerTable"
                            target="_blank">Performance Schedule per
                            table</a>
                    </li>
                    <li>
                        <a href="PerformanceNotes" target="_blank">Performance
                            Schedule per table for notes</a>
                    </li>
                    <li>
                        <a href="<c:url value='/admin/ScheduleAsCsv' />"
                            target="_blank">Full Schedule as CSV</a> -
                        for use with scheduling tools
                    </li>

                    <li>
                        <a
                            href="<c:url value='/admin/PerformanceSheets' />"
                            target="_blank">Performance sheets for
                            regular match play</a>
                    </li>

                    <!-- subjective sheets -->
                    <c:forEach
                        items="${challengeDescription.subjectiveCategories}"
                        var="category">

                        <c:set var="columns"
                            value="${categoryNameToColumn[category.name]}" />

                        <c:forEach items="${columns}" var="columnName">

                            <c:choose>
                                <c:when test="${columns.size() gt 1}">
                                    <li>
                                        <a
                                            target="_${category.name}-${columnName}"
                                            href="<c:url value='SubjectiveSheets/${category.name}/${columnName}'/>">Subjective
                                            sheets for ${category.title}
                                            schedule ${columnName}</a>
                                    </li>
                                </c:when>
                                <c:otherwise>
                                    <li>
                                        <a target="_${category.name}"
                                            href="<c:url value='SubjectiveSheets/${category.name}/${columnName}'/>">Subjective
                                            sheets for ${category.title}</a>
                                    </li>
                                </c:otherwise>
                            </c:choose>
                        </c:forEach>

                    </c:forEach>
                    <!-- end subjective sheets -->

                    <!-- Team schedules -->
                    <li>
                        <a href="<c:url value='/admin/TeamSchedules' />"
                            target="_blank">Team Schedules</a>
                    </li>
                    <li>
                        <form
                            action="<c:url value='/admin/TeamSchedules' />"
                            method="post">
                            Team schedule for
                            <select name='TeamNumber'>
                                <c:forEach items="${tournamentTeams}"
                                    var="team">
                                    <option
                                        value='<c:out value="${team.teamNumber}"/>'>
                                        <c:out
                                            value="${team.teamNumber}" />
                                        -
                                        <c:out value="${team.teamName}" />
                                    </option>
                                </c:forEach>
                            </select>
                            <input type='submit' value='Output Schedule' />
                        </form>
                    </li>
                    <!-- end Team schedules -->

                    <li>
                        <button type='button' id='schedule_download'>Download
                            Common Schedules</button>
                    </li>
                </ul>
            </c:if>
            <!-- end schedule loaded -->
        </li>

        <li>
            <a href="<c:url value='/report/PitSigns' />" target="_blank">All
                Pit Signs</a>
        </li>

        <li>
            <form action="<c:url value='/report/PitSigns' />"
                method='post' target="_blank">
                Pit sign for
                <select name='team_number'>
                    <c:forEach items="${tournamentTeams}" var="team">
                        <option
                            value='<c:out value="${team.teamNumber}"/>'>
                            <c:out value="${team.teamNumber}" /> -
                            <c:out value="${team.teamName}" />
                        </option>
                    </c:forEach>
                </select>
                <input type='submit' value='Output Pit Sign' />
            </form>
        </li>

        <li>
            <a id='change-award-groups' href='edit_event_division.jsp'>Change
                award group assignments for the current tournament</a><a
                href='javascript:display("EventDivisionHelp")'>[help]</a>
            <div id='EventDivisionHelp' class='help'
                style='display: none'>
                This information is typically specified in the schedule
                data file. If it needs to be modified you can use this
                page to change the award groups for teams in the current
                tournament.
                <br>
                <a href='javascript:hide("EventDivisionHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a href='edit_judging_groups.jsp'>Change judging groups
                assignments for the current tournament</a><a
                href='javascript:display("JudgingGroupHelp")'>[help]</a>
            <div id='JudgingGroupHelp' class='help'
                style='display: none'>
                This information is typically specified in the schedule
                data file. If it needs to be modified you can use this
                page to change the judging groups for teams in the
                current tournament.
                <br>
                <a href='javascript:hide("JudgingGroupHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a href="<c:url value='/admin/edit_run_metadata.jsp'/>">Edit
                Performance Run Data</a>
        </li>

        <li>
            <a href="<c:url value='/admin/delayed_performance.jsp'/>">Setup
                delay of displaying performance scores</a>
        </li>

        <li>
            <a href="manage-user-images.jsp" target="_blank">Modify
                the various images used in the software</a>
        </li>

        <li>
            <a href="manage-sponsor-logos.jsp" target="_blank">Add
                or delete sponsor logos</a>
        </li>

        <li>
            <a href="manage-slideshow.jsp" target="_blank">Add or
                delete slideshow images</a>
        </li>

        <li>
            <a href="database-backups.jsp" target="_blank">Access to
                database backups</a>
        </li>
    </ol>

    <h2>Tournament day</h2>
    <ol>

        <li>
            <c:if test="${judgesAssigned }">
                <span class='completed'>DONE </span>
            </c:if>
            <a href='<c:url value="GatherJudgeInformation"/>'
                id='assign_judges'>Assign Judges</a>
        </li>


        <li>
            <c:if test="${tablesAssigned}">
                <span class='completed'>DONE </span>
            </c:if>
            <a href='<c:url value="tables.jsp"/>'>Assign Table
                Labels</a>
        </li>



        <li>
            <a href='<c:url value="edit_select_team.jsp"/>'>Edit
                team data</a>
        </li>


        <li>
            <a id='remote-control' href='remoteControl.jsp'>Remote
                control of display</a>
        </li>

        <li>
            <h3>Subjective Scoring</h3>
            <ul>
                <li>
                    <a href='<c:url value="/subjective/index.html"/>'
                        target="_blank">Subjective Web application</a>
                </li>

                <li>
                    2 server actions
                    <ul>
                        <li>
                            <a
                                href="CheckSubjectiveEmptyForPerformanceExport">Export
                                performance data for judges server</a> . If
                            doing finalist scheduling, the initial head
                            to head brackets need to be created before
                            doing this export.
                        </li>

                        <li>
                            <form name="import-performance"
                                action="ProcessImportPerformance"
                                method="POST"
                                ENCTYPE="multipart/form-data">
                                Specify the file that was exported from
                                the performance server
                                <input type="file" size="32"
                                    name="performanceFile" />

                                <!-- performance file upload button -->
                                <input id='uploadPerformanceData'
                                    type="submit" value="Upload" />
                            </form>
                        </li>


                        <li>
                            <form name="import-finalist"
                                action="ProcessImportFinalist"
                                method="POST"
                                ENCTYPE="multipart/form-data">
                                Specify the file that was exported from
                                the judges server with the finalist data
                                <input type="file" size="32"
                                    name="finalistFile" />

                                <!-- performance file upload button -->
                                <input id='uploadFinalistData'
                                    type="submit" value="Upload" />
                            </form>
                        </li>

                    </ul>
                </li>
                <!--  end 2 server actions -->


            </ul>
        </li>

        <li>
            Once regular match play has been completed you will need to
            setup the <a href="../playoff" target="_blank">head to
                head brackets</a>
        </li>

    </ol>

    <h2>After the tournament</h2>
    <ul>
        <li>
            <a href='database.flldb'>Download database</a>
        </li>
    </ul>

    <h2>User Management</h2>
    <ul>
        <li>
            <a href="changePassword.jsp">Change Password</a>
        </li>
        <li>
            <a href="createUsername.jsp">Create User</a>
        </li>

        <li>
            <a href="edit-roles.jsp">Edit user roles</a>
        </li>
        <li>
            <a href="removeUser.jsp">Remove User</a>
        </li>
        <li>
            <a href="unlockUserAccount.jsp">Unlock User account</a>
        </li>
    </ul>

    <h2>Advanced</h2>
    <p>These links are for advanced users.</p>
    <ul>
        <li>
            <a href="edit_global_parameters.jsp">Edit global
                parameters</a>
        </li>

        <li>
            <form id='uploadTeamTournamentAssignments'
                ACTION="<c:url value='/UploadSpreadsheet'/>"
                METHOD="POST" ENCTYPE="multipart/form-data">
                Upload CSV or Excel of teams and tournaments to assign
                them to
                <input type="file" size="32" name="file" />
                <input type='hidden' name='uploadRedirect'
                    value="<c:url value='/admin/chooseAdvancementColumns.jsp'/>" />
                <input type="submit" value="Upload" />
            </form>
        </li>

        <li>
            <form id='uploadTeamInformation'
                ACTION="<c:url value='/UploadSpreadsheet'/>"
                METHOD="POST" ENCTYPE="multipart/form-data">
                Upload CSV or Excel of updated information for teams
                <input type="file" size="32" name="file" />
                <input type='hidden' name='uploadRedirect'
                    value="<c:url value='/admin/chooseTeamInformationColumns.jsp'/>" />
                <input type="submit" value="Upload" />
            </form>
        </li>

        <li>
            <form id='import'
                action="<c:url value='/developer/importdb/ImportDBDump'/>"
                method='post' enctype='multipart/form-data'>

                <p>Merge another database into the current database.</p>
                <input type='file' size='32' name='dbdump' />
                <input type='submit' name='importdb'
                    value='Import Database' />
            </form>
        </li>

    </ul>

</body>
</html>

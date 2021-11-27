<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Scoring Coordinator</title>
<link rel="stylesheet" type="text/css"
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

  function confirm_subjective() {
    var overwrite = document.getElementById('subjective_overwrite_all').checked;
    if (overwrite) {
      return confirm("Are you sure you want to overwrite all subjective scores?")
    } else {
      return true;
    }
  }
</script>
</head>

<body>
    <h1>Scoring Coordinator - ${challengeDescription.title }</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>This page contains links to the pages used in the performance
        area when a 2 server setup is used. This page assumes that you
        have been given a pre-populated database to load. Most links
        open up new tabs so that this page can be used to follow the
        workflow.</p>

    <p>
        The current tournament is
        <b>${tournament.description} on ${tournament.dateString}
            [${tournament.name}]</b>
    </p>

    <h2>Server addresses</h2>
    <p>These are the addresses that can be used on the judges
        electronic devices to connect to this server.</p>
    <ul>
        <c:forEach items="${urls}" var="url">
            <li>
                <a href="${url }">${url }</a>
            </li>
        </c:forEach>
    </ul>

    <h2>Tournament steps</h2>
    <ol>

        <li>
            <a target="_init" href="<c:url value='/setup' />">Database
                setup</a> - Ask the Lead Scoring Coordinator if the database
            is already added to the server. If not, click Database
            setup. Do this the night before the tournament, or the first
            thing the day of the tournament.
            <ul>
                <li>When setting up the database you will be
                    prompted to create a username and password. Pick
                    something that isn't going to be obvious to those at
                    the tournament and is easy to type into a tablet.
                    This keeps non-officials from being able to change
                    scores if they get onto the network that the server
                    is on.</li>
                <li>If the database was already setup, make sure to
                    get the username and password from the person that
                    did the setup. If you cannot get this information,
                    either run database setup or use the Create User
                    link on the Admin Index from the server computer.</li>
            </ul>
        </li>

        <li>
            <form id='currentTournament'
                action="<c:url value='/admin/SetCurrentTournament' />"
                method="post">
                Change tournament to the correct tournament and click
                Submit.
                <select id='currentTournamentSelect'
                    name='currentTournament'>
                    <c:forEach items="${tournaments }" var="tournament">
                        <c:choose>
                            <c:when
                                test="${tournament.tournamentID == currentTournament.tournamentID}">
                                <option selected
                                    value='${tournament.tournamentID }'>${tournament.description}
                                    [ ${tournament.name } ]</option>
                            </c:when>
                            <c:otherwise>
                                <option
                                    value='${tournament.tournamentID }'>${tournament.description}
                                    [ ${tournament.name } ]</option>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>
                </select>
                <input type='submit' name='change_tournament'
                    value='Submit'>
            </form>
        </li>

        <li>Test the printer by printing something useful. See
            Other Useful Tasks below.</li>

        <li>
            Set up Scoring Computers
            <ol>
                <li>Connect to the correct FLL wireless network</li>
                <li>Open Chrome (or other browser if Chrome is not
                    installed)</li>
                <li>Put server address in the address bar and hit
                    enter. This address is provided under the section
                    Server Addresses on the Scoring Coordinator page.</li>
                <li>Enter the username and password specified above
                    when running database setup.</li>
            </ol>
        </li>

        <li>
            Set up the display computer and projector
            <ol>
                <li>Connect to the correct FLL wireless network</li>
                <li>Open Chrome (or other browser if Chrome is not
                    installed)</li>
                <li>Put server address in the address bar and hit
                    enter. This address is provided under the section
                    Server Addresses on the Scoring Coordinator page.</li>
                <li>If you have more than one display, then name
                    the display computer. It is useful to use "Left" and
                    "Right" or "Lakes" and "Woods" depending on how you
                    will be using the additional screens.</li>
                <li>After clicking Submit, Alt-Tab to the new
                    browser window that was opened.</li>
                <li>Press F11 to make the window full screen,
                    hiding the address bar and buttons. You may need to
                    move the mouse pointer out of the way as well.</li>
            </ol>
        </li>

        <li>
            On the server click on <a target="_remote_control"
                id='remote-control' href='remoteControl.jsp'>Control
                the Remote display</a>. This is used to control what is
            displayed on the screens
        </li>

        <li>
            <form
                ACTION="<c:url value='/report/performanceRunReport.jsp' />"
                METHOD='POST'>
                Check that the right number of performance scores have
                been entered
                <select name='RunNumber'>
                    <c:forEach var="index" begin="1"
                        end="${maxRunNumber}">
                        <option value='${index }'>${index }</option>
                    </c:forEach>
                </select>
                <input type='submit' value='Show Scores for run' />
            </form>
        </li>

        <li>
            <a href="CheckSubjectiveEmptyForPerformanceExport">Export
                performance data for judges server</a>. Save this file to
            the Red PERF Data flash drive and deliver to the judges
            room.
            <ul>
                <li>
                    At
                    <b>Regionals</b>
                    : This is done between the 3 performance rounds and
                    the Just For Fun round.
                </li>
                <li>
                    At
                    <b>Sectionals</b>
                    : This is done between the regular match play and
                    Head to Head.
                </li>
                <li>
                    A
                    <b>State</b>
                    : This is done between regular match play and Head
                    to Head. The initial head to head brackets need to
                    be created before doing this export.
                </li>
            </ul>
        </li>


        <li>
            Import performance data into the judges server.
            <form name="import-performance"
                action="<c:url value='/admin/ProcessImportPerformance' />"
                target="_import_performance" method="POST"
                ENCTYPE="multipart/form-data">
                Specify the file that was exported from the performance
                server
                <input type="file" size="32" name="performanceFile" />

                <!-- performance file upload button -->
                <input id='uploadPerformanceData' type="submit"
                    value="Upload" />
            </form>
        </li>

        <li>
            Load an offline datafile from the subjective application.
            Use this when synchronize isn't working.
            <form name="subjective-offline"
                action="<c:url value='/admin/SubjectiveOffline'/>"
                method="POST" ENCTYPE="multipart/form-data">

                <input type="file" size="32"
                    name="subjectiveOfflineFile" />

                <input id='subjectiveOfflineData' type="submit"
                    value="Upload" />
            </form>
        </li>

        <li>
            <a href="<c:url value='/admin/DownloadJudgesDatabase' />">Download
                the final judges room database</a> and send it to the head
            computer person
        </li>

        <li>
            <a href="<c:url value='/admin/ExportFinalistData' />">Export
                finalist data</a> to bring to the performance computer
        </li>


        <li>
            <a href='database.flldb'>Download the final database</a> -
            send this to the lead Scoring Coordinator
        </li>

    </ol>

    <h2>Sectionals and State</h2>
    Sections and State run a Head to Head competition after regular
    match play.

    <ol>
        <li>
            Once regular match play has been completed you will need to
            setup the <a href="<c:url value='/playoff' />" target="_h2h">head
                to head brackets</a>.
        </li>


        <li>
            <b>State Only</b>
            <form name="import-finalist" action="ProcessImportFinalist"
                method="POST" ENCTYPE="multipart/form-data">
                Specify the file that was exported from the judges
                server with the finalist data
                <input type="file" size="32" name="finalistFile" />

                <!-- performance file upload button -->
                <input id='uploadFinalistData' type="submit"
                    value="Upload" />
            </form>
        </li>

        <li>
            Print the <a href='<c:url value="/report/PlayoffReport" />'
                target="_blank">Winners of each head to head bracket</a>.
            This is needed for the awards ceremony.
        </li>

        <li>
            <a href='database.flldb'>Download the final database</a> -
            send this to the lead computer person
        </li>

    </ol>

    <h2>Other useful pages</h2>
    <ul>
        <li>
            <form action="<c:url value='ChangeScorePageText' />"
                method='post'>
                Scoring display text:
                <input type='text' name='ScorePageText'
                    value='<c:out value="${ScorePageText}"/>'>
                <input type='submit' value='Change text'>
                <a href='javascript:display("ScorePageTextHelp")'>[help]</a>
                <div id='ScorePageTextHelp' class='help'
                    style='display: none'>
                    This text is displayed on the various big screen
                    display pages. There is only 1 or 2 lines of space
                    available, so keep it short. This can be used to
                    notify participants and spectators of when the next
                    break will be over.<a
                        href='javascript:hide("ScorePageTextHelp")'>[hide]</a>
                </div>
            </form>
        </li>

        <li>
            <a target="_edit_teams"
                href="<c:url value='/admin/select_team.jsp' />">Edit
                team data</a> - this is where to go for team name and
            organization changes
        </li>

        <li>
            <a href="<c:url value='/admin/ScheduleByTeam' />"
                target="_new">Full schedule sorted by team</a>
        </li>

        <li>
            <a
                href="<c:url value='/admin/SubjectiveScheduleByJudgingStation' />"
                target="_new">Subjective schedule sorted by judging
                group, then time</a>
        </li>

        <li>
            <a href="<c:url value='/admin/SubjectiveScheduleByTime' />"
                target="_new">Subjective schedule sorted by time</a>
        </li>

        <li>
            <a href="<c:url value='/admin/PerformanceSchedule' />"
                target="_new">Performance Schedule</a>
        </li>
    </ul>
</body>
</html>

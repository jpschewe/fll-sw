<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.MainIndex.populateContext(request, application, pageContext);
fll.web.admin.AdminIndex.populateContext(application, session, pageContext);
fll.web.report.ReportIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Judges room</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

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
    <h1>Judges room for ${challengeDescription.title }</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>This page contains links to the pages used in the judges
        room. Most links open new tabs so that you can continue to
        follow the workflow on this page.</p>

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
                setup</a> - Initialize the database first thing in the
            morning or the night before. This has probably already been
            done.
        </li>

        <li>
            Make sure that the correct tournament is selected.
            <ul>
                <li>The current tournament is
                    ${currentTournament.description }.</li>
                <li>
                    <form id='currentTournament'
                        action="<c:url value='/admin/SetCurrentTournament' />"
                        method="post">
                        Change tournament to
                        <select id='currentTournamentSelect'
                            name='currentTournament'>
                            <c:forEach items="${tournaments }"
                                var="tournament">
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

            </ul>
        </li>

        <li>
            <a target="_edit_parameters"
                href="<c:url value='/admin/edit_tournament_parameters.jsp'/>">Edit
                tournament parameters</a> - should not be needed for most
            tournaments.
        </li>


        <li>
            Enter subjective scores. This is done through the <a
                target="_subjective"
                href="<c:url value='/subjective/Auth' />">subjective
                web application</a>.
        </li>


        <li>
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
            <a href="<c:url value='/report/edit-award-winners.jsp' />"
                target="_blank">Enter the winners of awards for use
                in the awards report</a>
        </li>

        <li>
            <a href="<c:url value='/report/AwardsReport' />"
                target="_blank">Report of winners for the
                tournament.</a> This can be published on the web or used for
            the awards ceremony.
        </li>

        <li>
            <a target="_report"
                href="<c:url value='/report/index.jsp' />">Generate
                reports</a> - this is done once all of the subjective scores
            are in. <a href='javascript:display("ReportHelp")'>[help]</a>
            <div id='ReportHelp' class='help' style='display: none'>
                It is best to not generate the reports until all scores
                are in. However one can generate reports early to see
                the results of one award group. You just need to be
                aware that the reports for the unfinished award group
                will not be accurate. <a
                    href='javascript:hide("ReportHelp")'>[hide]</a>
            </div>
        </li>

        <li>
            <a href="<c:url value='/admin/DownloadJudgesDatabase' />">Download
                the final judges room database</a> and send it to the head
            computer person
        </li>

    </ol>

    <h2>Finalist scheduling</h2>
    <p>This is used at tournaments where there is more than 1
        judging group in an award group. This is typically the case at a
        state tournament where all teams are competing for first place
        in each category, but there are too many teams for one judge to
        see.</p>

    <p>Before using these links the initial head to head brackets
        need to be assigned in the performance area and the performance
        dump needs to be imported using the link above.</p>

    <ul>

        <li>
            <a href="<c:url value='/report/non-numeric-nominees.jsp' />"
                target="_blank">Enter non-numeric nominees</a>. This is
            used to enter the teams that are up for consideration for
            the non-scored subjective categories. This information
            transfers over to the finalist scheduling web application.
            This is also used in the awards scripts report.
        </li>

        <li>
            <a href="<c:url value='/report/finalist/load.jsp' />"
                target="_blank">Schedule Finalists</a>. Before visiting
            this page, all subjective scores need to be uploaded and any
            head to head brackets that will occur during the finalist
            judging should be created to avoid scheduling conflicts.
        </li>

        <li>
            <form
                ACTION="<c:url value='/report/finalist/PdfFinalistSchedule' />"
                METHOD='POST' target="_blank">
                <select name='division'>
                    <c:forEach var="division"
                        items="${finalistDivisions }">
                        <option value='${division }'>${division }</option>
                    </c:forEach>
                </select>
                <input type='submit' value='Finalist Schedule (PDF)' />
            </form>
        </li>

        <li>
            <a
                href="<c:url value='/report/finalist/TeamFinalistSchedule' />"
                target="_blank">Finalist Schedule for each team</a>
        </li>

        <li>
            <a href="<c:url value='/admin/ExportFinalistData' />">Export
                finalist data</a> to bring to the performance computer
        </li>

    </ul>


</body>
</html>

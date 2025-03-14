<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.EditRunMetadata.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Run Metadata</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type='text/javascript'
    src='<c:url value="/js/fll-functions.js" />'></script>

<script type='text/javascript' src='edit_run_metadata.js'></script>

</head>

<body>

    <div class='content'>

        <h1>Edit Run Metadata</h1>

        <div class='status-message'>${message}</div>
        <%-- clear out the message, so that we don't see it again --%>
        <c:remove var="message" />

        <p>Editing performance run information for
            ${tournamentData.currentTournament.description}.</p>
        <p>All runs must have a display name. Runs cannot be deleted
            if they have performance scores, are in the schedule, are
            not the last known run.</p>

        <!-- ${canDelete} -->
        <form name='edit_run_metadata' id='edit_run_metadata'
            action='EditRunMetadata' method='POST'>

            <table class='center' id='run_metadata_table'>
                <tr>
                    <th>Run</th>
                    <th>Display Name</th>
                    <th>Regular Match Play</th>
                    <th>Display on Scoreboard</th>
                    <th>Delete</th>
                </tr>

                <c:forEach items="${allRunMetadata}" var="runMetadata">
                    <tr>
                        <td>${runMetadata.runNumber}</td>
                        <td>
                            <input type='text'
                                name='${runMetadata.runNumber}_name'
                                id='${runMetadata.runNumber}_name'
                                value='${runMetadata.displayName}' />
                        </td>
                        <td>
                            <c:choose>
                                <c:when
                                    test="${runMetadata.regularMatchPlay}">
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_regularMatchPlay'
                                        id='${runMetadata.runNumber}_regularMatchPlay'
                                        checked />
                                </c:when>
                                <c:otherwise>
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_regularMatchPlay'
                                        id='${runMetadata.runNumber}_regularMatchPlay' />
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when
                                    test="${runMetadata.scoreboardDisplay}">
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_scoreboardDisplay'
                                        id='${runMetadata.runNumber}_scoreboardDisplay'
                                        checked />
                                </c:when>
                                <c:otherwise>
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_scoreboardDisplay'
                                        id='${runMetadata.runNumber}_scoreboardDisplay' />
                                </c:otherwise>
                            </c:choose>
                        </td>

                        <td>
                            <c:choose>
                                <c:when
                                    test='${canDelete[runMetadata.runNumber]}'>
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_delete'
                                        id='${runMetadata.runNumber}_delete' />
                                </c:when>
                                <c:otherwise>
                                    <input type='checkbox'
                                        name='${runMetadata.runNumber}_delete'
                                        id='${runMetadata.runNumber}_delete'
                                        disabled />
                                </c:otherwise>
                            </c:choose>
                            <input type='hidden'
                                name='${runMetadata.runNumber}_head2head'
                                value='${runMetadata.headToHead}' />
                        </td>
                    </tr>
                </c:forEach>

            </table>

            <button type='button' id='addRow'>Add Row</button>

            <input type="submit" id='submit_data' name='submit_data'
                value='Submit' />

        </form>
    </div>
</body>
</html>
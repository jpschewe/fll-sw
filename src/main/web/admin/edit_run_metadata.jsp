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

</head>

<body>

    <div class='content'>

        <h1>Edit Run Metadata</h1>

        <div class='status-message'>${message}</div>
        <%-- clear out the message, so that we don't see it again --%>
        <c:remove var="message" />

        <div>Editing performance run information for
            ${tournamentData.currentTournament.description}</div>

        <!-- ${canDelete} -->
        <form name='edit_run_metadata' id='edit_run_metadata'
            action='EditRunMetadata' method='POST'>

            <table class='center'>
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
                    </tr>
                </c:forEach>

            </table>


            <input type="submit" id='submit_data' name='submit_data'
                value='Submit' />

        </form>
    </div>
</body>
</html>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Add missing teams</title>
</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="promptCreateMissingTeams" action="AddMissingTeams">

        <p>The following teams are in the source database and not in
            the destination database. OK to add teams?</p>

        <table border='1'>
            <tr>
                <th>Team Number</th>
                <th>Team Name</th>
            </tr>
            <c:forEach items="${importDbSessionInfo.missingTeams}"
                var="team">
                <tr>
                    <td>${team.teamNumber}</td>
                    <td>${team.teamName}</td>
                </tr>
            </c:forEach>
        </table>

        <input name='submit_data' type='submit' value="Add teams" />
    </form>

    <p>
        Return to the <a href="selectTournament.jsp">tournament
            selection page</a> if you do not want to add the teams.
    </p>


</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Resolve Team information differences</title>
</head>

<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="resolveTeamDifferences" action="CommitTeamChanges">
        <p>There are differences in the team information between the
            source (imported) database and the destination database. You
            need to choose to accept the value from the source database
            or the value from the destination database.</p>

        <c:forEach items="${importDbSessionInfo.teamDifferences}"
            var="difference" varStatus="loopStatus">
            <p>
                The ${difference.property} for team
                ${difference.teamNumber} differs
                <br />
                <input type='radio' name='${loopStatus.index}'
                    value='source' checked />
                Use source value of '${difference.sourceValue}'
                <br />
                <input type='radio' name='${loopStatus.index}'
                    value='dest' />
                Use dest value of '${difference.destValue}'
                <br />
            </p>
        </c:forEach>


        <input name='submit_data' type='submit' value="Apply Changes" />
    </form>

    <p>
        Return to the <a href="selectTournament.jsp">tournament
            selection page</a>.
    </p>


</body>
</html>

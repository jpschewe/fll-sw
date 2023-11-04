<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Create tournament</title>
</head>

<body>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form name="createTournament" action="CreateTournament">
        <p>The tournament '${importDbSessionInfo.tournamentName}'
            does not exist in the destination database, create it?</p>

        <input name='submit_data' type='submit' value="Yes" />
        <input name='submit_data' type='submit' value="No" />
    </form>


</body>
</html>

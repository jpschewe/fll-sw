<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="REF" allowSetup="false" />

<%
fll.web.report.RegularMatchPlayRuns.populateContext(application, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<style>
table#perf-data {
    border-collapse: collapse;
}

table#perf-data, table#perf-data th, table#perf-data td {
    border: 1px solid black;
}

table#perf-data th, table#perf-data td {
    padding-right: 5px;
    padding-left: 5px;
}
</style>

<title>Regular Match Play Runs</title>
</head>

<body>
    <h1>Regular Match Play Runs</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <table id="perf-data" class="center">

        <tr>
            <th>Team Number</th>

            <c:forEach begin="1" end="${maxScoresPerTeam}"
                varStatus="loopStatus">
                <th>${loopStatus.current}</th>
            </c:forEach>
        </tr>

        <c:forEach items="${data}" var="entry">
            <tr>

                <td>${entry.key}</td>

                <c:forEach items="${entry.value}" var="score">
                    <td>${score}</td>
                </c:forEach>
            </tr>
        </c:forEach>

    </table>

</body>
</html>


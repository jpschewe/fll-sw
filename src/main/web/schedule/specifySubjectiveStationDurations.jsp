<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.SpecifySubjectiveStationDurations.populateContext(session, pageContext);
%>

<html>
<head>
<title>Specify Subjective Station Durations (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</script>

</head>

<body>
    <h1>Specify Subjective Station Durations (Upload Schedule)</h1>

    <p>Specify the amount of time allocated for each subjective
        judging station.</p>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="specify_durations" id="specify_durations" method='POST'
        action="<c:url value='/schedule/SpecifySubjectiveStationDurations'/>">

        <table border='1'>

            <tr>
                <th>Judging Station</th>
                <th>Duration (minutes)</th>
            </tr>


            <c:forEach items="${subjectiveStations}" var="station">
                <tr>
                    <td>${station}</td>

                    <td>
                        <input type="number" name="${station}:duration"
                            id="${station}:duration"
                            value="${default_duration}" min="1" required />
                    </td>
            </c:forEach>

        </table>

        <input type="submit" id='submit_data' value="Submit" />
    </form>

</body>
</html>
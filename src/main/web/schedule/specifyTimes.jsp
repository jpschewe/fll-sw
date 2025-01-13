<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.SpecifyTimes.populateContext(session, pageContext);
%>

<html>
<head>
<title>Specify Times (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>
    <%@ include file="/WEB-INF/jspf/message.jspf"%>

    <h1>Specify Wave Check-in times</h1>

    <p>Specify what time each wave should be checking in. This is
        used for the general schedule PDF.</p>


    <form name="specify_times" id="specify_times" method='POST'
        action="<c:url value='/schedule/SpecifyTimes'/>">

        <table border='1'>

            <tr>
                <th>Wave</th>
                <th>Check-in time</th>
            </tr>

            <c:forEach items="${waves}" var="wave">
                <tr>
                    <td>${wave}</td>
                    <td>
                        <input type="time" id="${wave}:checkin_time"
                            name="${wave}:checkin_time" />
                    </td>
                </tr>
            </c:forEach>
        </table>

        <input type="submit" id='submit_data' value="Submit" />
    </form>

</body>
</html>
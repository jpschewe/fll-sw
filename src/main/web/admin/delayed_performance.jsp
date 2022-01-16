<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.admin.StoreDelayedPerformance.populateContext(application, pageContext);
%>

<html>
<head>
<script type="text/javascript" src="delayed_performance.js"></script>

<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>">

<title>Delayed Performance Display</title>
</head>

<body>
    <p>Scores for the specified performance run number will not be
        displayed until after the specified date and time.</p>

    <p>These times only apply to the display of regular match play
        scores on the main scoreboard. They do not effect the head to
        head bracket display.</p>

    <form action="<c:url value='/admin/StoreDelayedPerformance' />"
        method="POST" name="delayed_performance"
        id="delayed_performance">

        <table border="1" id="delayedPerformanceTable">
            <tr>
                <th>Run Number</th>
                <th>Date and time</th>
            </tr>

            <c:forEach items="${delays}" var="delay"
                varStatus="loopStatus">
                <tr>
                    <td>
                        <input type="number"
                            name="runNumber${loopStatus.index}"
                            id="runNumber${loopStatus.index}"
                            value="${delay.runNumber}" min="1" size="8"
                            required />
                    </td>
                    <td>
                        <input type="datetime-local"
                            name="datetime${loopStatus.index}"
                            id="datetime${loopStatus.index}"
                            value="${delay.delayUntilDateTimeString}"
                            required />
                    </td>
                </tr>
            </c:forEach>
        </table>

        <input type="hidden" name="numRows" id="numRows"
            value="${fn:length(delays)}" />
        <!--  -->
        <button type="button" id='addRow'>Add Row</button>
        <!--  -->
        <input type='submit' name='commit' value='Finished'
            onclick='return validateData()' />
    </form>

</body>

</html>

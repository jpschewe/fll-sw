<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<html>
<head>
<title>Specify schedule constraints (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<body>
    <h1>Specify schedule constraints (Upload Schedule)</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="constraints" id="constraints" method='POST'
        action='ProcessScheduleConstraints'>
        <p>Specify the constraints to use for checking the uploaded
            schedule. If you haven't been given any specific values to
            use, just use the defaults.</p>

        <div>
            Change time duration:
            <input type="number" name="changeTimeDuration"
                id="changeTimeDuration"
                value="${uploadScheduleData.schedParams.changetimeMinutes }"
                min="0" required>
            minutes
        </div>

        <div>
            Performance change time duration:
            <input name="performanceChangeTimeDuration"
                id="performanceChangeTimeDuration"
                value="${uploadScheduleData.schedParams.performanceChangetimeMinutes }"
                type="number" min="0" required>
            minutes
        </div>

        <div>
            Performance duration:
            <input name="performanceDuration" id="performanceDuration"
                value="${uploadScheduleData.schedParams.performanceMinutes }"
                type="number" min="0" required>
            minutes
        </div>


        <input type="submit" id='submit_data' value='Submit' />
    </form>

</body>
</html>
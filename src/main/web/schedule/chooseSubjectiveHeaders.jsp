<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.schedule.ChooseSubjectiveHeaders.populateContext(pageContext);
%>

<html>
<head>
<title>Choose Subjective Headers (Upload Schedule)</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript"
    src="<c:url value='/extlib/jquery-validation/dist/jquery.validate.min.js'/>"></script>

<script type="text/javascript">
  $(document).ready(function() {
    $("#choose_headers").validate();
  });
</script>

</head>

<body>
    <h1>Choose Subjective Headers (Upload Schedule)</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <form name="choose_headers" id="choose_headers" method='POST'
        action='ProcessSubjectiveHeaders'>
        <p>Match the column names from the schedule data file with
            the subjective categories that they contain the schedule
            for. Also specify the number of minutes between judging
            sessions for each category.</p>

        <table border='1'>
            <tr>
                <th>Subjective Category</th>
                <th>Data file column name</th>
                <th>Duration (minutes)</th>
            </tr>
            <c:forEach
                items="${challengeDescription.subjectiveCategories }"
                var="subcat">
                <tr>

                    <td>${subcat.title}</td>

                    <td>
                        <select name='${subcat.name}:header'>

                            <c:forEach
                                items="${uploadScheduleData.unusedHeaders }"
                                var="subjHeader" varStatus="loopStatus">
                                <c:if
                                    test="${fn:length(subjHeader) > 0 }">
                                    <option value='${loopStatus.index }'>${subjHeader }</option>
                                </c:if>
                            </c:forEach>
                            <!-- foreach data file header -->

                        </select>
                    </td>

                    <td>
                        <input type="text"
                            name="${subcat.name}:duration"
                            id="${subcat.name}:duration"
                            value="${default_duration}"
                            class="required digits" />
                    </td>

                </tr>
                <!-- row for category -->

            </c:forEach>
            <!--  foreach category -->

        </table>

        <input type="submit" id='submit_data'
            onsubmit="return validateForm()" value="Submit" />
    </form>

</body>
</html>
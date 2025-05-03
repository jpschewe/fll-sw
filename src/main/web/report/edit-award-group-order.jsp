<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
fll.web.report.EditAwardGroupOrder.populateContext(application, pageContext);
%>

<fll-sw:required-roles roles="HEAD_JUDGE,REPORT_GENERATOR"
    allowSetup="false" />

<!DOCTYPE HTML>
<html>
<head>
<title>Edit Award Group Order</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

</head>

<body>

    <h1>Order of award groups</h1>
    <p>Specify the sort order for the award groups for display and
        printing. This is used in both the awards report and the awards
        script. Put a number next to each group name. The groups will be
        output from lowest number to highest.</p>

    <form method='POST' action='EditAwardGroupOrder'>
        <ul>
            <c:forEach items="${groups}" var="group"
                varStatus="loopStatus">
                <li>
                    <input type='number' step='1' name='${group}'
                        value='${loopStatus.index + 1}' />
                    <span>${group}</span>
                </li>
            </c:forEach>
        </ul>

        <input type='submit' value='Store Order' />
    </form>

</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.SelectHeaderRow.populateContext(request, session, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Select Header Row</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<style>
.data table, .data th, .data td {
    border: 1px solid black;
    border-collapse: collapse;
}
</style>
<body>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <p>Select the row that contains the headers</p>

    <form action="StoreColumnNames" method="POST">
        <table class="data">
            <c:forEach items="${data}" var="row" varStatus="loopStatus">
                <tr>
                    <td>
                        <input type="radio" name="headerRowIndex"
                            value="${loopStatus.index}"
                            id="headerRowIndex_${loopStatus.index}" />
                    </td>

                    <c:forEach items="${row}" var="cell">
                        <td>${cell}</td>
                    </c:forEach>
                </tr>
            </c:forEach>
        </table>

        <input type="submit" id="submit_data" value="Save header row" />
    </form>

    <form action="selectHeaderRow.jsp" method="POST">
        <input type="hidden" name="numRowsToLoad"
            value="${numRowsToLoad + 10}" />
        <input type="submit" value="Load More Data" />
    </form>

</body>
</html>
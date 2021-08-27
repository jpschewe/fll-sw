<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.report.awards.EditCategoriesAwarded.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit Categories Awarded</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<body>

    <p>Specify which categories will be awarded for tournaments at
        each level.</p>
    <form action="EditCategoriesAwarded" method="POST">

        <table border="1">
            <tr>
                <th>Category</th>
                <c:forEach items="${tournamentLevels}" var="level">
                    <th>${level.name}</th>
                </c:forEach>
            </tr>

            <c:forEach
                items="${challengeDescription.nonNumericCategories}"
                var="nonNumericCategory">

                <c:set var="categoryChecked"
                    value="${boxesChecked[nonNumericCategory]}" />
                <!-- categoryChecked: ${categoryChecked} -->

                <tr>
                    <th>${nonNumericCategory.title}</th>

                    <c:forEach items="${tournamentLevels}" var="level">
                        <c:choose>
                            <c:when test="${categoryChecked[level]}">
                                <c:set var="checkedState"
                                    value="checked" />
                            </c:when>
                            <c:otherwise>
                                <c:set var="checkedState" value="" />
                            </c:otherwise>
                        </c:choose>

                        <td>
                            <input type="checkbox" name="${level.id}"
                                value="${nonNumericCategory.title}"
                                ${checkedState} />
                        </td>
                    </c:forEach>
                </tr>

            </c:forEach>

        </table>

        <input type="submit" value="Submit" />

    </form>

</body>
</html>

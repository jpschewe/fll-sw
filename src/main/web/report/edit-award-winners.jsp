<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN,JUDGE" allowSetup="false" />

<%
fll.web.report.EditAwardWinners.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit award winners</title>

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

</head>

<body>

    <h1>Edit Award Winners</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <c:forEach items="${challengeDescription.subjectiveCategories}"
        var="category">
        <h1>${category.title}</h1>
        <c:forEach items="${awardGroups}" var="awardGroup">

            <h2>${awardGroup}</h2>

            <form action="add-award-winner.jsp" method="POST">
                <input type="hidden" name="categoryTitle"
                    value="${category.title}" />
                <input type="hidden" name="awardGroup"
                    value="${awardGroup}" />
                <input type="hidden" name="awardType" value="subjective" />
                <input type="submit" value="Add Team" />
            </form>

            <table>
                <tr>
                    <th>Place</th>
                    <th>Number</th>
                    <th>Name</th>
                    <th>Organization</th>
                    <th>Description</th>
                    <th></th>
                </tr>
                <c:forEach
                    items="${subjectiveAwardWinners[category.title][awardGroup]}"
                    var="winner">
                    <tr>
                        <td>${winner.place}</td>
                        <td>${winner.teamNumber}</td>
                        <td>${teams[winner.teamNumber].teamName}</td>
                        <td>${teams[winner.teamNumber].organization}</td>
                        <td>${winner.description}</td>
                        <td>
                            <form action="EditOrDeleteAwardWinner"
                                method="POST">
                                <input type="hidden"
                                    name="categoryTitle"
                                    value="${category.title}" />
                                <input type="hidden" name="awardGroup"
                                    value="${awardGroup}" />
                                <input type="hidden" name="teamNumber"
                                    value="${winner.teamNumber}" />
                                <div>
                                    <input type="submit" name="edit"
                                        value="Edit" />
                                </div>
                                <div>
                                    <input type="submit" name="delete"
                                        value="Delete" />
                                </div>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </table>

        </c:forEach>
        <%-- foreach award group --%>

        <%-- foreach subjective category --%>
    </c:forEach>

    <c:forEach items="${challengeDescription.nonNumericCategories}"
        var="category">
        <h1>${category.title}</h1>

        <c:choose>
            <c:when test="${category.perAwardGroup}">
    award groups
            </c:when>
            <c:otherwise>
            overall
            </c:otherwise>
        </c:choose>
    </c:forEach>


    <input type="submit" value="Store Data" />
</html>

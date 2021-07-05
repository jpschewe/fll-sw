<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN,JUDGE" allowSetup="false" />

<%
fll.web.report.EditAwardWinners.populateContext(application, pageContext);
%>

<!DOCTYPE HTML>
<html>
<head>
<title>Edit award winners</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<link rel="stylesheet" type="text/css" href="edit-award-winners.css" />

<script type='text/javascript' src="<c:url value='/js/fll-objects.js'/>"></script>
<script type='text/javascript'
    src="<c:url value='/js/fll-functions.js'/>"></script>

</head>

<body>

    <h1>Edit Award Winners</h1>

    <div class='status-message'>${message}</div>
    <%-- clear out the message, so that we don't see it again --%>
    <c:remove var="message" />

    <div id="container">
        <!-- subjective categories -->
        <c:forEach items="${challengeDescription.subjectiveCategories}"
            var="category">
            <h1>${category.title}</h1>
            <c:forEach items="${awardGroups}" var="awardGroup">

                <h2>${awardGroup}</h2>
                <c:set var="winners"
                    value="${subjectiveAwardWinners[category.title][awardGroup]}" />
                <c:set var="categoryTitle" value="${category.title}" />
                <c:set var="awardType" value="subjective" />

                <%@ include file="edit-award-winners-table.jspf"%>

            </c:forEach>
            <%-- foreach award group --%>

            <%-- foreach subjective category --%>
        </c:forEach>
        <!-- end subjective categories -->

        <c:forEach items="${challengeDescription.nonNumericCategories}"
            var="category">
            <h1>${category.title}</h1>

            <c:choose>
                <c:when test="${category.perAwardGroup}">
                    <!-- per award group award -->
                    <c:forEach items="${awardGroups}" var="awardGroup">

                        <h2>${awardGroup}</h2>
                        <c:set var="winners"
                            value="${extraAwardWinners[category.title][awardGroup]}" />
                        <c:set var="categoryTitle"
                            value="${category.title}" />
                        <c:set var="awardType" value="non-numeric" />

                        <%@ include file="edit-award-winners-table.jspf"%>

                    </c:forEach>
                    <%-- foreach award group --%>
                    <!-- end per award group award -->
                </c:when>
                <c:otherwise>
                    <c:set var="awardGroup" value="" />
                    <c:set var="winners"
                        value="${overallAwardWinners[category.title]}" />
                    <c:set var="categoryTitle" value="${category.title}" />
                    <c:set var="awardType" value="non-numeric" />

                    <%@ include file="edit-award-winners-table.jspf"%>

                    <!-- end overall award -->
                </c:otherwise>
            </c:choose>
        </c:forEach>

        <!-- championship -->
        <h1>Championship</h1>
        <c:forEach items="${awardGroups}" var="awardGroup">

            <h2>${awardGroup}</h2>

            <c:set var="winners"
                value="${extraAwardWinners['Championship'][awardGroup]}" />

            <c:set var="categoryTitle" value="Championship" />
            <c:set var="awardType" value="championship" />

            <%@ include file="edit-award-winners-table.jspf"%>

        </c:forEach>
        <%-- foreach award group --%>
        <!-- end championship -->

    </div>
</html>

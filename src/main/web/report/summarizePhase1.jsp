<%@page import="fll.web.report.SummarizePhase1"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="HEAD_JUDGE" allowSetup="false" />

<%
    SummarizePhase1.populateContext(request, application, session, pageContext);
%>

<html>
<head>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<title>Subjective Score Summary</title>

<style>
.no-judge {
    background-color: red;
}

.missing-score {
    background-color: yellow;
}

.good {
    background-color: lightgreen;
}

table {
    border-collapse: collapse;
}

div.content table, div.content th, div.content td {
    border: 1px solid black;
}
</style>
</head>

<body>
    <div class='content'>
        <h1>Subjective Score Summary</h1>

        <!--  ${ERROR} -->
        <c:choose>
            <c:when test="${not empty ERROR}">
                <p>There was an error summarizing the scores. The
                    most common problem is that there are not enough
                    scores in a judging group. Each group must have 2 or
                    more scores that are not no shows.</p>

                <p class='error'>${ERROR}</p>
            </c:when>
            <c:otherwise>

                <p>
                    Below is a list of judges found and what categories
                    they scored.<b>Note:</b> If there is no schedule
                    loaded and there are multiple judging groups for an
                    award group, then the number of teams expected will
                    be too high.
                </p>

                <c:forEach items="${judgeSummary}" var="entry">
                    <h2>${entry.key}</h2>

                    <table>
                        <tr>
                            <th>Category</th>
                            <th>Judge</th>
                            <th>Num Teams Expected</th>
                            <th>Num Teams Scored</th>
                        </tr>

                        <c:forEach items="${entry.value}"
                            var="judgeInfo">
                            <tr>
                                <td>${judgeInfo.category }</td>
                                <c:choose>
                                    <c:when
                                        test="${empty judgeInfo.judge}">
                                        <td>&nbsp;</td>
                                    </c:when>
                                    <c:otherwise>
                                        <td>${judgeInfo.judge}</td>
                                    </c:otherwise>
                                </c:choose>
                                <td>${judgeInfo.numExpected}</td>
                                <c:choose>
                                    <c:when
                                        test="${judgeInfo.numExpected == judgeInfo.numActual }">
                                        <td class='good'>${judgeInfo.numActual}</td>
                                    </c:when>
                                    <c:when
                                        test="${empty judgeInfo.judge }">
                                        <td class='no-judge'>${judgeInfo.numActual}</td>
                                    </c:when>
                                    <c:otherwise>
                                        <td class='missing-score'>${judgeInfo.numActual}</td>
                                    </c:otherwise>
                                </c:choose>
                            </tr>
                        </c:forEach>
                    </table>

                </c:forEach>

                <form action="SummarizePhase2">
                    <p>
                        If this does not look correct, go back to the <a
                            href="../index.jsp">main page and
                            correct the scores</a>. Otherwise <input
                            type='submit' value="finish" id='finish' />
                        the score summarization.
                    </p>
                </form>

            </c:otherwise>
        </c:choose>
    </div>
</body>
</html>

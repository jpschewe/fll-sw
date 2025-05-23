<%@page import="fll.xml.BracketSortType"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.playoff.BracketParameters.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Bracket Parameters</title>

<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
</head>

<script type='text/javascript'>
  function sanityCheck() {

    <c:if test="${not empty tableInfo}">
    const selectedTables = document
        .querySelectorAll("input[name='tables']:checked");
    if (selectedTables.length == 0) {
      alert('You must select at least 1 pair of tables to run the bracket on.');
      return false;
    }
    </c:if>

    return true;
  }
</script>

</head>
<body>


    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <form method="POST" action="BracketParameters">

        <p>
            Choose how the teams are sorted into the brackets. Unless
            you are doing something unusual the right answer is "<%=BracketSortType.SEEDING.getDescription()%>"
        </p>

        <select name="sort" id="sort">
            <c:forEach items="${sortOptions}" var="sortType">

                <c:choose>
                    <c:when test="${sortType == defaultSort }">
                        <option value="${sortType }" selected>${sortType.description }</option>
                    </c:when>
                    <c:otherwise>
                        <option value="${sortType }">${sortType.description }</option>
                    </c:otherwise>
                </c:choose>

            </c:forEach>
        </select>

        <div>
            <label>
                Order of teams for custom sort order. This is only used
                in special circumstances.
                <input type='text' name='custom_order' id='custom_order' />
            </label>
        </div>

        <c:if test="${not empty tableInfo}">
            <p>
                Choose which table pairs will be used for this playoff
                bracket.
                <i>If running parallel head to head runs you should
                    only select 1 table pair for each bracket</i>
                .
            </p>


            <c:forEach items="${tableInfo}" var="info">

                <label for='${info.id }'>${info.sideA } /
                    ${info.sideB }</label>

                <input type="checkbox" name='tables' value='${info.id}'
                    checked />
                <br />

            </c:forEach>
        </c:if>

        <input type='submit' id='submit_data' value='Submit'
            onclick='return sanityCheck()' />

    </form>


</body>
</html>

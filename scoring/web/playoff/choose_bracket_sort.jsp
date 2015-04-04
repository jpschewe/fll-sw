<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.playoff.ChooseBracketSort.populateContext(pageContext);
%>

<html>
<head>
<title>Choose bracket sort</title>
</head>
<body>

  <p>Choose how the teams are sorted into the brackets.</p>

  <form
    method="POST"
    action="ChooseBracketSort">

    <select
      name="sort"
      id="sort">

      <c:forEach
        items="${sortOptions}"
        var="sortType">

        <c:choose>
          <c:when test="${sortType == defaultSort }">
            <option
              value="${sortType }"
              selected>${sortType.description }</option>
          </c:when>
          <c:otherwise>
            <option value="${sortType }">${sortType.description }</option>
          </c:otherwise>
        </c:choose>

      </c:forEach>

    </select> <input
      type='submit'
      id='submit' />

  </form>


</body>
</html>

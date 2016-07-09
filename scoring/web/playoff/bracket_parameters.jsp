<%@page import="fll.xml.BracketSortType"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.playoff.BracketParameters.populateContext(application, session, pageContext);
%>

<html>
<head>
<title>Bracket Parameters</title>

<script
  type='text/javascript'
  src='../extlib/jquery-1.11.1.min.js'></script>

<script type='text/javascript'>
  function sanityCheck() {

    <c:if test="${not empty tableInfo}">
    var selectedTables = $("input[name='tables']:checked");
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


  <div class='status-message'>${message}</div>
  <%-- clear out the message, so that we don't see it again --%>
  <c:remove var="message" />

  <form
    method="POST"
    action="BracketParameters">

    <p>
      Choose how the teams are sorted into the brackets. Unless you are
      doing something unusual the right answer is "<%=BracketSortType.SEEDING.getDescription()%>"
    </p>

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
    </select>

    <c:if test="${not empty tableInfo}">
      <p>Choose which table pairs will be used for this playoff
        bracket.</p>


      <c:forEach
        items="${tableInfo}"
        var="info">

        <label for='${info.id }'>${info.sideA } / ${info.sideB }</label>

        <c:choose>
          <c:when test="${info.use }">
            <input
              type="checkbox"
              name='tables'
              value='${info.id}'
              checked />
          </c:when>
          <c:otherwise>
            <input
              type="checkbox"
              name='tables'
              value='${info.id}' />
          </c:otherwise>
        </c:choose>
        <br />

      </c:forEach>
    </c:if>

    <input
      type='submit'
      id='submit'
      value='Submit'
      onclick='return sanityCheck()' />

  </form>


</body>
</html>

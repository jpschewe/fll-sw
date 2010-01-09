<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <title>Developer Database Commands</title>
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Developer Database Commands)</h1>

    <p><font color='red'><b>This page is intended for developers only.  If you
    don't know what you're doing, LEAVE THIS PAGE!</b></font></p>

    <c:if test="${not empty param.message}">
      <p><i><c:out value="${param.message}"/></i></p>
    </c:if>

    <form name='query' method='post'>
      <p>Enter query
      <textarea name='query' rows='5' cols='60'><c:out value="${param.query}"/></textarea>
      </p>
      <c:if test="${not empty param.query}">
        <sql:query dataSource="${datasource}" var="query_result" scope="page" sql="${param.query}"/>
        <p>Results</p>
        <table id='queryResult' border='1'>
          <tr>
            <c:forEach var="columnName" items="${query_result.columnNames}">
              <th><c:out value="${columnName}"/></th>
            </c:forEach>
          </tr>
          <c:forEach var="row" items="${query_result.rowsByIndex}">
            <tr>
              <c:forEach var="column" items="${row}">
                <td><c:out value="${column}"/></td>
              </c:forEach>
            </tr>
          </c:forEach>
        </table>
      </c:if>
      <input type='submit' value='Execute Query'/>
    </form>
        
    <form name='update' method='post'>
      <p>Enter update
             <textarea name='update' rows='5' cols='60'><c:out value="${param.update}"/></textarea>
      </p>
      <c:if test="${not empty param.update}">
        <sql:update dataSource="${datasource}" var="update_result" scope="page" sql="${param.update}"/>
        <p>Modified rows: <c:out value="${update_result}"/></p>
      </c:if>
      <input type='submit' value='Execute Update'/>
    </form>


  </body>
</html>

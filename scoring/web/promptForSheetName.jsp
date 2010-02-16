<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <title>Choose Sheet Name</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>
    <h1><x:out select="$challengeDocument/fll/@title"/> (Choose Sheet Name)</h1>

    <form action='ProcessSelectedSheet' method='post'>
    <ul>
    <c:forEach items="${sheetNames }" var="sheetName">
      <li><input type='radio' name='sheetName' value='${sheetName}' /> ${sheetName }</li>
    </c:forEach>
        </ul>
    <input type="submit"/>
    </form>
    
  </body>
</html>

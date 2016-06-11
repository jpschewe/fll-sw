<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <title>Choose Sheet Name</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/fll-sw.css'/>" />
  </head>

  <body>
    <h1>Choose Sheet Name</h1>

    <form action='ProcessSelectedSheet' method='post'>
    <ul>
    <c:forEach items="${sheetNames }" var="sheetName">
      <li><input type='radio' name='sheetName' id='${sheetName}' value='${sheetName}' /> <label for='${sheetName}'>${sheetName }</label></li>
    </c:forEach>
        </ul>
    <input type="submit"/>
    </form>
    
  </body>
</html>

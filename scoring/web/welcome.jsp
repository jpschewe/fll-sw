<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <meta http-equiv='refresh' content='90' />
    
    <title><x:out select="$challengeDocument/fll/@title"/></title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>

  <body>

    <center>
      <h1><x:out select="$challengeDocument/fll/@title"/></h1>
      
      <br />
      <br />
      <img width='95%' align='center' src='<c:url value="/images/logo.gif"/>' /><br />

      <font face='arial' size='3'>
        <b><c:out value="${ScorePageText}" /></b>
      </font>

            
    </center>
        
  </body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf" %>

<html>
  <head>
    <meta http-equiv='refresh' content='90' />
    
    <title><x:out select="$challengeDocument/fll/@title"/></title>
<style type='text/css'>
html {
  margin-top: 5px;
  margin-bottom: 5px;
  margin-left: 5px;
  margin-right: 5px;
}
body {
      margin-top: 4;
      }
</style>
    
  </head>

  <body>

    <center>
      <h1>Boston Scientific MN <i>FIRST</i> LEGO League Tournament</h1>
        
      <h2><x:out select="$challengeDocument/fll/@title"/></h2>
      
      <br />
      <br />
      <img height="50%" align='center' src='<c:url value="/images/logo.gif"/>' /><br />

      <img height="20%" align='center' src='<c:url value="/images/fll_logo.gif"/>' /><br />
          
          <br/>

            
    </center>
        
  </body>
</html>

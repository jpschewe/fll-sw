<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.io.File"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="java.util.List"%>
<%@ page import="fll.Utilities"%>

<%
//All logos shall be located under sponsor_logos in the fll web folder.
String imagePath = application.getRealPath("sponsor_logos");
File[] directories = {new  File(imagePath)};
List<String> logoFiles = new ArrayList<String>();
Utilities.buildGraphicFileList("", directories, logoFiles);

// truncate will keep it smaller
int imagePercentage = 80 / (logoFiles.size() + 1);

%>

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
      
      <img height="40%" align='center' src='<c:url value="/images/logo.gif"/>' /><br />

     <%
      out.print("<img width='" + imagePercentage + "%' src='images/fll_logo.gif' />");
      for(final String file : logoFiles) {
        out.print("<img width='" + imagePercentage + "%' src='" + file + "' />");
     }
      %>
            
    </center>
        
  </body>
</html>

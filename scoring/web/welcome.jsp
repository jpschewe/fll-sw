<%@ include file="/WEB-INF/jspf/init.jspf" %>

<%@ page import="java.io.File"%>
<%@ page import="java.util.SortedSet"%>
<%@ page import="java.util.TreeSet"%>
<%@ page import="fll.Utilities"%>

<%
//All logos shall be located under sponsor_logos in the fll web folder.
String imagePath = application.getRealPath("sponsor_logos");
File[] directories = {new  File(imagePath)};
SortedSet<String> logoFiles = new TreeSet<String>();
Utilities.buildGraphicFileList("", directories, logoFiles);

// truncate will keep it smaller
int imagePercentage = 80 / (logoFiles.size() + 1);
int fllPercentage = Math.min(20, imagePercentage);
%>

<html>
  <head>
    <meta http-equiv='refresh' content='90' />
    
    <title>Welcome</title>
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
     <h1><x:out select="$challengeDocument/fll/@title"/></h1>
      
      <!--img height="40%" align='middle' src='<c:url value="/images/logo.gif"/>' /><br /-->
      <img height="20%" align='middle' src='<c:url value="/images/htk_logo.jpg"/>' /><br/>

<table>
<tr>

     <%
     if(!logoFiles.isEmpty()) {
     %>
<td align="center" width="50%">
<table>
      <tr><td align="center" style="padding:20px">Sponsored by:</td></tr>
      <%
      for(final String file : logoFiles) {
        out.print("<tr><td align='center'><img src='" + file + "' /></td></tr>");
      //for(final String file : logoFiles) {
      //  out.print("<img width='" + imagePercentage + "%' src='" + file + "' />");
        out.println("<tr><td>&nbsp;</td></tr>");
     }
      %>
</table>
</td>
   <%
   }
   %>
   
   <td valign="bottom">
     <%
      //out.print("<img width='" + fllPercentage + "%' src='images/fll_logo.gif' />");
      out.print("<img width='100%' src='images/fll_logo.gif' />");
     %>
</td>
   
</tr>
</table>
            
    </center>
        
  </body>
</html>

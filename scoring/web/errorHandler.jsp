<%@ page isErrorPage="true" %>
  
<html>
  <head>
    <title>An error has occurred</title>
    
    <%
    // make sure the exception gets logged
    fll.util.LogUtils.getLogger().error("An unhandled exception occurred", exception);
    %>
  </head>

  <body id='exception-handler'>
  ${message}
  
    <h1>An error has occurred</h1>

    <p><font class="error">An error has occurred!</font><br/>
        Please send the text of this page and the files in <code>tomcat/logs</code> and <code>tomcat/webapps/fll-sw/fllweb*</code> along with your bug report.<br/>
        
        Error messages:</p>
    <ul>
<%
Throwable e = exception;
while(null != e) {
%>
       <li><%=e.getMessage()%></li>
<%
  e = e.getCause();
}
%>
    </ul>

    <p>Full exception trace:</p>
    <ul>
    <%
    e = exception;
    while(null != e) {
      out.println("<li>");
      out.println(e.getMessage());
      out.println("<ul>");
      StackTraceElement[] stack = e.getStackTrace();
      for(StackTraceElement ele : stack) {
        out.println("<li>" + ele.toString() + "</li>");      
      }
      out.println("</ul></li>");
      e = e.getCause();
    }
    %>
    </ul>
      



  </body>
</html>

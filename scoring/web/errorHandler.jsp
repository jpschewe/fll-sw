<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page isErrorPage="true"%>

<html>
<head>
<title>An error has occurred</title>

<%
  // make sure the exception gets logged
  fll.util.LogUtils.getLogger().error("An unhandled exception occurred", exception);
%>
</head>

<body id='exception-handler'>
 <div class='status-message'>${message}</div>
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />
 

 <h1 class="error">An error has occurred</h1>

 <form action="<c:url value='/GatherBugReport'/>">
  <p>If you would like to submit a bug report please fill in a brief
   description and submit the bug report.</p>

  <textarea rows="5" cols="40" name="bug_description"
   id="bug_description" wrap="virtual"></textarea>
<br/>

  <input type='submit' id='submit_bug_report' value="Submit Bug Report" />
 </form>

 <p>Error messages:</p>
 <ul>
  <%
    Throwable e = exception;
    while (null != e) {
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
    while (null != e) {
      out.println("<li>");
      out.println(e.getMessage());
      out.println("<ul>");
      StackTraceElement[] stack = e.getStackTrace();
      for (StackTraceElement ele : stack) {
        out.println("<li>"
            + ele.toString() + "</li>");
      }
      out.println("</ul></li>");
      e = e.getCause();
    }
  %>
 </ul>

</body>
</html>

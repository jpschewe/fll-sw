
<%@ page import="fll.web.GetFile" %>

<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<% GetFile.getFile(application, request, response); %>

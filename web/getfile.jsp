<%@ page errorPage="errorHandler.jsp" %>
<%@ page import="fll.web.GetFile" %>

<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<% GetFile.getFile(application, request, response); %>

<%@ page import="fll.web.InitFilter" %>

<%
if(null == InitFilter.initialize(request, response)) {
  response.sendRedirect(response.encodeRedirectURL("existingdb.jsp"));
  return;
}
%>

<%@ include file="/WEB-INF/jspf/dbsetup.jspf" %>

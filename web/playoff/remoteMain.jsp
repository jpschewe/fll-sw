<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<html>
  <head>
    <title><x:out select="$challengeDocument//@title"/> (Playoff Brackets)</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
  </head>
  <frameset rows="80,*" border='1' framespacing='0'>
      <frame name='title' src='title.jsp' marginheight='0' marginwidth='0' scrolling='no'>
      <frame name='brackets' src='remoteControlBrackets.jsp' marginwidth='0' scrolling='no'>
</frameset>

</HTML>

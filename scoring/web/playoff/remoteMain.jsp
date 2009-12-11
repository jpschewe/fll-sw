<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<html>
  <head>
    <title>Playoff Brackets</title>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
    <style type='text/css'>
		html {
		  margin-top: 5px;
		  margin-bottom: 5px;
		  margin-left: 5px;
		  margin-right: 5px;
		}
    </style>
  </head>
  <frameset rows="50,*,55" border='1' framespacing='0'>
    <frame name='title' src='title.jsp' marginheight='0' marginwidth='0' scrolling='no'/>
    <frame name='brackets' src='remoteControlBrackets.jsp' marginwidth='0' scrolling='no'/>
    <frame name='sponsors' src='sponsors.jsp' marginheight='0' marginwidth='0' scrolling='no'/>
  </frameset>
</html>

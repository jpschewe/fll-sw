<%@ include file="/WEB-INF/jspf/init.jspf" %>
  
<HTML>
<head>
<script language=javascript>
  var width = screen.width;
  var height = screen.height;
  if(screen.width >= 1024) {
    //redirect to 1024x768 scoreboard
    location.href='<c:url value="/scoreboard/main.jsp"/>';
  } else {
    window.resizeTo(width, height);
  }
</script>
<style type='text/css'>
html {
  margin-top: 5px;
  margin-bottom: 5px;
  margin-left: 5px;
  margin-right: 5px;
}
</style>
</head>

<frameset cols="40%,*" border='1' framespacing='0'>
  <frameset rows='60,*' border='0' framespacing='0'>
    <frame src='<c:url value="/scoreboard/title.jsp" />' marginheight='0' marginwidth='0' scrolling='no'>
    <frame src='<c:url value="/scoreboard/allteams.jsp"><c:param name="scroll" value="true"/></c:url>' scrolling='no'>
  </frameset>
  <frameset rows='350,*' border='1' framespacing='0'>
    <frame src='<c:url value="/scoreboard/Top10"><c:param name="showOrganization" value="false" /></c:url>' marginheight='0' marginwidth='0' noresize scrolling='no' >
    <frame src='<c:url value="/scoreboard/Last8"><c:param name="showOrganization" value="false" /></c:url>' marginheight='3' marginwidth='0' noresize scrolling='no' >
  </frameset>
</frameset>

</HTML>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<HTML>
<head>
<script type='text/javascript'>
  if (screen.width >= 1024) {
    //redirect to 1024x768 scoreboard
    location.href = 'main.jsp';
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
    <frameset rows='40,*' border='0' framespacing='0'>
        <frame src='title.jsp' marginheight='0' marginwidth='0'
            scrolling='no'>
        <frame
            src='<c:url value="allteams.jsp"><c:param name="allTeamsScroll" value="true"/></c:url>'
            scrolling='no'>
    </frameset>
    <frameset rows='50%,*' border='1' framespacing='0'>
        <frame
            src='<c:url value="Top10"><c:param name="showOrganization" value="false" /></c:url>'
            marginheight='0' marginwidth='0' noresize scrolling='no'>
        <frame
            src='<c:url value="Last8"><c:param name="showOrganization" value="false" /></c:url>'
            marginheight='3' marginwidth='0' noresize scrolling='no'>
    </frameset>
</frameset>

</HTML>

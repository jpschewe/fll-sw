<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="PUBLIC" allowSetup="false" />

<html class="fll-sw-hide-cursor">
<head>
<title>Playoff Brackets</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />
<style type='text/css'>
html {
    margin-top: 5px;
    margin-bottom: 5px;
    margin-left: 5px;
    margin-right: 5px;
}
</style>
</head>
<frameset rows="25,*,65" border='1' framespacing='0'>
    <frame name='title' id='title' src='title.jsp' marginheight='0'
        marginwidth='0' scrolling='no' />
    <%-- display_uuid needs to match the value of DisplayHandler.DISPLAY_UUID_PARAMETER_NAME --%>
    <frame name='brackets'
        src='remoteControlBrackets.jsp?display_uuid=${param.display_uuid}'
        marginwidth='0' scrolling='no' />
    <frame name='sponsors' src='sponsors.jsp' marginheight='0'
        marginwidth='0' scrolling='no' />
</frameset>
</html>

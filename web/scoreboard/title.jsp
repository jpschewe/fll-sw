<%@ page errorPage="../errorHandler.jsp" %>
<%@ include file="../WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<html>
<head>
<style type="text/css">
        .clock {font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 14px; color: #FFFFFF; background-color: #000080; font-style: normal; font-weight: bold; font-variant: normal}
    </style>
      
<script language="JavaScript">

<!--
// please keep these lines on when you copy the source
// made by: Nicolas - http://www.javascript-page.com

var clockID = 0;

function UpdateClock() {
   if(clockID) {
      clearTimeout(clockID);
      clockID  = 0;
   }

   var tDate = new Date();

   document.theClock.theTime.value = "" 
                                   + fixTime(tDate.getHours()) + ":" 
                                   + fixTime(tDate.getMinutes()) + ":" 
                                   + fixTime(tDate.getSeconds());
   
   clockID = setTimeout("UpdateClock()", 1000);
}
function StartClock() {
   clockID = setTimeout("UpdateClock()", 500);
}

function KillClock() {
   if(clockID) {
      clearTimeout(clockID);
      clockID  = 0;
   }
}

function fixTime(the_time) {
  if (the_time <10) {
    the_time = "0" + the_time;
  }
  return the_time;
}


//-->

</script>
        
<script language=javascript>
        window.setInterval("location.href='title.jsp'",90000);
</script>
</head>
<body bgcolor='#000080' onload='StartClock()' onunload='KillClock()'>
<center>
<table border='0' cellpadding='0' cellspacing='0' width='98%'>
<tr>
  <td align='left'>
    <font face='arial' size='3' color='#ffffff'><b><%=application.getAttribute("ScorePageText")%></b></font>
  </td>
    <form name='theClock'>
  <td align='right' valign='top' nowrap>
    <input type='text' name='theTime' readonly class='clock' width='8' size='8'>
  </td>
    </form>
</tr>

<tr><td colspan='2'><hr size='1' color='#ffffff'></td></tr>
<tr>
  <td align='left' valign='top' nowrap colspan='2'>
    <font face='Arial' size='3' color='#ffffff'><b>All Teams: Alphabetical by Organization</b></font>
  </td>
</tr>
</table>
</center>
</body>
</html>

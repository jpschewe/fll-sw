<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ include file="/WEB-INF/jspf/initializeApplicationVars.jspf" %>
  
<html>
<head>
  <meta http-equiv='refresh' content='90'>
<style type="text/css">
        .clock {font-family: Verdana, Arial, Helvetica, sans-serif; font-size: 14px; background-color: #ffffff; font-style: normal; font-weight: bold; font-variant: normal}
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
        
</head>
  <body background="<c:url value="/images/bricks1.gif" />" bgcolor="#ffffff" onload='StartClock()' onunload='KillClock()'>
<center>
<table border='0' cellpadding='0' cellspacing='0' width='98%'>
<tr>
  <td align='center'>
    <font face='arial' size='3'>
      <b><c:out value="${ScorePageText}" /></b>
    </font>
  </td>
  <td align='right' valign='top' nowrap>
    <form name='theClock'>
    <input type='text' name='theTime' readonly class='clock'>
     </form>
  </td>
</tr>
<tr><td colspan='2'><hr size='1'></td></tr>
<tr>
</tr>
</table>
</center>
</body>
</html>

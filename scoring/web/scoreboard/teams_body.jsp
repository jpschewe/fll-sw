<% 	
String colorStr;
String divisionStr;
      
colorStr="A";
      
if(rs.next()) {
  boolean done = false;
  while(!done) {
%>
  <table border='0' cellpadding='0' cellspacing='0' width='99%' class='<%=colorStr%>'>
    <tr><td colspan='2'><img src='blank.gif' height='15' width='1'></td></tr>
    <tr align='left'>
<%
  if(rs.getInt("Division") == 1) {
    divisionStr = "#800000";
  } else {
    divisionStr = "#008000";
  }
%>
      <td width='25%' bgcolor='<%=divisionStr%>'>
        <font size='2'><b>&nbsp;&nbsp;Division:&nbsp;<%=rs.getInt("Division")%>&nbsp;&nbsp;</b></font>
     </td>
     <td align='right'>
       <font size='2'><b>Team&nbsp;#:&nbsp;<%=rs.getInt("TeamNumber")%>&nbsp;&nbsp;</b></font>
    </td>
  </tr>
  <tr align='left'>
    <td colspan='2'>
      <font size='4'>&nbsp;&nbsp;<%=rs.getString("Organization")%></font>
    </td>
  </tr>
  <tr align='left'>
    <td colspan='2'>
      <font size='4'>&nbsp;&nbsp;<%=rs.getString("TeamName")%></font>
    </td>
  </tr>
  <tr>
    <td colspan='2'><hr color='#ffffff' width='96%'></td>
  </tr>
  <tr>
  <td colspan='2'>

    <table border='0' cellpadding='1' cellspacing='0'>
      <tr align='center'>
        <td><img src='../images/blank.gif' height='1' width='60'></td>
        <td><font size='4'>Run #</font></td>
        <td><img src='../images/blank.gif' width='20' height='1'></td>
          <td><font size='4'>Score</font></td>
        </tr>
        <%
        int prevNum = rs.getInt("TeamNumber");
        do {
        %>
        <tr align='right'>
          <td><img src='blank.gif' height='1' width='60'></td>
          <td><font size='4'><%=rs.getInt("RunNumber")%></font></td>
          <td><img src='blank.gif' width='20' height='1'></td>
          <td><font size='4'>
          <%if(rs.getBoolean("NoShow")) {%>
            No Show
          <%} else if(rs.getBoolean("Bye")) {%>
            Bye
          <%} else {
              out.println(rs.getInt("ComputedTotal"));
            }
            if(!rs.next()) {
              done = true;
            }
           } while(!done && prevNum == rs.getInt("TeamNumber"));
          %>
          </font></td>
      </table>
      
    </td>
  </tr>
  <tr><td colspan='2'><img src='../images/blank.gif' width='1' height='15'></td></tr>
</table>
<%
    if("A".equals(colorStr)) {
      colorStr="B";
    } else {
      colorStr="A";
    }
  } //end while(!done)
}//end if
%>

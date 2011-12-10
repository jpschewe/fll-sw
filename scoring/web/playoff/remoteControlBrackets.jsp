<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%@ page import="fll.web.playoff.BracketData"%>
<%@ page import="fll.web.SessionAttributes" %>
<%@ page import="javax.sql.DataSource" %>
<%@ page import="java.util.List"%>
<%@ page import="fll.db.Queries" %>
<%@ page import="java.sql.Connection" %>

<%
	/*
  application parameters
  playoffDivision - String for the division
  playoffRoundNumber - Integer for the playoff round number, counted from the 1st playoff round
   */

   final DataSource datasource = SessionAttributes.getDataSource(session);
   final Connection connection = datasource.getConnection();
  final int currentTournament = Queries.getCurrentTournament(connection);

  final String divisionKey = "playoffDivision";
  final String roundNumberKey = "playoffRoundNumber";
  final String displayName = (String)session.getAttribute("displayName");

  final String sessionDivision;
  final Number sessionRoundNumber;
  if (null != displayName) {
    sessionDivision = (String) application.getAttribute(displayName
        + "_" + divisionKey);
    sessionRoundNumber = (Number) application.getAttribute(displayName
        + "_" + roundNumberKey);
  } else {
    sessionDivision = null;
    sessionRoundNumber = null;
  }

  final String division;
  if (null != sessionDivision) {
    division = sessionDivision;
  } else if (null == application.getAttribute(divisionKey)) {
    final List<String> divisions = Queries.getEventDivisions(connection);
    if (!divisions.isEmpty()) {
      division = divisions.get(0);
    } else {
      throw new RuntimeException("No division specified and no divisions in the database!");
    }
  } else {
    division = (String) application.getAttribute(divisionKey);
  }

  final int playoffRoundNumber;
  if (null != sessionRoundNumber) {
    playoffRoundNumber = sessionRoundNumber.intValue();
  } else if (null == application.getAttribute(roundNumberKey)) {
    playoffRoundNumber = 1;
  } else {
    playoffRoundNumber = ((Number) application.getAttribute(roundNumberKey)).intValue();
  }

  final int numPlayoffRounds = Queries.getNumPlayoffRounds(connection, division);

  final BracketData bracketInfo = new BracketData(connection, division, playoffRoundNumber, playoffRoundNumber + 2, 4, false, true);

  bracketInfo.addBracketLabels(playoffRoundNumber);
  bracketInfo.addStaticTableLabels(connection, currentTournament, division);
%>

<html>
<head>
<link rel="stylesheet" type="text/css" href="<c:url value='/style/style.jsp'/>" />
<title>Playoff Round <%=playoffRoundNumber%>, Division <%=division%></title>
</head>
<style type='text/css'>
TD.Leaf {
	color: #ffffff;
	font-family: Arial;
	background-color: #000000
}

TD.Bridge {
	background-color: #808080
}

FONT {
	font-family: Arial
}

FONT.TeamNumber {
	color: #ff8080;
	font-weight: bold
}

FONT.TeamName {
	color: #ffffff;
	font-weight: bold
}

FONT.TeamScore {
	color: #ffffff;
	font-weight: bold;
	font-size: 10pt
}

FONT.TIE {
	color: #ff0000;
	font-weight: bold
}

.TABLE_ASSIGNMENT {
	font-family: monospace;
	font-size: 85%;
	font-weight: bold;
	background-color: white;
	padding-left: 5%;
	padding-right: 5%
}
</style>

<!-- stuff for automatic scrolling -->
<script type="text/javascript">
var scrollTimer;
var scrollAmount = 10;    // scroll by 10 pixels each tick
var documentYposition = 0;
var scrollPause = 250; // amount of time, in milliseconds, to pause between scrolls

function scroll() {
  window.scrollBy(0, scrollAmount);
}

function start() {
    scrollTimer = window.setInterval('scroll()',scrollPause);
}
</script>
<!-- end stuff for automatic scrolling -->
<%final String jQueryURL = response.encodeURL("/fll-sw/jquery-1.7.1.min.js");%>
<script type="text/javascript" src="<%=jQueryURL%>"></script>
<script type="text/javascript">
  <%final String ajaxURL = response.encodeURL("/fll-sw/ajax/");%>
  var ajaxURL = '<%=ajaxURL%>';
  <%final int numSeedingRounds = Queries.getNumSeedingRounds(connection, currentTournament); %>
  var seedingRounds = <%=numSeedingRounds%>;
  function iterate () {
      $(".js-leaf").each(function() {
          var lid = $(this).attr('id');
          var leafEl = this;
          $.ajax({
          url: ajaxURL+"brackets.jsp?row="+lid.split("-")[0]+"&round="+lid.split("-")[1],
          dataType: "json",
          cache: false,
          beforeSend: function(xhr){
              xhr.overrideMimeType('text/plain');
          }
          }).done(function(data) {
              if (data._team._teamNumber < 0) {
                $(this).html("<font class=\"TeamName\">" + data._team._teamName + "</font>"); 
                return;
              } else { // /if team number meant a bye
                  var score;
                  $.ajax({
                      url: ajaxURL+"runScore.jsp?team="+data._team._teamNumber+"&run="+(seedingRounds+parseInt(lid.split("-")[1])),
                      cache: true,
                      beforeSend: function(xhr){
                          xhr.overrideMimeType('text/plain');
                      } // /beforesend
                  }).done(function(scoreData){
                  console.log(scoreData.indexOf("NaN") == -1);
                      if (scoreData.indexOf("NaN") == -1) {
                          $(leafEl).html("<font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + data._team._teamName + "</font><font class=\"TeamScore\"> Score: " + scoreData + "</font><!-- updated -->");
                          return;
                      } else {
                          $(this).html("<font class=\"TeamNumber\">#" + data._team._teamNumber + "</font> <font class=\"TeamName\">" + data._team._teamName + "</font><!-- updated -->");
                          return;
                      } // /else
                  }); // /.done
              } // /if team num not a bye
              }); // /first .ajax
      }); // /.each
  } // /iterate()
  
  
</script>

<body onload='start()'>
<!-- dummy tag and some blank lines for scolling -->
<div id="dummy" style="position: absolute"><br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>
<br/>

<table align='center' width='100%' border='0' cellpadding='3' cellspacing='0'>
 <%
   if (playoffRoundNumber <= numPlayoffRounds) {

     for (int rowIndex = 1; rowIndex <= bracketInfo.getNumRows(); rowIndex++) {
 %>
 <tr>
  <%
    // Get each cell. Insert bridge cells between columns.
        for (int i = bracketInfo.getFirstRound(); i < bracketInfo.getLastRound(); i++) {
  %>
  <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, i)%>
  <%=bracketInfo.getHtmlBridgeCell(rowIndex, i, BracketData.TopRightCornerStyle.MEET_TOP_OF_CELL)%>
  <%
    }
  %>
  <%=bracketInfo.getHtmlCell(connection, currentTournament, rowIndex, bracketInfo.getLastRound())%>
 </tr>
 <%
   }
   }//end if we have more than 2 teams
 %>
</table>
</div>

</body>
</html>

<%@ include file="/WEB-INF/jspf/init.jspf"%>

<%
  fll.web.admin.RemoteControl.populateContext(application,
					pageContext);
%>

<html>
<head>
<title>Display Controller</title>
<link rel="stylesheet" type="text/css"
 href="<c:url value='/style/fll-sw.css'/>" />

<script type="text/javascript">
	function display(id) {
		document.getElementById(id).style.display = "block";
	}
	function hide(id) {
		document.getElementById(id).style.display = "none";
	}
</script>

</head>
<body>

 <h1>Display Controller</h1>

 <p>This page is used to control what page is currently visible on
  the display screen. Note that it takes some time for the display to
  change, up to 2 minutes.</p>

 <%-- DEBUG
<ul>
<li>displayPage - ${applicationScope.displayPage }</li>
<li>displayURL - ${applicationScope.displayURL }</li>
<li>playoffDivision - ${applicationScope.playoffDivision }</li>
<li>playoffRoundNumber - ${applicationScope.playoffRoundNumber }</li>
    <c:if test="${not empty displayNames}">
      <c:forEach items="${displayNames}" var="displayName">
  <c:set var="displayPageKey" value="${displayName.key}_displayPage" />
  <c:set var="displayURLKey" value="${displayName.key}_displayURL" />
                <c:set var="playoffDivisionKey" value="${displayName.key}_playoffDivision" />
              <c:set var="playoffRoundNumberKey" value="${displayName.key}_playoffRoundNumber" />
  
<li>Display - ${displayName.key}
<ul>
<li>displayPage - ${applicationScope[displayPageKey]}</li>
<li>displayURL - ${applicationScope[displayURLKey] }</li>
<li>playoffDivision - ${applicationScope[playoffDivisionKey] }</li>
<li>playoffRoundNumber - ${applicationScope[playoffRoundNumberKey] }</li>
</ul>
</li>
</c:forEach>
</c:if>

</ul>
END DEBUG --%>

 <%-- NOTE: The values of the radio buttons need to match up with the strings in DisplayQueryServlet.pickURL() --%>
 ${message}
 <%-- clear out the message, so that we don't see it again --%>
 <c:remove var="message" />

 <form name='remote' action='RemoteControlPost' method='post'>

  <table border='1'>
   <tr>
    <td>&nbsp;</td>
    <th>Default</th>
    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <th>${displayName.key}- seen @ <fmt:formatDate
        value="${displayName.value }" type="TIME" timeStyle="short" /></th>
     </c:forEach>
    </c:if>
   </tr>

   <tr>
    <th>Delete Named Display</th>
    <td>&nbsp;</td>
    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <c:set var="displayPageKey" value="${displayName.key}_displayPage" />
      <td><input type="checkbox" name="${displayName.key }_delete" />
      </td>
     </c:forEach>
    </c:if>
   </tr>

   <tr>
    <th>Follow Default</th>
    <td>&nbsp;</td>
    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <c:set var="displayPageKey" value="${displayName.key}_displayPage" />
      <td><c:choose>
        <c:when test="${empty applicationScope[displayPageKey]}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='default' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='default' />
        </c:otherwise>
       </c:choose></td>
     </c:forEach>
    </c:if>
   </tr>

   <tr>
    <th>Welcome</th>
    <td><input type='radio' id='welcome' name='remotePage'
     value='welcome'
     <c:if test='${displayPage == "welcome"}'>
    checked
    </c:if> />
    </td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:choose>
        <c:when test="${applicationScope[displayPageKey] == 'welcome'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='welcome' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='welcome' />
        </c:otherwise>
       </c:choose></td>
     </c:forEach>
    </c:if>
   </tr> <!-- welcome -->

   <tr>
    <th>Scoreboard</th>
    <td><c:if test='${displayPage == "scoreboard"}'
      var='scoreboardPage'>
      <input type='radio' id='scoreboard' name='remotePage'
       value='scoreboard' checked />
     </c:if> <c:if test='${not scoreboardPage}'>
      <input type='radio' id='scoreboard' name='remotePage'
       value='scoreboard' />
      <br />
     </c:if></td>
    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:choose>
        <c:when
         test="${applicationScope[displayPageKey] == 'scoreboard'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='scoreboard' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='scoreboard' />
        </c:otherwise>
       </c:choose></td>
     </c:forEach>
    </c:if>
   </tr> <!--  Scoreboard -->

   <tr>
    <th>Playoffs <i>WARNING: Do not select brackets until all
      seeding runs have been recorded!</i>
    </th>
    <td><c:if test='${displayPage == "playoffs"}'
      var='playoffsPage'>
      <input type='radio' id='playoffs' name='remotePage'
       value='playoffs' checked />
     </c:if> <c:if test='${not playoffsPage}'>
      <input type='radio' id='playoffs' name='remotePage'
       value='playoffs' />
     </c:if> Division: <select name='playoffDivision'>
      <c:forEach items="${divisions}" var="division">
       <c:choose>
        <c:when test="${division == playoffDivision}">
         <option value="${division}" selected>${division}</option>
        </c:when>
        <c:otherwise>
         <option value="${division}">${division}</option>
        </c:otherwise>
       </c:choose>
      </c:forEach>
    </select> <br /> Playoff Round: <select name='playoffRoundNumber'>
      <c:forEach begin="1" end="${numPlayoffRounds}" var="numRounds">
       <c:choose>
        <c:when test="${numRounds == playoffRoundNumber}">
         <option value="${numRounds}" selected>${numRounds}</option>
        </c:when>
        <c:otherwise>
         <option value="${numRounds}">${numRounds}</option>
        </c:otherwise>
       </c:choose>
      </c:forEach>
    </select></td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:set
        var="playoffDivisionKey" value="${displayName.key}_playoffDivision" />
       <c:set var="playoffRoundNumberKey"
        value="${displayName.key}_playoffRoundNumber" /> <c:choose>
        <c:when test="${applicationScope[displayPageKey] == 'playoffs'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='playoffs' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='playoffs' />
        </c:otherwise>
       </c:choose> Division: <select name='${displayName.key}_playoffDivision'>
        <c:forEach items="${divisions}" var="division">
         <c:choose>
          <c:when
           test="${division == applicationScope[playoffDivisionKey]}">
           <option value="${division}" selected>${division}</option>
          </c:when>
          <c:otherwise>
           <option value="${division}">${division}</option>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </select> <br /> Playoff Round: <select
       name='${displayName.key}_playoffRoundNumber'>
        <c:forEach begin="1" end="${numPlayoffRounds}" var="numRounds">
         <c:choose>
          <c:when
           test="${numRounds == applicationScope[playoffRoundNumberKey]}">
           <option value="${numRounds}" selected>${numRounds}</option>
          </c:when>
          <c:otherwise>
           <option value="${numRounds}">${numRounds}</option>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </select></td>

     </c:forEach>
    </c:if>

   </tr> <!--  Playoffs -->

   <tr>
    <th>Finalist Schedule</th>
    <td><c:choose>
      <c:when test='${displayPage == "finalistSchedule"}'>
       <input type='radio' id='finalistSchedule' name='remotePage'
        value='finalistSchedule' checked />
      </c:when>
      <c:otherwise>

       <input type='radio' id='finalistSchedule' name='remotePage'
        value='finalistSchedule' />
      </c:otherwise>
     </c:choose> Division: <select name='finalistDivision'>
      <c:forEach var="fdiv" items="${finalistDivisions }">

       <c:choose>
        <c:when test="${fdiv == finalistDivision}">
         <option value="${fdiv}" selected>${fdiv}</option>
        </c:when>
        <c:otherwise>
         <option value="${fdiv}">${fdiv}</option>
        </c:otherwise>
       </c:choose>
      </c:forEach>
    </select></td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:set
        var="finalistDivisionKey"
        value="${displayName.key}_finalistDivision" /> <c:choose>
        <c:when
         test="${applicationScope[displayPageKey] == 'finalistSchedule'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='finalistSchedule' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='finalistSchedule' />
        </c:otherwise>

       </c:choose> Division: <select name='${displayName.key}_finalistDivision'>

        <c:forEach items="${finalistDivisions}" var="fdiv">
         <c:choose>
          <c:when
           test="${fdiv == applicationScope[finalistDivisionKey]}">
           <option value="${fdiv}" selected>${fdiv}</option>
          </c:when>
          <c:otherwise>
           <option value="${fdiv}">${fdiv}</option>
          </c:otherwise>
         </c:choose>
        </c:forEach>
      </select></td>

     </c:forEach>
    </c:if>

   </tr> <!--  Finalist Schedule -->

   <tr>
    <th>Finalist Teams</th>
    <td><input type='radio' id='finalistTeams' name='remotePage'
     value='finalistTeams'
     <c:if test='${displayPage == "finalistTeams"}'>
    checked
    </c:if> />
    </td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:choose>
        <c:when test="${applicationScope[displayPageKey] == 'finalistTeams'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='finalistTeams' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='finalistTeams' />
        </c:otherwise>
       </c:choose></td>
     </c:forEach>
    </c:if>
   </tr> <!--  Finalist teams -->
   
      <tr>
    <th>Slide show<br /> Seconds to show a slide: <input
     type='text' name='slideInterval'
     value='<c:out value="${slideShowInterval}"/>' size='3' />
    </th>
    <td><c:if test='${displayPage eq "slideshow"}'
      var='slideshowPage'>
      <input type='radio' id="slideshow" name='remotePage'
       value='slideshow' checked />
     </c:if> <c:if test='${not slideshowPage}'>
      <input type='radio' id="slideshow" name='remotePage'
       value='slideshow' />
     </c:if></td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:choose>
        <c:when
         test="${applicationScope[displayPageKey] == 'slideshow'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='slideshow' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='slideshow' />
        </c:otherwise>
       </c:choose></td>
     </c:forEach>
    </c:if>

   </tr> <!--  Slideshow -->
   
   <tr>
    <th>Specify page relative to /fll-sw</th>
    <td><c:choose>
      <c:when test='${displayPage == "special"}'>
       <input type='radio' id='special' name='remotePage'
        value='special' checked />
       <input type='text' name='remoteURL'
        value="${appliationScope.displayURL}">
      </c:when>
      <c:otherwise>
       <input type='radio' id='special' name='remotePage'
        value='special' />
       <input type='text' name='remoteURL'
        value="${applicationScope.displayURL}">
      </c:otherwise>
     </c:choose></td>

    <c:if test="${not empty displayNames}">
     <c:forEach items="${displayNames}" var="displayName">
      <td><c:set var="displayPageKey"
        value="${displayName.key}_displayPage" /> <c:choose>
        <c:when test="${applicationScope[displayPageKey] == 'special'}">
         <input type='radio' name="${displayName.key}_remotePage"
          value='special' checked />
        </c:when>
        <c:otherwise>
         <input type='radio' name="${displayName.key}_remotePage"
          value='special' />
        </c:otherwise>
       </c:choose> <input type='text' name='${displayName.key}_remoteURL'
       value="${applicationScope.displayURL}"></td>
     </c:forEach>
    </c:if>

   </tr> <!-- specify page -->

  </table>

  <input type='submit' name='submit' value='Submit' />

 </form>



</body>
</html>

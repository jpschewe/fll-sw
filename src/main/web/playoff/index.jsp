<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%
fll.web.playoff.PlayoffIndex.populateContext(application, session, pageContext);
%>

<html>
<head>
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

<title>Head to Head</title>
</head>

<body>
    <h1>Head to Head menu</h1>

    <%@ include file="/WEB-INF/jspf/message.jspf"%>
    <c:choose>
        <c:when test="${not runningHeadToHead}">
            <p>Head to head is disabled for this tournament. There
                is nothing to do here.
        </c:when>
        <c:otherwise>

            <p>
                If using the automatic table assignment feature for
                score sheet generation, make certain to set up labels
                for each of your tables, available from the Admin Index
                or by clicking <a
                    href='<c:url value="/admin/tables.jsp"/>'
                    target='_new'>here</a>.

                <c:if test="${not tablesAssigned }">
                    <span class='warning'> Tables are currently
                        not assigned. </span>
                </c:if>

            </p>

            <p>
                Scores for the head to head brackets are entered just
                like the scores for regular match play using the <a
                    href='<c:url value="/scoreEntry/select_team.jsp"/>'
                    target='_new'>Score Entry</a> page. If a team leaves
                the tournament before head to head, they should be
                entered as a No Show on the Score Entry page. If a tie
                occurs in a match; print out the score sheets again and
                run the teams a second time. Then change the scores for
                the teams to be the new scores.
            </p>

            <ol>

                <li>
                    <a id='create-bracket'
                        href="create_playoff_division.jsp">Create
                        head to head bracket</a>
                </li>

                <li>
                    <a href='checkSeedingRoundsResult.jsp'
                        id='check_seeding_rounds'>Check Missing
                        Rounds</a> Check to make sure all teams have scores
                    entered for each regular match play round.
                </li>


                <li>
                    <form name='initialize'
                        action='StorePlayoffParameters' method='POST'>
                        <b>WARNING: Do not initialize any head to
                            head brackets until regular match play
                            rounds have been recorded!</b>
                        Doing so will automatically add bye runs to the
                        teams that don't have enough regular match play
                        rounds.
                        <br />
                        Select Bracket:
                        <c:set var="init_disabled" value="disabled" />
                        <select id='initialize-division' name='division'>
                            <c:forEach
                                items="${playoff_data.uninitializedBrackets }"
                                var="division">
                                <option value='${division}'>${division}</option>

                                <c:set var="init_disabled" value="" />

                            </c:forEach>
                        </select>
                        <br>
                        <input type='checkbox' name='enableThird'
                            value='yes' />
                        Check to enable 3rd/4th place match
                        <br>
                        <input type='submit' id='initialize_brackets'
                            value='Initialize Bracket' ${init_disabled } />
                        <a
                            href='javascript:display("InitializeBracketHelp")'>[help]</a>
                        <div id='InitializeBracketHelp' class='help'
                            style='display: none'>
                            Initializing a head to head bracket allows
                            it to be run. A head to head bracket cannot
                            be initialized if any teams that are to
                            compete in the head to head bracket Are
                            still competing in a head to head bracket
                            that has not been run to completion. The
                            3rd/4th place match is need if you want to
                            know not only 1st and 2nd place in the
                            bracket, but 3rd and 4th place as well. This
                            will add a match with the two teams that
                            lost in the semi-final matches.<a
                                href='javascript:hide("InitializeBracketHelp")'>[hide]</a>
                        </div>

                    </form>
                </li>

                <%-- edit table assignments --%>
                <li>
                    <form name='edit_table_assignments'
                        action='scoregenbrackets.jsp' method='get'
                        target="_blank">
                        <input type='hidden' name='editTables'
                            value='true' />

                        Edit Table Assignments
                        <br />
                        <%-- bracket --%>
                        Select Bracket:
                        <select id='edit_tables.division'
                            name='division'>
                            <c:forEach
                                items="${playoff_data.initializedBrackets }"
                                var="division">
                                <option value='${division}'>${division}</option>
                            </c:forEach>
                        </select>

                        <%-- select rounds --%>
                        from round
                        <select name='firstRound'>
                            <c:forEach begin="1"
                                end="${playoff_data.numPlayoffRounds }"
                                var="numRounds">
                                <c:choose>
                                    <c:when test="${numRounds == 1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>
                        to

                        <%-- numPlayoffRounds+1 == the column in which the 1st place winner is displayed  --%>
                        <select name='lastRound'>
                            <c:forEach begin="2"
                                end="${playoff_data.numPlayoffRounds+1 }"
                                var="numRounds">
                                <c:choose>
                                    <c:when
                                        test="${numRounds == playoff_data.numPlayoffRounds+1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>

                        <%-- submit--%>
                        <input type='submit'
                            id='display_edit_table_assignments'
                            value='Display Brackets'>
                        <a href='javascript:display("EditTablesHelp")'>[help]</a>
                        <div id='EditTablesHelp' class='help'
                            style='display: none'>
                            This page is used to edit table assignments.
                            This page is primarly used when scoring is
                            done electronically and no score sheets are
                            printed. <a
                                href='javascript:hide("EditTablesHelp")'>[hide]</a>
                        </div>

                    </form>
                </li>
                <%-- end edit table assignments --%>

                <%-- scoresheet generation --%>
                <li>
                    <form name='printable' action='scoregenbrackets.jsp'
                        method='get' target="_blank">
                        Score sheet Generation Brackets
                        <br />
                        <%-- bracket --%>
                        Select Bracket:
                        <select id='printable.division' name='division'>
                            <c:forEach
                                items="${playoff_data.initializedBrackets }"
                                var="division">
                                <option value='${division}'>${division}</option>
                            </c:forEach>
                        </select>

                        <%-- select rounds --%>
                        from round
                        <select name='firstRound'>
                            <c:forEach begin="1"
                                end="${playoff_data.numPlayoffRounds }"
                                var="numRounds">
                                <c:choose>
                                    <c:when test="${numRounds == 1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>
                        to

                        <%-- numPlayoffRounds+1 == the column in which the 1st place winner is displayed  --%>
                        <select name='lastRound'>
                            <c:forEach begin="2"
                                end="${playoff_data.numPlayoffRounds+1 }"
                                var="numRounds">
                                <c:choose>
                                    <c:when
                                        test="${numRounds == playoff_data.numPlayoffRounds+1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>

                        <%-- submit--%>
                        <input type='submit'
                            id='display_scoregen_brackets'
                            value='Display Brackets'>
                        <a
                            href='javascript:display("ScoreGenBracketsHelp")'>[help]</a>
                        <div id='ScoreGenBracketsHelp' class='help'
                            style='display: none'>
                            This page is used to print out score sheets
                            for the specified head to head bracket. This
                            page will automatically assign the matches
                            to the tables specified in on the table
                            assignment page. The assigned tables can be
                            changed before printing if desired. <a
                                href='javascript:hide("ScoreGenBracketsHelp")'>[hide]</a>
                        </div>

                    </form>
                </li>
                <%-- end scoresheet generation --%>

                <li>
                    <form name='admin' action='adminbrackets.jsp'
                        method='get' target="_blank">
                        Select Bracket to print:
                        <select name='division'>
                            <c:forEach
                                items="${playoff_data.initializedBrackets }"
                                var="division">
                                <option value='${division}'>${division}</option>
                            </c:forEach>
                        </select>
                        from round
                        <select name='firstRound'>
                            <c:forEach begin="1"
                                end="${playoff_data.numPlayoffRounds }"
                                var="numRounds">
                                <c:choose>
                                    <c:when test="${numRounds == 1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>
                        to
                        <%-- numPlayoffRounds+1 == the column in which the 1st place winner is displayed  --%>
                        <select name='lastRound'>
                            <c:forEach begin="2"
                                end="${playoff_data.numPlayoffRounds+1 }"
                                var="numRounds">
                                <c:choose>
                                    <c:when
                                        test="${numRounds == playoff_data.numPlayoffRounds+1 }">
                                        <option value='${numRounds }'
                                            selected>${numRounds }</option>
                                    </c:when>
                                    <c:otherwise>
                                        <option value='${numRounds }'>${numRounds }</option>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </select>
                        <input type='submit'
                            id='display_printable_brackets'
                            value='Display Brackets'>
                        <a
                            href='javascript:display("PrintableBracketsHelp")'>[help]</a>
                        <div id='PrintableBracketsHelp' class='help'
                            style='display: none'>
                            This page is used to print out the bracket
                            for registration or the emcee. The score
                            sheet generation bracket page should be used
                            to print out the first matches before this
                            page is used to print the bracket. This will
                            ensure that the assigned tables appears on
                            the printed bracket. <a
                                href='javascript:hide("PrintableBracketsHelp")'>[hide]</a>
                        </div>

                    </form>
                </li>


                <li>
                    <%-- uninitialize bracket --%>
                    <form name="uninitialize_playoff" method="POST"
                        action="UninitializePlayoff">
                        Select bracket to uninitialize:
                        <select id='uninitialize-division'
                            name='division'>
                            <c:forEach
                                items="${playoff_data.safeToUninitialize }"
                                var="division">
                                <option value='${division}'>${division}</option>
                            </c:forEach>
                        </select>
                        <input type='submit'
                            id='uninitialize_playoff-submit'
                            value='Submit'
                            onclick='return confirm("Are you absolutely sure you want to delete all scores associated with this head to head bracket?")' />

                        <a
                            href='javascript:display("UninitializeBracketsHelp")'>[help]</a>
                        <div id='UninitializeBracketsHelp' class='help'
                            style='display: none'>
                            This link is used to uninitialize a head to
                            head bracket. In most cases this link should
                            not be used. It may be useful if a head to
                            head bracket was initialized by mistake and
                            a different one should be run first or the
                            options for how the bracket is initialized
                            should change. Any scores that have been
                            entered for this bracket will be deleted.<a
                                href='javascript:hide("UninitializeBracketsHelp")'>[hide]</a>
                        </div>

                    </form>
                    <%-- end uninitialize bracket --%>
                </li>

                <li>
                    <%-- delete bracket --%>
                    <form name="delete_playoff" method="POST"
                        action="DeletePlayoff">
                        Select bracket to delete:
                        <select id='delete-division' name='division'>
                            <c:forEach
                                items="${playoff_data.safeToDelete }"
                                var="division">
                                <option value='${division}'>${division}</option>
                            </c:forEach>
                        </select>
                        <input type='submit' id='delete_playoff-submit'
                            value='Submit'
                            onclick='return confirm("Are you absolutely sure you want to delete all scores associated with this head to head bracket?")' />

                        <a
                            href='javascript:display("DeleteBracketsHelp")'>[help]</a>
                        <div id='DeleteBracketsHelp' class='help'
                            style='display: none'>
                            This link is used to delete a head to head
                            bracket. In most cases this link should not
                            be used. It may be useful if a head to head
                            bracket was created by mistake. Any scores
                            that have been entered for this bracket will
                            be deleted.<a
                                href='javascript:hide("DeleteBracketsHelp")'>[hide]</a>
                        </div>

                    </form>
                    <%-- end delete bracket --%>
                </li>

                <li>
                    <%-- finish bracket --%>
                    <form name="finish_bracket" method="POST"
                        action="FinishBracket">
                        Select bracket to finish:
                        <select id='finish-bracket' name='bracket'>
                            <c:forEach
                                items="${playoff_data.unfinishedBrackets }"
                                var="bracket">
                                <option value='${bracket}'>${bracket}</option>
                            </c:forEach>
                        </select>
                        <input type='submit' id='finish_bracket-submit'
                            value='Submit'
                            onclick='return confirm("Are you absolutly sure you want to fill in dummy scores for the rest of this head to head bracket?")' />

                        <a
                            href='javascript:display("FinishBracketsHelp")'>[help]</a>
                        <div id='FinishBracketsHelp' class='help'
                            style='display: none'>
                            This link is used to finish a head to head
                            bracket. This can be used when you don't
                            want to run to the end of the bracket.
                            Minnesota does this when running their state
                            head to head as they run 2 brackets down to
                            4 teams each and then run a final 8 team
                            bracket.<a
                                href='javascript:hide("FinishBracketsHelp")'>[hide]</a>
                        </div>

                    </form>
                    <%-- end finish division --%>
                </li>

            </ol>

            <c:if test="${not empty playoff_data.initializedBrackets }">

                <h2>Other useful pages</h2>
                <ul>

                    <li>
                        <a
                            href="<c:url value='/playoff/h2h-checkin-instructions.jsp' />">Instructions
                            for tablet displays</a>
                    </li>

                    <li>
                        <a href="remoteMain.jsp">Scrolling Head to
                            head Bracket</a> (as on big screen display)
                        <br />
                        Bracket and round must be selected from the big
                        screen display <a
                            href="<c:url value='/admin/remoteControl.jsp'/>">remote
                            control</a> page.
                    </li>

                    <li>
                        <a href="remoteControlBrackets.jsp?scroll=false">Non-Scrolling
                            Head to head Bracket</a> (as on big screen
                        display)
                        <br />
                        Bracket and round must be selected from the big
                        screen display <a
                            href="<c:url value='/admin/remoteControl.jsp'/>">remote
                            control</a> page.
                    </li>

                </ul>
            </c:if>
            <!-- if initialized playoff divisions not empty -->

        </c:otherwise>
    </c:choose>
    <!-- if head to head enabled -->


</body>
</html>

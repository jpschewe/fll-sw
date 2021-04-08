<%@page import="fll.web.admin.GatherParameterInformation"%>
<%@ include file="/WEB-INF/jspf/init.jspf"%>

<fll-sw:required-roles roles="ADMIN" allowSetup="false" />

<%@ page import="fll.db.TournamentParameters"%>

<%
GatherParameterInformation.populateContext(application, pageContext);
%>

<html>
<head>
<title>Edit Parameters</title>
<link rel="stylesheet" type="text/css"
    href="<c:url value='/style/fll-sw.css'/>" />

<style>
.content table {
    border-collapse: collapse;
}

.content table, .content th, .content td {
    border: 1px solid black;
}

.content td, .content td {
    text-align: center;
}
</style>

<script type="text/javascript"
    src="<c:url value='/extlib/jquery-1.11.1.min.js'/>"></script>
<script type="text/javascript"
    src="<c:url value='/extlib/jquery-validation/dist/jquery.validate.min.js'/>"></script>

<!-- functions to displaying and hiding help -->
<script type="text/javascript">
  function display(id) {
    document.getElementById(id).style.display = "block";
  }
  function hide(id) {
    document.getElementById(id).style.display = "none";
  }

  $(document).ready(function() {
    $("#edit_global_parameters").validate();
  });
</script>

</head>

<body>

    <div class='content'>

        <h1>Edit Global Parameters</h1>

        <p>These parameters are specified globally and apply to all
            tournaments in the database.</p>

        <div class='status-message'>${message}</div>
        <%-- clear out the message, so that we don't see it again --%>
        <c:remove var="message" />


        <p>This page is for advanced users only. Be careful changing
            parameters here.</p>

        <form name='edit_global_parameters' id='edit_global_parameters'
            action='StoreGlobalParameters' method='POST'>

            <table>

                <tr>
                    <th>Parameter</th>
                    <th>Value</th>
                </tr>

                <tr>
                    <th>
                        Standardized Mean <a
                            href='javascript:display("StandardizedMeanHelp")'>[help]</a>
                        <div id='StandardizedMeanHelp' class='help'
                            style='display: none'>
                            The mean that we scale the raw mean of all
                            scores in a category for a judge to. <a
                                href='javascript:hide("StandardizedMeanHelp")'>[hide]</a>
                        </div>

                    </th>
                    <td>
                        <input type='text' value="${gStandardizedMean }"
                            id='gStandardizedMean'
                            name='gStandardizedMean'
                            class='required number' />
                    </td>
                </tr>

                <tr>
                    <th>
                        Standardized Sigma <a
                            href='javascript:display("StandardizedSigmaHelp")'>[help]</a>
                        <div id='StandardizedSigmaHelp' class='help'
                            style='display: none'>
                            The sigma to use when scaling scores for
                            comparison. <a
                                href='javascript:hide("StandardizedSigmaHelp")'>[hide]</a>
                        </div>

                    </th>
                    <td>
                        <input type='text'
                            value="${gStandardizedSigma }"
                            id='gStandardizedSigma'
                            name='gStandardizedSigma'
                            class='required number' />
                    </td>
                </tr>

                <tr>
                    <th>
                        Award Group Flip Rate (seconds) <a
                            href='javascript:display("DivisionFlipRateHelp")'>[help]</a>
                        <div id='DivisionFlipRateHelp' class='help'
                            style='display: none'>
                            The number of seconds between when the
                            scoreboard's "Top Scores" panel switches
                            which division is shown. Default 30 seconds.
                            <a
                                href='javascript:hide("DivisionFlipRateHelp")'>[hide]</a>
                        </div>
                    </th>
                    <td>
                        <input type='text' value="${gDivisionFlipRate}"
                            id='gDivisionFlipRate'
                            name='gDivisionFlipRate'
                            class='required digits' />
                    </td>
                </tr>

                <tr>
                    <th>
                        All teams scroll rate control. <a
                            href='javascript:display("gAllTeamsMsPerRowHelp")'>[help]</a>
                        <div id='gAllTeamsMsPerRowHelp' class='help'
                            style='display: none'>The value is
                            nominally the number of milliseconds to
                            display each row of the page.</div>
                    </th>

                    <td>
                        <input type='text' value="${gAllTeamsMsPerRow }"
                            id='gAllTeamsMsPerRow'
                            name='gAllTeamsMsPerRow'
                            class='required digits' />
                    </td>
                </tr>

                <tr>
                    <th>
                        Head to head scroll rate control. <a
                            href='javascript:display("gHeadToHeadMsPerRowHelp")'>[help]</a>
                        <div id='gHeadToHeadMsPerRowHelp' class='help'
                            style='display: none'>The value is
                            nominally the number of milliseconds to
                            display each row of the page. The remote
                            control brackets page needs to be refreshed
                            for this parameter to take effect.</div>
                    </th>

                    <td>
                        <input type='text'
                            value="${gHeadToHeadMsPerRow }"
                            id='gHeadToHeadMsPerRow'
                            name='gHeadToHeadMsPerRow'
                            class='required digits' />
                    </td>
                </tr>
                <tr>
                    <th colspan="2">FLL Tools integration</th>
                </tr>

                <tr>
                    <th>
                        Mhub hostname<a
                            href='javascript:display("MhubHostnameHelp")'>[help]</a>
                        <div id='MhubHostnameHelp' class='help'
                            style='display: none'>
                            The hostname where mhub is running. Clear to
                            specify that mhub is not in use. <a
                                href='javascript:hide("MhubHostnameHelp")'>[hide]</a>
                        </div>

                    </th>
                    <td>
                        <input type='text' value="${gMhubHostname }"
                            id='gMhubHostname' name='gMhubHostname' />
                    </td>
                </tr>

                <tr>
                    <th>
                        Mhub port<a
                            href='javascript:display("MhubPortHelp")'>[help]</a>
                        <div id='MhubPortHelp' class='help'
                            style='display: none'>
                            The port where mhub is running.<a
                                href='javascript:hide("MhubPortHelp")'>[hide]</a>
                        </div>

                    </th>
                    <td>
                        <input type='text' value="${gMhubPort }"
                            id='gMhubPort' name='gMhubPort'
                            class='required digits' />
                    </td>
                </tr>

                <tr>
                    <th>
                        Display node<a
                            href='javascript:display("MhubDisplayNodeHelp")'>[help]</a>
                        <div id='MhubDisplayNodeHelp' class='help'
                            style='display: none'>
                            The node to send display messages to. <a
                                href='javascript:hide("MhubDisplayNodeHelp")'>[hide]</a>
                        </div>

                    </th>
                    <td>
                        <input type='text' value="${gMhubDisplayNode }"
                            id='gMhubDisplayNode'
                            name='gMhubDisplayNode' />
                    </td>
                </tr>


            </table>


            <input type='submit' value='Save Changes' id='submit_data'
                name='submit_data' />
        </form>

    </div>

</body>
</html>

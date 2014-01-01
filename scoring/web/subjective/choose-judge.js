/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$("#choose-judge-page").live(
		"pagebeforecreate",
		function(event) {

			var judges = $.subjective.getPossibleJudges();
			$.each(judges, function(i, judge) {
				$("#judges").append(
						"<input type='radio' name='judge' id='" + judge.id
								+ "' value='" + judge.id + "'>");
				$("#judges").append(
						"<label for='" + judge.id + "'>" + judge.id
								+ "</label>");
			});

			var currentJudgingGroup = $.subjective.getCurrentJudgingGroup();
			$("#judging-group").text(currentJudgingGroup);

			var currentCategory = $.subjective.getCurrentCategory();
			$("#category").text(currentCategory.title);

			var currentJudge = $.subjective.getCurrentJudge();
			if(null != currentJudge) {
				$("input:radio[value=\"" + currentJudge.id + "\"]").prop('checked', true);
			} else {
				$("input:radio[value='new-judge']").prop('checked', true);
			}
			
			var currentJudgePhone = $.subjective.getCurrentJudgePhone();
			if(null != currentJudgePhone) {
				$("#judge-phone").val(currentJudgePhone);
			}
		});

$("#choose-judge-page").live(
		"pageinit",
		function(event) {

			$("#judge-submit").click(function() {
				setJudge();
			});
			
		});

function setJudge() {
	var judgeID = $("input:radio[name='judge']:checked").val();
	if('new-judge' == judgeID) {
		judgeID = $("#new-judge-name").val();
		if(null == judgeID || "" == judgeID) {
			alert("You must enter a name");
			return;
		}
		judgeID = judgeID.toUpperCase();
	}
	
	var phone = $("#judge-phone").val();
	if(null == phone || "" == phone)
	{
		alert("You must enter a phone nunmber");
		return;
	}
	
	$.subjective.setCurrentJudge(judgeID, phone);
	
	location.href = "teams-list.html";
}

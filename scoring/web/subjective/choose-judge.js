/*
 * Copyright (c) 2014 INSciTE.  All rights reserved
 * INSciTE is on the web at: http://www.hightechkids.org
 * This code is released under GPL; see LICENSE.txt for details.
 */

$(document).on(
		"pagebeforecreate",
		"#choose-judge-page",
		function(event) {
			$("#new-judge-info").hide();

			$("#judges").empty();
			$("#judges").append(
					"<input type='radio' name='judge' id='new-judge' value='new-judge'>");
			$("#judges").append(
					"<label for='new-judge'>New Judge</label>");
			
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
			if (null != currentJudge) {
				$("input:radio[value=\"" + currentJudge.id + "\"]").prop(
						'checked', true);
			} else {
				$("input:radio[value='new-judge']").prop('checked', true);
				$("#new-judge-info").show();
			}
						
			$("input[name=judge]:radio").change(function () {
				var judgeID = $("input:radio[name='judge']:checked").val();
				if ('new-judge' == judgeID) {
					$("#new-judge-info").show();
				} else {
					$("#new-judge-info").hide();
				}
			});
			
		});

$(document).on("pageinit", "#choose-judge-page", function(event) {

	$("#judge-submit").click(function() {
		setJudge();
	});

});

function setJudge() {
	var judgeID = $("input:radio[name='judge']:checked").val();
	if ('new-judge' == judgeID) {
		judgeID = $("#new-judge-name").val();
		if (null == judgeID || "" == judgeID) {
			alert("You must enter a name");
			return;
		}
		judgeID = judgeID.toUpperCase();
		
		var phone = $("#new-judge-phone").val();
		if (null == phone || "" == phone) {
			alert("You must enter a phone nunmber");
			return;
		}
		
		$.subjective.addJudge(judgeID, phone);
	}


	$.subjective.setCurrentJudge(judgeID);

	location.href = "teams-list.html";
}

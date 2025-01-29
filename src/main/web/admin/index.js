"use strict";

function display(id) {
    document.getElementById(id).style.display = "block";
}
function hide(id) {
    document.getElementById(id).style.display = "none";
}

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("schedule_download").addEventListener("click", () => {
        window.open('ScheduleByWaveAndTeam');
        window.open('SubjectiveScheduleByJudgingStation');
        window.open('SubjectiveScheduleByCategory');
    });

});
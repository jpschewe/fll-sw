"use strict";

function display(id) {
    document.getElementById(id).style.display = "block";
}
function hide(id) {
    document.getElementById(id).style.display = "none";
}

document.addEventListener("DOMContentLoaded", function() {
    document.getElementById("schedule_download").addEventListener("click", () => {
        window.open('/admin/ScheduleByWaveAndTeam');
        window.open('/admin/SubjectiveScheduleByCategory');
        window.open('/admin/SubjectiveScheduleByTime');
        window.open('/admin/PerformanceSchedule');
        window.open('/admin/PerformanceNotes');
        window.open('/report/PitSigns');
    });

});
"use strict";

document.addEventListener("DOMContentLoaded", () => {
    setSelectValue(document.getElementById("teamNumber"), TEAM_NUMBER_HEADER);
    setSelectValue(document.getElementById("teamName"), TEAM_NAME_HEADER);
    setSelectValue(document.getElementById("organization"), ORGANIZATION_HEADER);
    setSelectValue(document.getElementById("awardGroup"), AWARD_GROUP_HEADER);
    setSelectValue(document.getElementById("judgingGroup"), JUDGE_GROUP_HEADER);
    setSelectValue(document.getElementById("wave"), WAVE_HEADER);
});

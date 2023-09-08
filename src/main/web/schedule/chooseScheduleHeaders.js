"use strict";

document.addEventListener("DOMContentLoaded", () => {
    setSelectValue(document.getElementById("teamNumber"), TEAM_NUMBER_HEADER);
    setSelectValue(document.getElementById("teamName"), TEAM_NAME_HEADER);
    setSelectValue(document.getElementById("organization"), ORGANIZATION_HEADER);
    setSelectValue(document.getElementById("awardGroup"), AWARD_GROUP_HEADER);
    setSelectValue(document.getElementById("judgingGroup"), JUDGE_GROUP_HEADER);

    for (let i = 0; i < numPracticeRounds; ++i) {
        const roundNumber = i + 1;
        setSelectValue(document.getElementById("practice" + roundNumber), practiceHeaders[i]);
        setSelectValue(document.getElementById("practiceTable" + roundNumber), practiceTableHeaders[i]);
    }

    for (let i = 0; i < numSeedingRounds; ++i) {
        const roundNumber = i + 1;
        setSelectValue(document.getElementById("perf" + roundNumber), perfHeaders[i]);
        setSelectValue(document.getElementById("perfTable" + roundNumber), perfTableHeaders[i]);
    }

});

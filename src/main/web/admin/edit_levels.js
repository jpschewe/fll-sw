const DELETE_PREFIX = "delete_";
const NEXT_PREFIX = "next_";
const ROW_PREFIX = "row_";

const NEW_LEVEL_ID_PREFIX = "new_";
var nextNewLevelIndex = 1;

const NONE_OPTION_VALUE = "none";
const NONE_OPTION_TITLE = "None";

document.addEventListener("DOMContentLoaded", function() {
    const buttons = document.getElementsByTagName("button");
    for (var i = 0; i < buttons.length; ++i) {
        const button = buttons[i];
        if (button.id && button.id.startsWith(DELETE_PREFIX)) {
            initDeleteButton(button);
        }
    }

    const inputs = document.getElementsByTagName("input");
    for (var i = 0; i < inputs.length; ++i) {
        initLevelNameInput(inputs[i]);
    }

    const addButton = document.getElementById("add");
    addButton.addEventListener('click', addNewRow);

    const saveButton = document.getElementById("save");
    saveButton.addEventListener('click', function() {
        if (validateNames()) {
            const form = document.getElementById("level_form");
            form.submit();
        }
    });

    init();
});

function selectNextLevel(levelId, nextLevelId) {
    const select = document.getElementById(NEXT_PREFIX + levelId);
    if (select) {
        select.value = nextLevelId;
    }
}


function addNewRow() {
    const newId = NEW_LEVEL_ID_PREFIX + nextNewLevelIndex;
    const newLevelName = "New Level " + nextNewLevelIndex;
    ++nextNewLevelIndex;

    addRow(newId, newLevelName);

    // default the next tournament to None
    document.getElementById(NEXT_PREFIX + newId).value = "none";
}

function addRow(newId, newLevelName) {
    const table = document.getElementById("level_table");

    const newRow = document.createElement("tr");
    table.appendChild(newRow);
    newRow.setAttribute("id", ROW_PREFIX + newId);

    const newInputCell = document.createElement("td");
    newRow.appendChild(newInputCell);

    const newInput = document.createElement("input");
    newInputCell.appendChild(newInput);
    newInput.setAttribute("type", "text");
    newInput.setAttribute("id", newId);
    newInput.value = newLevelName;
    initLevelNameInput(newInput);

    const newSelectCell = document.createElement("td");
    newRow.appendChild(newSelectCell);

    const newSelect = document.createElement("select");
    newSelectCell.appendChild(newSelect);
    newSelect.setAttribute("id", NEXT_PREFIX + newId);

    const newDeleteCell = document.createElement("td");
    newRow.appendChild(newDeleteCell);

    const newDeleteButton = document.createElement("button");
    newDeleteCell.appendChild(newDeleteButton);
    newDeleteButton.setAttribute("id", DELETE_PREFIX + newId);
    initDeleteButton(newDeleteButton);
    newDeleteButton.setAttribute("type", "button");
    newDeleteButton.innerHTML = "Delete";

    // add all existing select elements
    const inputs = document.getElementsByTagName("input");
    for (var i = 0; i < inputs.length; ++i) {
        const input = inputs[i];

        // don't include self as that is added below
        if (input != newInput) {
            const newOption = document.createElement("option");
            newOption.setAttribute("value", input.id);
            newOption.innerHTML = input.value;
            newSelect.appendChild(newOption);
        }
    }

    const noneOption = document.createElement("option");
    noneOption.setAttribute("value", NONE_OPTION_VALUE);
    noneOption.innerHTML = NONE_OPTION_TITLE;

    newSelect.appendChild(noneOption);

    // add this new level to all selects, including the new one
    addNewLevelOption(newId, newLevelName);
}

function addNewLevelOption(newId, newLevelName) {

    const inputs = document.getElementsByTagName("select");
    for (var i = 0; i < inputs.length; ++i) {
        const select = inputs[i];

        const newOption = document.createElement("option");
        newOption.setAttribute("value", newId);
        newOption.innerHTML = newLevelName;

        var added = false;
        const options = select.getElementsByTagName("option");
        var noneOption = null;
        for (var j = 0; j < options.length; ++j) {
            const option = options[j];
            if (option.value == NONE_OPTION_VALUE) {
                select.insertBefore(newOption, option);
                added = true;
                break;
            }
        }

        if (!added) {
            select.appendChild(newOption);
        }
    }

}

function initLevelNameInput(input) {
    input.addEventListener("input", function(event) {
        const levelId = input.id;
        changeLevelName(levelId, input.value);
    });
}

function initDeleteButton(button) {
    button.addEventListener('click', function() {
        const levelId = button.id.substring(DELETE_PREFIX.length, button.id.length);
        if (!levelId) {
            alert("Internal error: Cannot parse level id from button id '" + button.id + "'");
        }

        if (!canDelete(levelId)) {
            const nameInput = document.getElementById(levelId);
            const levelName = nameInput.value;
            alert("Cannot delete level \"" + levelName + "\" as it is referenced by another level");
        } else {
            const row = document.getElementById(ROW_PREFIX + levelId);
            if (row) {
                row.remove();
            }
        }
    });
}

function changeLevelName(levelId, levelNewName) {
    const inputs = document.getElementsByTagName("select");
    for (var i = 0; i < inputs.length; ++i) {
        const select = inputs[i];
        const options = select.getElementsByTagName("option");
        for (var j = 0; j < options.length; ++j) {
            const option = options[j];
            if (option.value == levelId) {
                option.innerHTML = levelNewName;
            }
        }
    }
}

function canDelete(levelId) {
    const inputs = document.getElementsByTagName("select");
    for (var i = 0; i < inputs.length; ++i) {
        const select = inputs[i];
        if (select.id) {
            const selectLevelId = select.id.substring(NEXT_PREFIX.length, select.id.length);
            // it's safe to delete a level that is referenced by itself
            if (selectLevelId != levelId && select.value == levelId) {
                return false;
            }
        } // select has id
    }
    return true;
}


/**
 * Validate the level names.
 *
 * @return true if everything is OK, false if an error was reported
 */
function validateNames() {
    const inputs = document.getElementsByTagName("input");
    for (var i = 0; i < inputs.length; ++i) {
        const input = inputs[i];
        var levelName = input.value;
        if (levelName) {
            levelName = levelName.trim();
        }
        if (!levelName) {
            alert("All levels must have non-blank names");
            return false;
        }
    }
    return true;
}

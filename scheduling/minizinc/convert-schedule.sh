#!/bin/sh

mydir=$(cd "$(dirname "$0")" && pwd -L) || exit 1

debug() { ! "${log_debug-false}" || log "DEBUG: $*" >&2; }
log() { printf '%s\n' "$*"; }
warn() { log "WARNING: $*" >&2; }
error() { log "ERROR: $*" >&2; }
fatal() { error "$*"; exit 1; }

if [ $# -lt 1 ]; then
        fatal "Usage: $0 <params.dzn>"
fi

param_file=$1
feasible=${2-0}

if [ ${feasible} -ne 0 ]; then
    flatzinc_file="${param_file}.feasible.fzn"
    schedule_solve_file="${mydir}/schedule-feasible.mzn"
else
    flatzinc_file="${param_file}.optimal.fzn"
    schedule_solve_file="${mydir}/schedule-objective.mzn"
fi

needs_update=0
if [ ! -e "${flatzinc_file}" ]; then
    needs_update=1
else
    param_file_time=$(stat --format "%Y" "${param_file}") \
        || fatal "Error executing stat on ${param_file}"
    schedule_file_time=$(stat --format "%Y" schedule.mzn) \
        || fatal "Error executing stat on schedule.mzn"
    flatzinc_file_time=$(stat --format "%Y" "${flatzinc_file}") \
        || fatal "Error executing stat on ${flatzinc_file}"
    schedule_solve_file_time=$(stat --format "%Y" "${schedule_solve_file}") \
        || fatal "Error executing stat on ${schedule_solve_file}"

    if [ ${param_file_time} -gt ${flatzinc_file_time} ]; then
        needs_update=1
    elif [ ${schedule_file_time} -gt ${flatzinc_file_time} ]; then
        needs_update=1
    elif [ ${schedule_solve_file_time} -gt ${flatzinc_file_time} ]; then
        needs_update=1
    fi
fi

if [ ${needs_update} -ne 0 ]; then
    cat "${mydir}/schedule.mzn" "${schedule_solve_file}" > "${mydir}/schedule-$$.mzn"

    log "Converting to flatzinc"
    date
    mzn2fzn --no-output-ozn --globals-dir linear \
        "${mydir}/schedule-$$.mzn" "${param_file}" \
        -o "${flatzinc_file}" \
        || fatal "Error executing mzn2fzn"
    date
    log "flatzinc file is ${flatzinc_file}"
    rm -f "${mydir}/schedule-$$.mzn"
else
    log "${flatzinc_file} is already up to date"
fi

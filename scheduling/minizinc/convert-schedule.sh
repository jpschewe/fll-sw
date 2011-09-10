#!/bin/sh

mydir=$(cd "$(dirname "$0")" && pwd -L) || exit 1

debug() { ! "${log_debug-false}" || log "DEBUG: $*" >&2; }
log() { printf '%s\n' "$*"; }
warn() { log "WARNING: $*" >&2; }
error() { log "ERROR: $*" >&2; }
fatal() { error "$*"; exit 1; }

if [ $# -ne 1 ]; then
        fatal "Usage: $0 <params.dzn>"
fi

param_file=$1
flatzinc_file="${param_file}.fzn"


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

    if [ ${param_file_time} -gt ${flatzinc_file_time} ]; then
        needs_update=1
    elif [ ${schedule_file_time} -gt ${flatzinc_file_time} ]; then
        needs_update=1
    fi
fi

if [ ${needs_update} -ne 0 ]; then
    log "Converting to flatzinc"
    date
    mzn2fzn --no-output-ozn --globals-dir linear \
        "${mydir}/schedule.mzn" "${param_file}" \
        -o "${flatzinc_file}" \
        || fatal "Error executing mzn2fzn"
    date
    log "flatzinc file is ${flatzinc_file}"
else
    log "${flatzinc_file} is already up to date"
fi

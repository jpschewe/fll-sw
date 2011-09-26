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
else
    flatzinc_file="${param_file}.fzn"
fi

"${mydir}/convert-schedule.sh" "${param_file}" ${feasible} || fatal "Error executing convert-schedule.sh"

date
log "Solving"
#flatzinc -b mip -o "${param_file}.result" "${flatzinc_file}" || fatal "Error executing flatzinc"
#log "Result is in ${param_file}.result"

# use scip
#FIXME figure out how to set limits/gap to reduce search time?
/home/jpschewe/projects/fll-sw/scip-2.0.2.linux.x86_64.gnu.opt.spx \
  -l "${flatzinc_file}.scip.log" \
  -c "read ${flatzinc_file}" \
  -c "optimize" \
  -c "write solution ${flatzinc_file}.scip.sol" \
  -c "quit"
date
log "Finished with ${param_file}"

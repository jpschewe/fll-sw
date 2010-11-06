#!/bin/sh

OPWD="${PWD}"
mypath=`dirname $0`
cd "${mypath}"
mypath="${PWD}"
cd "${OPWD}"

install_files_from_dir() {
    dir=$1
    install -d ${HOME}/.${dir}
    for file in ${temp_dir}/${dir}/*; do
        install -m 0600 ${file} ${HOME}/.${dir}
    done

}

# clean up from previous runs that may have failed
rm -fr ${mypath}/to-install

temp_dir=`mktemp -d`
#echo "Temp is ${temp_dir}"
cp -r ${mypath}/config ${temp_dir}
cp -r ${mypath}/local ${temp_dir}
for file in `find ${temp_dir}/local/share/applications/fllsw -type f -name '*.desktop'`; do
    cat ${file} | sed -e "s:/home/flluser:${HOME}:" > ${file}.new
    mv -f ${file}.new ${file}
done

# install everything, creating directories as needed
install_files_from_dir config/menus/applications-merged
install_files_from_dir local/share/applications/fllsw
install_files_from_dir local/share/desktop-directories
install_files_from_dir local/share/icons

rm -fr ${temp_dir}

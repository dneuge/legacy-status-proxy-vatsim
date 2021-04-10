#!/bin/bash

set -e

expected_license_md5='12cedd2198470f645d1a14462b8c6241'
checkout_name='vatspy-data-project'

function die {
    echo $@ >&2
    exit 1
}

# switch to script directory
basedir=$(realpath `dirname "$0"`)
cd "${basedir}"

# determine revision to be built
if [[ "$1" != "" ]]; then
    commit_hash="$1"
    echo "Using commit hash provided by command line: ${commit_hash}"
else
    echo "No commit hash provided on command line, checking VATSIM API for current release..."
    commit_hash=$(curl --silent 'https://api.vatsim.net/api/map_data/' | sed -e 's/.*"current_commit_hash":"\([^"]\+\)".*/\1/')
    echo "Commit hash resolved through VATSIM API: ${commit_hash}"
fi

if [[ ! "$commit_hash" =~ ^[0-9a-f]{40}$ ]]; then
    die "Bad format for a commit hash: ${commit_hash}"
fi

# generate POM
sed -e "s/##VERSION##/${commit_hash}/" pom.xml.template >pom.xml || die 'Failed to generate POM'

# switch to resource directory
mkdir -p src/main/resources/com/github/vatsimnetwork
cd src/main/resources/com/github/vatsimnetwork

# clone/update repository
if [ ! -e "${checkout_name}" ]; then
    git clone https://github.com/vatsimnetwork/vatspy-data-project.git "${checkout_name}" || die 'Cloning upstream project failed'
    cd "${checkout_name}"
else
    cd "${checkout_name}"
    git pull origin master || die 'Updating upstream project failed'
fi

# checkout requested revision
git checkout ${commit_hash} || die "Switching to ${commit_hash} failed"

# verify license
md5sum -c <(echo "${expected_license_md5} LICENSE") || die 'LICENSE changed'

# build Maven artifact
cd "${basedir}"
mvn clean compile package || die 'Building artifact failed'

# QA checks
[ "" == "$(jar -tf target/vatspy-data-project-${commit_hash}.jar | grep -i '\.git/')" ] || die 'QA: git repository leaked into JAR'
[ "" != "$(jar -tf target/vatspy-data-project-${commit_hash}.jar | grep 'com/github/vatsimnetwork/vatspy-data-project/VATSpy.dat')" ] || die 'QA: VATSpy.dat is missing in JAR'
[ "" != "$(jar -tf target/vatspy-data-project-${commit_hash}.jar | grep 'com/github/vatsimnetwork/vatspy-data-project/FIRBoundaries.dat')" ] || die 'QA: FIRBoundaries.dat is missing in JAR'

# install artifact
mvn install || die 'Installing artifact failed'

echo 'VAT-Spy data installed successfully.'

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

# check if artifact already exists
expected_path="$HOME/.m2/repository/_inofficial/com/github/vatsimnetwork/vatspy-data-project/${commit_hash}/vatspy-data-project-${commit_hash}.jar"
if [ -e "$expected_path" ]; then
    if [ "$FORCE_REBUILD" != "$checkout_name" ]; then
            echo "Artifact already exists at ${expected_path} - skipping build"
            echo "To force rebuilding either delete the artifact from Maven your repository or rerun with FORCE_REBUILD='${checkout_name}'"
        exit 0
    fi

    echo "Forcing rebuild although artifact already exists at ${expected_path}"
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
    git pull origin master || die 'Pulling from upstream project for in-place update failed'
    git fetch --tags origin || die 'Fetching tags from upstream project for in-place update failed'
fi

# checkout requested revision
git checkout ${commit_hash} || die "Switching to ${commit_hash} failed"

# verify license
md5sum -c <(echo "${expected_license_md5} LICENSE") || die 'LICENSE changed'

# add commit date
commit_date_file='.git_commit_date'
[[ "" == $(git ls-files | grep -F "${commit_date_file}") ]] || die "File collision: ${commit_date_file} seems to be part of the upstream repository"
(git show ${commit_hash} --format='%cI%n' | head -n1 >${commit_date_file}) || die 'Failed to add commit date'
commit_date=$(cat ${commit_date_file})

if [[ ! "${commit_date}" =~ ^20[0-9]{2}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])T([01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9][+\-][01][0-9]:[0-5][0-9]$ ]]; then
	die "Invalid commit date: \"${commit_date}\""
fi

# download FIRBoundaries.dat from GitHub release if missing
firboundaries_file='FIRBoundaries.dat'
if [ ! -e "${firboundaries_file}" ]; then
	echo "${firboundaries_file} is missing, attempting to download from GitHub..."

	tag_name=$(git describe --abbrev=0 --tags --exact-match ${commit_hash})
	[[ "${tag_name}" =~ ^v[0-9a-zA-Z\._\-]+$ ]] || die "Invalid tag name: '${tag_name}'"
	echo "Commit ${commit_hash} is referred to by tag '${tag_name}'"

	firboundaries_url=$(python3 ${basedir}/../get_github_asset_url.py vatsimnetwork/vatspy-data-project ${tag_name} ${firboundaries_file}) || die "Failed to retrieve URL for GitHub artifact ${firboundaries_file}"
	echo "Downloading ${firboundaries_file} from ${firboundaries_url}..."
	curl -L ${firboundaries_url} -o ${firboundaries_file} || die "Failed to download GitHub artifact ${firboundaries_file} from ${firboundaries_url}"

	# check for plausible file size
	minimum_expected_size=400000
	actual_size=$(wc -c <"${firboundaries_file}")
	[ $actual_size -ge $minimum_expected_size ] || die "Expected ${minimum_expected_size} bytes but ${firboundaries_file} file downloaded from ${firboundaries_url} has only ${actual_size} bytes."
	echo "Size check OK: ${firboundaries_file} has ${actual_size} bytes (required at least ${minimum_expected_size} bytes)"
fi

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

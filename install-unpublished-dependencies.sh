#!/bin/bash

set -e

vatspy_commit_hash='7d52849b5bb579fce7be8b3610c3d1e35f43ebc0'

repos=""
# https://github.com/vatplanner/dataformats-vatsim-public.git@v0.1-pre210402
# https://github.com/dneuge/web-data-retrieval.git@v0.2
    
basedir=$(realpath `dirname "$0"`)

for repo in ${repos}; do
    url=$(cut -d'@' -f1 <<< "$repo")
    tag=$(cut -d'@' -f2 <<< "$repo")
    
    tmpdir=$(mktemp -d)
    cd "$tmpdir"
    git clone $url .
    git checkout $tag
    mvn clean install
    
    rm -Rf "$tmpdir"
done

cd "${basedir}"
deps/vatspy-data-project/install.sh "${vatspy_commit_hash}"

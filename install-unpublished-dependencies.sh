#!/bin/bash

set -e

repos="
    https://github.com/dneuge/web-data-retrieval.git@v0.2
    https://github.com/vatplanner/dataformats-vatsim-public.git@v0.1-pre210402
"

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


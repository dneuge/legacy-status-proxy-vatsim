#!/bin/env python3

import json
import urllib.request
import sys

project = sys.argv[1]
wanted_tag = sys.argv[2]
wanted_asset = sys.argv[3]

url_releases = 'https://api.github.com/repos/%s/releases' % project
releases = json.load(urllib.request.urlopen(url_releases))

url_asset = None
for release in releases:
    if release['tag_name'] != wanted_tag:
        continue

    if release['draft']:
        print('Release %d matches tag %s but has draft status, ignoring...' % (release['id'], release['tag_name']), file=sys.stderr)
        continue
    
    if release['prerelease']:
        print('Release %d matches tag %s but has prerelease status, ignoring...' % (release['id'], release['tag_name']), file=sys.stderr)
        continue
    
    for asset in release['assets']:
        if asset['name'] != wanted_asset:
            continue

        if url_asset is not None:
            print('Multiple assets found named %s for tag %s, unable to determine correct URL.' % (asset['name'], release['tag_name']), file=sys.stderr)
            sys.exit(1)
        
        url_asset = asset['browser_download_url']

if url_asset is None:
    print('Asset %s for tag %s could not be found for project %s.' % (wanted_asset, wanted_tag, project), file=sys.stderr)
    sys.exit(1)

print(url_asset)

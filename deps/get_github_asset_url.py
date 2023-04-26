#!/bin/env python3

import json
import urllib.request
import sys

project = sys.argv[1]
wanted_tag = sys.argv[2]
wanted_asset = sys.argv[3]

def find_asset_url(release, wanted_name):
    found = None
    if release is not None:
        for asset in release['assets']:
            if asset['name'] != wanted_name:
                continue

            if url_asset is not None:
                print('Multiple assets found named %s for tag %s, unable to determine correct URL.' % (asset['name'], release['tag_name']), file=sys.stderr)
                sys.exit(1)
        
            found = asset['browser_download_url']

    return found


url_asset = None

url_tag_release = 'https://api.github.com/repos/%s/releases/tags/%s' % (project, wanted_tag)
print('Searching for release by tag name using %s' % (url_tag_release), file=sys.stderr)
release = json.load(urllib.request.urlopen(url_tag_release))
if release is None or len(release) <= 0:
    print('... release not found by tag', file=sys.stderr)
else:
    url_asset = find_asset_url(release, wanted_asset)
    if url_asset is  None:
        print('... asset not found in tag release', file=sys.stderr)
    else:
        print(url_asset)
        sys.exit(0)

url_releases = 'https://api.github.com/repos/%s/releases' % project
print('Listing all releases using %s' % (url_releases), file=sys.stderr)
releases = json.load(urllib.request.urlopen(url_releases))

for release in releases:
    if release['tag_name'] != wanted_tag:
        continue

    if release['draft']:
        print('Release %d matches tag %s but has draft status, ignoring...' % (release['id'], release['tag_name']), file=sys.stderr)
        continue
    
    if release['prerelease']:
        print('Release %d matches tag %s but has prerelease status, ignoring...' % (release['id'], release['tag_name']), file=sys.stderr)
        continue

    url_asset = find_asset_url(release, wanted_asset)
    if url_asset is not None:
        break

if url_asset is None:
    print('Asset %s for tag %s could not be found for project %s.' % (wanted_asset, wanted_tag, project), file=sys.stderr)
    sys.exit(1)

print(url_asset)

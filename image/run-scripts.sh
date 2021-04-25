#!/usr/bin/env bash

COLLECTION_REPORT="collection-report.txt"
KTLINT_REPORT="ktlint-report.json"
EMPTY_KTLINT_REPORT=$'[\n]'

cp /executeCollectPrChanges $GITHUB_WORKSPACE
cp /ktlint $GITHUB_WORKSPACE
cp /executeMakePrComments $GITHUB_WORKSPACE

echo 'Collecting PR changes...'
./executeCollectPrChanges $GITHUB_EVENT_PATH $INPUT_REPOTOKEN

collection_result=$?
if [ $collection_result -ne 0 ]; then
  echo 'There was an error while collecting PR changes'
  exit $collection_result
fi

test -s $COLLECTION_REPORT
collection_is_empty=$?
if [ $collection_is_empty -ne 0 ]; then
  echo 'There were no changes in .kt files'
  exit 0
fi

echo '------------------------'
echo ''


# Strip out any --reporter arguments supplied by the user
INPUT_ARGUMENTS=${INPUT_ARGUMENTS/--reporter*[[:space:]]/}
echo "::debug::INPUT_ARGUMENTS=$INPUT_ARGUMENTS"

echo 'Running ktlint...'
echo "::debug::$COLLECTION_REPORT=$(cat $COLLECTION_REPORT)"
./ktlint $(cat $COLLECTION_REPORT | awk 'BEGIN { ORS=" " }; {print $1}') --reporter=json,output=$KTLINT_REPORT $INPUT_ARGUMENTS

echo "::debug::$KTLINT_REPORT=$(cat $KTLINT_REPORT)"

if [ "$(cat $KTLINT_REPORT)" = "$EMPTY_KTLINT_REPORT" ]; then
  echo 'There are no errors in these files'
  exit 0
fi

echo '-----------------'
echo ''


echo 'Make comments in PR...'
./executeMakePrComments $GITHUB_EVENT_PATH $INPUT_REPOTOKEN
echo '----------------------'
echo ''

if [ $INPUT_FAILONCOMMENTS == "yes" ]; then
    echo 'Fail on comments is enabled'
    exit -1
fi
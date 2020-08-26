#!/usr/bin/env bash

rm executeCollectPrChanges
rm executeMakePrComments

kscript --package src/main/kotlin/executeCollectPrChanges.kts
kscript --package src/main/kotlin/executeMakePrComments.kts
#!/bin/bash
set -e
pushd sample-app || exit
rm -rf build*
./package-lambda-function.sh
popd || exit

pushd terraform || exit
terraform init
terraform apply -auto-approve
popd || exit
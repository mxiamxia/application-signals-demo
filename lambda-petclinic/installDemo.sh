#!/bin/bash
set -e
pushd sample-apps2 || exit
rm -rf build*
./package-lambda-function.sh
popd || exit

pushd terraform2 || exit
terraform init
terraform apply -auto-approve
popd || exit
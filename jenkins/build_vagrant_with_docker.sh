#!/bin/bash

# Copyright (c) 2016, CodiLime Inc.
#
# Usage ./build_vagrant_with_docker.sh TAG

set -e

# Check if number of parameters is correct
if [ $# != 1 ]; then
  echo ">>> Exactly one parameters must be provided."
  exit 1
fi


# Settings
SEAHORSE_BUILD_TAG=$1
ARTIFACT_NAME="docker-compose-internal.yml"
COMPOSE_FILE="http://artifactory.deepsense.codilime.com:8081/artifactory/seahorse-distribution/io/deepsense/$SEAHORSE_BUILD_TAG/dockercompose/$ARTIFACT_NAME"
VAGRANT_BOX_NAME="seahorse-vm"
PUBLISH_DIR="../image_publication"

# Download docker-compose config file
cd ../deployment/vagrant_with_docker
rm -f $ARTIFACT_NAME
wget $COMPOSE_FILE
mv $ARTIFACT_NAME docker-compose.yml

# Create Vagrant box
vagrant destroy -f $VAGRANT_BOX_NAME
vagrant up $VAGRANT_BOX_NAME
rm -f $PUBLISH_DIR/$VAGRANT_BOX_NAME.box
vagrant package --output $PUBLISH_DIR/$VAGRANT_BOX_NAME.box
vagrant destroy -f $VAGRANT_BOX_NAME
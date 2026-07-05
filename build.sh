#!/bin/bash
docker compose run --rm --user $(id -u):$(id -g) android ./gradlew --no-daemon :app:distApk 2>&1 | tail -20

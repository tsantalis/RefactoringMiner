#!/bin/sh

if [ -z "$GITHUB_TOKEN" ]; then
  echo "Missing GITHUB_TOKEN"
  exit 1
fi

echo "OAuthToken=$GITHUB_TOKEN" > /app/github-oauth.properties

exec java -jar /app/app.jar

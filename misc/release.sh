#!/bin/bash

# This script will
# - remove -SNAPSHOT from the current version in build.gradle.kts, commit that change
# - create a tag for this version
# - increase the version in build.gradle.kts to the next -SNAPSHOT version by increasing the patch version
# 
# Several checks whether
# - we are on the master branch
# - local and remote branches are the same
# - there are no unversioned files
# - there are no uncommitted changes
# - there are no staged files
# - the tag to be created exists neither locally nor remotely

DIRECTORY=`dirname "$0"`
cd "$DIRECTORY/.."

BRANCH=`git rev-parse --abbrev-ref HEAD`
if [ "$BRANCH" != "master" ]; then
	echo "Not on branch 'master'"
	exit 1
fi

git remote update || exit 1

# https://stackoverflow.com/a/3278427/8569278
UPSTREAM=${1:-'@{u}'}
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse "$UPSTREAM")
BASE=$(git merge-base @ "$UPSTREAM")

if [ "$LOCAL" == "$REMOTE" ]; then
	echo "Local and remote branches are up-to-date"
elif [ "$LOCAL" == "$BASE" ]; then
	echo "Need to pull"
	exit 1
elif [ "$REMOTE" == "$BASE" ]; then
	echo "Need to push"
	exit 1
else
	echo "Locale and remote branches are diverged"
	exit 1
fi

# https://stackoverflow.com/questions/3801321/git-list-only-untracked-files-also-custom-commands
UNTRACKED_FILES=`git add -A -n | wc -l`
if [ "$UNTRACKED_FILES" -ne "0" ]; then
	echo "There are untracked files"
	exit 1
fi

MODIFIED_FILES=`git status --porcelain | grep -c '^ M'`
if [ "$MODIFIED_FILES" -ne "0" ]; then
	echo "There are modified files"
	exit 1
fi

# https://stackoverflow.com/questions/33610682/git-list-of-staged-files
UNTRACKED_FILES=`git diff --name-only --cached | wc -l`
if [ "$UNTRACKED_FILES" -ne "0" ]; then
	echo "There are files staged for commit"
	exit 1
fi

CURRENT_VERSION=`grep '^version = "[0-9]\+\.[0-9]\+\.[0-9]\+-SNAPSHOT"' build.gradle.kts | sed -e 's/^.* "//;s/-SNAPSHOT"$//'`
if [ "$CURRENT_VERSION" == "" ]; then
	echo "Unable to determine current version (not -SNAPSHOT or wrong pattern?)"
	exit 1
fi
echo "Current version: $CURRENT_VERSION"

CURRENT_PATCH=`echo $CURRENT_VERSION | sed -e 's/^.*\.//'`
NEW_PATCH=$((CURRENT_PATCH + 1))

WITHOUT_PATCH=`echo $CURRENT_VERSION | sed -e 's/\.[^\.]\+$//'`
NEW_VERSION="$WITHOUT_PATCH.$NEW_PATCH"

echo "New version: $NEW_VERSION"

LOCAL_TAG_EXISTS=`git tag -l "v$CURRENT_VERSION" | wc -l`
REMOTE_TAG_EXISTS=`git ls-remote --tags 2> /dev/null | grep -c "refs/tags/v$CURRENT_VERSION$"`
if [ "$LOCAL_TAG_EXISTS" -ne "0" ]; then
	echo "Local tag exists"
	exit 1
fi
if [ "$REMOTE_TAG_EXISTS" -ne "0" ]; then
	echo "Remote tag exists"
	exit 1
fi

echo "All checks passed. Hit enter to continue."
read

sed -i "s/version = \".*\"/version = \"$CURRENT_VERSION\"/" build.gradle.kts || exit 1
git add build.gradle.kts || exit 1
git commit -m "change version to $CURRENT_VERSION" || exit 1
git tag "v$CURRENT_VERSION" || exit 1

sed -i "s/version = \".*\"/version = \"$NEW_VERSION-SNAPSHOT\"/" build.gradle.kts || exit 1
git add build.gradle.kts || exit 1
git commit -m "change version to $NEW_VERSION-SNAPSHOT" || exit 1

git push || exit 1
git push origin "v$CURRENT_VERSION" || exit 1


#!/bin/bash

PROJECT_NAME=$1
AI_TOOL=$2

SKILLS_REPO="https://github.com/joshsoftware/ai-assistant-skilles.git"
TEMP_DIR="/tmp/ai-assistant-skills"

if [ -z "$PROJECT_NAME" ]; then
    echo ""
    echo "Usage:"
    echo "./create_flutter_project.sh <project_name> <cursor|claude>"
    exit 1
fi

if [ -z "$AI_TOOL" ]; then
    AI_TOOL="cursor"
fi

if command -v fvm >/dev/null 2>&1; then
    FLUTTER_CMD="fvm flutter"
elif command -v flutter >/dev/null 2>&1; then
    FLUTTER_CMD="flutter"
else
    echo "Flutter not found."
    exit 1
fi

$FLUTTER_CMD create "$PROJECT_NAME"

git clone "$SKILLS_REPO" "$TEMP_DIR"

if [ "$AI_TOOL" = "cursor" ]; then

    mkdir -p "$PROJECT_NAME/.cursor/skills"

    cp "$TEMP_DIR/flutter/AGENTS.md" \
       "$PROJECT_NAME/AGENTS.md"

    cp -R \
       "$TEMP_DIR/flutter/skills/"* \
       "$PROJECT_NAME/.cursor/skills/"

fi

if [ "$AI_TOOL" = "claude" ]; then

    mkdir -p "$PROJECT_NAME/.claude/skills"

    cp "$TEMP_DIR/flutter/CLAUDE.md" \
       "$PROJECT_NAME/CLAUDE.md"

    cp -R \
       "$TEMP_DIR/flutter/skills/"* \
       "$PROJECT_NAME/.claude/skills/"

fi

rm -rf "$TEMP_DIR"

echo ""
echo "Project Created Successfully"
echo ""
echo "cd $PROJECT_NAME"

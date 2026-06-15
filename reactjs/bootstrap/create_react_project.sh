#!/bin/bash
#
# create_react_project.sh
#
# Creates a new BFSI React project. The boilerplate is NOT cloned wholesale —
# instead this script:
#   1. generates a fresh Vite + React + TypeScript app with npm (non-interactive
#      so it does NOT auto-install + launch the dev server), and
#   2. copies the `.claude` toolkit (skills/agents/commands/hooks) AND a starter
#      `CLAUDE.md` from this git repo into the project.
#
# The complete boilerplate (folder structure, http/pii/i18n primitives, login
# reference feature, configs, husky) is GENERATED later by running the
# `bfsi-bootstrap` skill — see setup_architecture.sh.
#
# Uses npm only.
#
# Usage:
#   ./create_react_project.sh <project_name> [claude|cursor]
#
# Env overrides:
#   BFSI_BRANCH   branch to clone the toolkit from   (default: main)
#   BFSI_REPO     repo to clone                       (default: joshsoftware/ai-assistant-skilles)
#   NO_INSTALL=1  skip `npm install`
#

set -euo pipefail

PROJECT_NAME="${1:-}"
AI_TOOL="${2:-claude}"

SKILLS_REPO="${BFSI_REPO:-https://github.com/joshsoftware/ai-assistant-skilles.git}"
SKILLS_BRANCH="${BFSI_BRANCH:-main}"
BFSI_SUBDIR="reactjs/bfsi-boilerplate"
TEMP_DIR="$(mktemp -d -t ai-assistant-skills-XXXXXX)"

cleanup() { rm -rf "$TEMP_DIR"; }
trap cleanup EXIT

# ---- arg validation --------------------------------------------------------

if [ -z "$PROJECT_NAME" ]; then
    echo ""
    echo "Usage:"
    echo "  ./create_react_project.sh <project_name> [claude|cursor]"
    echo ""
    exit 1
fi

if [ "$AI_TOOL" != "claude" ] && [ "$AI_TOOL" != "cursor" ]; then
    echo "Error: AI tool must be 'claude' or 'cursor' (got '$AI_TOOL')."
    exit 1
fi

if [ -e "$PROJECT_NAME" ]; then
    echo "Error: '$PROJECT_NAME' already exists in $(pwd)."
    exit 1
fi

# ---- prerequisites ---------------------------------------------------------

command -v git >/dev/null 2>&1 || { echo "git is required."; exit 1; }
command -v npm >/dev/null 2>&1 || { echo "npm is required."; exit 1; }

# ---- 1. generate the base Vite + React + TS app ----------------------------

echo "→ Creating Vite + React + TS app '$PROJECT_NAME' with npm (non-interactive)..."
npm create vite@latest "$PROJECT_NAME" -- --template react-ts --no-interactive

# ---- 2. add the AI toolkit (.claude/ + CLAUDE.md) --------------------------

echo "→ Cloning the toolkit from $SKILLS_REPO ($SKILLS_BRANCH)..."
git clone --depth 1 --branch "$SKILLS_BRANCH" --filter=blob:none --sparse \
    "$SKILLS_REPO" "$TEMP_DIR" >/dev/null 2>&1 \
    || { echo "Error: clone failed (branch '$SKILLS_BRANCH'?)."; exit 1; }
# Pull the whole bfsi-boilerplate subdir so we get both .claude/ and CLAUDE.md.
git -C "$TEMP_DIR" sparse-checkout set "$BFSI_SUBDIR" >/dev/null 2>&1

SRC="$TEMP_DIR/$BFSI_SUBDIR/.claude"
SRC_CLAUDE_MD="$TEMP_DIR/$BFSI_SUBDIR/CLAUDE.md"
if [ ! -d "$SRC" ]; then
    echo "Error: '$BFSI_SUBDIR/.claude' not found in the repo."
    exit 1
fi
if [ ! -f "$SRC_CLAUDE_MD" ]; then
    echo "Error: '$BFSI_SUBDIR/CLAUDE.md' not found in the repo."
    exit 1
fi

if [ "$AI_TOOL" = "claude" ]; then
    echo "→ Installing Claude toolkit into .claude/ + CLAUDE.md ..."
    cp -R "$SRC" "$PROJECT_NAME/.claude"
    # CLAUDE.md is copied as-is; the bfsi-bootstrap skill refines it later
    # (setup_architecture.sh). Placeholders like @<scope> stay until then.
    cp "$SRC_CLAUDE_MD" "$PROJECT_NAME/CLAUDE.md"
else
    echo "→ Installing Cursor toolkit (.cursor/skills + AGENTS.md) ..."
    mkdir -p "$PROJECT_NAME/.cursor/skills"
    [ -d "$SRC/skills" ] && cp -R "$SRC/skills/." "$PROJECT_NAME/.cursor/skills/"
    # Surface the bootstrap skill's intent as AGENTS.md for Cursor users.
    if [ -f "$SRC/skills/bfsi-bootstrap/SKILL.md" ]; then
        cp "$SRC/skills/bfsi-bootstrap/SKILL.md" "$PROJECT_NAME/AGENTS.md"
    fi
fi

find "$PROJECT_NAME" -name ".DS_Store" -type f -delete 2>/dev/null || true

# ---- 3. install base dependencies ------------------------------------------

if [ "${NO_INSTALL:-}" = "1" ]; then
    echo "→ Skipping install (NO_INSTALL=1)."
else
    echo "→ Installing base dependencies with npm..."
    (cd "$PROJECT_NAME" && npm install)
fi

# ---- done ------------------------------------------------------------------

echo ""
if [ "$AI_TOOL" = "claude" ]; then
    echo "✓ Base project '$PROJECT_NAME' created (Vite + React + TS + .claude/ + CLAUDE.md)."
else
    echo "✓ Base project '$PROJECT_NAME' created (Vite + React + TS + .cursor toolkit + AGENTS.md)."
fi
echo ""
echo "Next — generate the full BFSI boilerplate (the bfsi-bootstrap skill is already in .claude/):"
echo ""
echo "  cd $PROJECT_NAME"
if [ "$AI_TOOL" = "claude" ]; then
    echo "  claude     # then say: \"set up the BFSI boilerplate\""
    echo ""
    echo "  # ...or run it headlessly with the companion script (same GitHub link as this one):"
    echo "  curl -O https://raw.githubusercontent.com/joshsoftware/ai-assistant-skilles/main/reactjs/bootstrap/setup_architecture.sh"
    echo "  bash setup_architecture.sh ."
else
    echo "  cursor .   # then ask Cursor to follow AGENTS.md"
fi
echo ""
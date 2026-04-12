#!/bin/bash
# Code quality check script for Fil
#
# Checks:
# 1. Validates commit message format ([patch], [minor], [major])
# 2. Automatically updates version based on commit prefix
# 3. Ensures file length stays under 500 lines
# 4. Ensures TODO comments are categorized
# 5. Compilation check (when gradlew is available)

REPO_ROOT="$(git rev-parse --show-toplevel)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ============================================================================
# PART 1: Validate Commit Message Format
# ============================================================================
validate_commit_message() {
  COMMIT_MSG_FILE="$1"

  if [ -z "$COMMIT_MSG_FILE" ] || [ ! -f "$COMMIT_MSG_FILE" ]; then
    COMMIT_MSG_FILE="$REPO_ROOT/.git/COMMIT_EDITMSG"
    if [ ! -f "$COMMIT_MSG_FILE" ]; then
      return 0
    fi
  fi

  # Skip validation for merge commits
  if [ -f "$REPO_ROOT/.git/MERGE_HEAD" ]; then
    echo -e "${GREEN}✓ Merge commit detected - skipping version prefix check${NC}"
    return 0
  fi

  COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")

  if echo "$COMMIT_MSG" | head -1 | grep -qE '^\[(patch|minor|major)\]'; then
    echo -e "${GREEN}✓ Commit message has valid version prefix${NC}"
    return 0
  fi

  echo -e "${RED}✗ Error: Commit message must start with a version prefix${NC}"
  echo ""
  echo "Valid prefixes:"
  echo -e "  ${YELLOW}[patch]${NC} - Bug fixes, small changes (0.0.X)"
  echo -e "  ${YELLOW}[minor]${NC} - New features, backwards compatible (0.X.0)"
  echo -e "  ${YELLOW}[major]${NC} - Breaking changes (X.0.0)"
  echo ""
  echo "Example:"
  echo -e "  ${GREEN}[patch] Fix gait asymmetry threshold edge case${NC}"
  echo -e "  ${GREEN}[minor] Add Symbol-Digit micro-test${NC}"
  echo -e "  ${GREEN}[major] Redesign relapse detection engine${NC}"
  echo ""
  echo "Your commit message was:"
  echo -e "  ${RED}$(head -1 "$COMMIT_MSG_FILE")${NC}"
  echo ""
  return 1
}

# ============================================================================
# PART 2: Update Version Based on Commit Message
# ============================================================================
update_version() {
  COMMIT_MSG_FILE="$1"
  echo ""
  echo -e "${CYAN}Checking version update...${NC}"

  if [ -f "$REPO_ROOT/.git/MERGE_HEAD" ]; then
    echo -e "${YELLOW}⚠ Merge commit detected - skipping version update${NC}"
    return 0
  fi

  VERSION_FILE="$REPO_ROOT/VERSION"
  BUILD_GRADLE="$REPO_ROOT/android/app/build.gradle.kts"

  if [ ! -f "$VERSION_FILE" ]; then
    echo -e "${RED}✗ Error: VERSION file not found${NC}"
    return 1
  fi

  CURRENT_VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')

  IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
  MAJOR=${MAJOR:-0}
  MINOR=${MINOR:-0}
  PATCH=${PATCH:-0}

  LAST_VERSION_COMMIT=$(git log -1 --format="%H" -- "$VERSION_FILE" 2>/dev/null || echo "")

  if [ -z "$LAST_VERSION_COMMIT" ]; then
    echo -e "${GREEN}✓ No previous version history (first version commit)${NC}"
    echo -e "${GREEN}✓ Version remains: ${CURRENT_VERSION}${NC}"
    return 0
  else
    COMMIT_RANGE="$LAST_VERSION_COMMIT..HEAD"
  fi

  COMMITS=$(git log --pretty=format:"%s" "$COMMIT_RANGE" 2>/dev/null | grep -v "^\[version\]" || echo "")

  CURRENT_COMMIT_MSG=""
  if [ -n "$COMMIT_MSG_FILE" ] && [ -f "$COMMIT_MSG_FILE" ]; then
    CURRENT_COMMIT_MSG="$(head -1 "$COMMIT_MSG_FILE")"
    if echo "$CURRENT_COMMIT_MSG" | grep -qE '^\[version\]'; then
      CURRENT_COMMIT_MSG=""
    fi
  fi

  if [ -z "$COMMITS" ] && [ -z "$CURRENT_COMMIT_MSG" ]; then
    echo -e "${GREEN}✓ No version update needed (no commits since last version)${NC}"
    return 0
  fi

  MAJOR_COUNT=0
  MINOR_COUNT=0
  PATCH_COUNT=0

  while IFS= read -r commit_msg; do
    [ -z "$commit_msg" ] && continue

    if echo "$commit_msg" | grep -qE '^\[major\]'; then
      MAJOR_COUNT=$((MAJOR_COUNT + 1))
    elif echo "$commit_msg" | grep -qE '^\[minor\]'; then
      MINOR_COUNT=$((MINOR_COUNT + 1))
    elif echo "$commit_msg" | grep -qE '^\[patch\]'; then
      PATCH_COUNT=$((PATCH_COUNT + 1))
    fi
  done <<< "$(printf "%s\n%s" "$COMMITS" "$CURRENT_COMMIT_MSG")"

  NEW_MAJOR=$MAJOR
  NEW_MINOR=$MINOR
  NEW_PATCH=$PATCH
  BUMP_TYPE=""

  if [ $MAJOR_COUNT -gt 0 ]; then
    NEW_MAJOR=$((MAJOR + 1))
    NEW_MINOR=0
    NEW_PATCH=0
    BUMP_TYPE="major"
  elif [ $MINOR_COUNT -gt 0 ]; then
    NEW_MAJOR=$MAJOR
    NEW_MINOR=$((MINOR + 1))
    NEW_PATCH=0
    BUMP_TYPE="minor"
  elif [ $PATCH_COUNT -gt 0 ]; then
    NEW_MAJOR=$MAJOR
    NEW_MINOR=$MINOR
    NEW_PATCH=$((PATCH + 1))
    BUMP_TYPE="patch"
  fi

  if [ -z "$BUMP_TYPE" ]; then
    echo -e "${GREEN}✓ No version update needed (no version-tagged commits)${NC}"
    return 0
  fi

  NEW_VERSION="$NEW_MAJOR.$NEW_MINOR.$NEW_PATCH"

  if [ "$NEW_VERSION" == "$CURRENT_VERSION" ]; then
    echo -e "${GREEN}✓ Version already up to date: ${CURRENT_VERSION}${NC}"
    return 0
  fi

  echo -e "${CYAN}Detected ${BUMP_TYPE} changes${NC}"
  echo -e "${CYAN}Updating version: ${CURRENT_VERSION} → ${NEW_VERSION}${NC}"

  # Update VERSION file
  echo "$NEW_VERSION" > "$VERSION_FILE"

  # Update build.gradle.kts if it exists
  if [ -f "$BUILD_GRADLE" ]; then
    sed -i "s/versionName = \"[^\"]*\"/versionName = \"$NEW_VERSION\"/" "$BUILD_GRADLE"
    VERSION_CODE=$((NEW_MAJOR * 10000 + NEW_MINOR * 100 + NEW_PATCH))
    sed -i "s/versionCode = [0-9]*/versionCode = $VERSION_CODE/" "$BUILD_GRADLE"
    echo -e "${GREEN}✓ Updated android/app/build.gradle.kts (versionName=$NEW_VERSION, versionCode=$VERSION_CODE)${NC}"
    git add "$BUILD_GRADLE"
  fi

  git add "$VERSION_FILE"
  echo -e "${GREEN}✓ Version updated and staged${NC}"
  echo ""

  return 0
}

# ============================================================================
# PART 3: File Length Check
# ============================================================================
check_file_length() {
  echo ""
  echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║  Pre-commit: Checking file length (max 500 lines)         ║${NC}"
  echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""

  MAX_LINES=500
  FAILED=0

  for file in $(git diff --cached --name-only --diff-filter=ACM); do
    case "$file" in
      *.md|*.lock|*.png|*.jpg|*.jpeg|*.gif|*.ico|*.svg|*.webp) continue ;;
    esac

    if [ -f "$REPO_ROOT/$file" ]; then
      LINE_COUNT=$(wc -l < "$REPO_ROOT/$file")
      if [ "$LINE_COUNT" -gt "$MAX_LINES" ]; then
        echo -e "${RED}✗ $file has $LINE_COUNT lines (max: $MAX_LINES)${NC}"
        FAILED=1
      fi
    fi
  done

  if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All staged files are within the $MAX_LINES line limit${NC}"
    return 0
  else
    echo ""
    echo -e "${RED}✗ Some files exceed the maximum line limit${NC}"
    echo -e "${YELLOW}Consider splitting large files into smaller, focused modules${NC}"
    return 1
  fi
}

# ============================================================================
# PART 4: TODO Categorization Check
# ============================================================================
check_todos() {
  echo ""
  echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║  Pre-commit: Checking TODO categorization                 ║${NC}"
  echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""

  FAILED=0

  for file in $(git diff --cached --name-only --diff-filter=ACM); do
    case "$file" in
      *.kt|*.kts|*.java|*.xml) ;;
      *) continue ;;
    esac

    if [ -f "$REPO_ROOT/$file" ]; then
      UNCATEGORIZED=$(grep -n 'TODO[^(]' "$REPO_ROOT/$file" 2>/dev/null | grep -v 'TODO(' || true)
      if [ -n "$UNCATEGORIZED" ]; then
        echo -e "${RED}✗ Uncategorized TODO(s) in $file:${NC}"
        echo "$UNCATEGORIZED" | while read -r line; do
          echo -e "  ${YELLOW}$line${NC}"
        done
        FAILED=1
      fi
    fi
  done

  if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All TODO/FIXME comments are properly categorized${NC}"
    return 0
  else
    echo ""
    echo -e "${YELLOW}Please categorize TODO comments using:${NC}"
    echo -e "  ${GREEN}TODO(critical)${NC} - Must fix before next release"
    echo -e "  ${GREEN}TODO(performance)${NC} - Performance optimization needed"
    echo -e "  ${GREEN}TODO(security)${NC} - Security improvement needed"
    echo -e "  ${GREEN}TODO(refactor)${NC} - Code quality improvement"
    return 1
  fi
}

# ============================================================================
# PART 5: Compilation Check
# ============================================================================
check_compilation() {
  echo ""
  echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║  Pre-commit: Compiling app before commit                  ║${NC}"
  echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
  echo ""

  GRADLEW_PATH="$REPO_ROOT/android/gradlew"

  if [ ! -f "$GRADLEW_PATH" ]; then
    echo -e "${YELLOW}⚠ android/gradlew not found - skipping compilation check${NC}"
    return 0
  fi

  chmod +x "$GRADLEW_PATH"

  echo -e "${CYAN}Running: ./gradlew assembleDebug${NC}"
  echo ""

  if (cd "$REPO_ROOT/android" && ./gradlew assembleDebug --no-daemon --quiet); then
    echo ""
    echo -e "${GREEN}✓ Compilation successful - proceeding with commit${NC}"
    return 0
  else
    echo ""
    echo -e "${RED}✗ Compilation failed - commit aborted${NC}"
    echo -e "${YELLOW}Please fix compilation errors before committing${NC}"
    return 1
  fi
}

# ============================================================================
# Main execution
# ============================================================================
main() {
  COMMIT_MSG_FILE="$1"

  # If commit-msg hook: validate message + update version
  if [ -n "$COMMIT_MSG_FILE" ] && [ -f "$COMMIT_MSG_FILE" ]; then
    validate_commit_message "$COMMIT_MSG_FILE" || return 1
    update_version "$COMMIT_MSG_FILE" || return 1
    return 0
  fi

  # Pre-commit: run quality gates
  check_file_length || return 1
  check_todos || return 1
  check_compilation || return 1
  return 0
}

main "$@"

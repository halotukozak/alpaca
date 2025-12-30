# Automatic Pull Request Description

This document explains the automatic PR description generation feature for the Alpaca project.

## Overview

The Alpaca repository includes an AI-powered automated system that generates descriptions for pull requests when they don't have one. This helps reviewers quickly understand what changes are included in a PR without having to manually analyze commits and diffs.

## How It Works

### Automatic Trigger

When a pull request is created or edited, the system automatically:
1. Checks if the PR has a meaningful description (> 50 characters of non-boilerplate content)
2. If the description is minimal or empty, generates an automatic description using AI
3. Updates the PR with the generated description

**This happens automatically** - no manual intervention is needed.

### AI-Powered Generation

The workflow uses **GitHub Copilot / AI models** to generate intelligent, context-aware PR descriptions:

#### Primary Method: AI Generation
- Uses GitHub Models API with GPT-4 for intelligent description generation
- Analyzes:
  - PR title
  - Commit messages
  - Changed files and their statistics
  - Code diff (first 5000 characters)
- Generates a natural, human-like description that explains:
  - What changed
  - Why it changed
  - Key technical details
  - Impact of the changes

#### Fallback Method: Template-Based
If AI generation fails (API unavailable, rate limits, etc.), the system falls back to template-based generation that includes:

1. **Change Type Detection** - Analyzes the PR title and commit messages to identify:
   - Bug fixes
   - New features
   - Refactoring
   - Documentation updates
   - Testing improvements
   - Performance optimizations
   - Build/CI changes

2. **Summary Section** - Lists all unique commit messages (up to 5)

3. **Changes Statistics** - Number of files changed, additions, and deletions

4. **Affected Files** - Categorizes and lists modified files by type:
   - **Source files** - Scala/Java source code
   - **Test files** - Test files and specs
   - **Documentation** - Markdown and documentation files
   - **Configuration** - Build configs, workflow files, etc.

### Example Generated Descriptions

#### AI-Generated (Primary):

```markdown
## AI-Generated Description

This pull request introduces an automatic PR description generation workflow that leverages
AI to create meaningful descriptions when PRs are opened without one. The implementation
adds a GitHub Actions workflow that triggers on PR creation/editing and uses the GitHub
Models API to analyze commits, file changes, and diffs to generate context-aware descriptions.

### Key Changes

- New workflow file `.github/workflows/pr-auto-description.yml` implements the automation
- Comprehensive documentation in `.github/PR_AUTO_DESCRIPTION.md` explains usage
- Fallback template-based generation for when AI is unavailable
- Smart detection to avoid overwriting existing meaningful descriptions

### Technical Details

The workflow checks for minimal descriptions (< 50 chars), fetches PR context including
commits and file changes, and calls the GitHub Models API with GPT-4 to generate intelligent
descriptions. If AI generation fails, it falls back to a template-based approach.

---

### Statistics

- **2** files changed
- **436** additions
- **0** deletions
```

#### Template-Based (Fallback):

```markdown
## Automatic Description

This pull request includes feature, testing changes.

### Summary

- Add automatic PR description generation workflow
- Create documentation for PR auto-description
- Add tests for description generation logic

### Changes

- **3** files changed
- **245** additions
- **12** deletions

### Affected Files

**Source files:**
- `.github/workflows/pr-auto-description.yml`

**Documentation:**
- `.github/PR_AUTO_DESCRIPTION.md`
```

## When Auto-Description Runs

The workflow runs when:
- A pull request is **opened**
- A pull request is **edited**

The workflow **does NOT run** for:
- PRs from `dependabot[bot]` (they have their own descriptions)
- PRs from `github-actions[bot]`

## When Auto-Description is Skipped

The system skips generating a description if:
- The PR already has a description longer than 50 characters (excluding boilerplate)
- The PR is from a bot that provides its own descriptions

## Preserving Existing Content

The workflow preserves:
- Copilot agent prompt sections (marked with `<!-- START COPILOT`)
- Other special HTML comments or sections
- Any manually added content after the automatic section

## Manual Override

You can provide your own description by:

1. **Before creating the PR**: Add a description in the PR template or creation form
2. **After auto-generation**: Simply edit the PR description and replace or add to the generated content
   - If you add significant content (> 50 chars), the workflow won't overwrite it on subsequent edits

## Customization

### Adjusting the Detection Threshold

To change when a description is considered "minimal", edit the workflow file. In the script section, find and modify:

```javascript
// In .github/workflows/pr-auto-description.yml
// Around line 46 in the script section
let hasMinimalDescription = currentBody.trim().length < 50;  // Change 50 to desired threshold
```

### Adding More Change Type Keywords

To detect additional change types, edit the workflow file. In the script section, add new keyword detection patterns:

```javascript
// In .github/workflows/pr-auto-description.yml
// Around line 118 in the script section
if (allText.match(/\b(migrate|upgrade|database)\b/)) changeType.push('migration');
if (allText.match(/\b(security|vulnerability|cve)\b/)) changeType.push('security');
```

### Modifying File Categorization

To change how files are categorized, edit the file detection logic:

```javascript
files.forEach(file => {
  const filename = file.filename;
  if (filename.includes('your-pattern')) {
    filesByType.yourCategory.push(filename);
  }
  // ... more conditions
});
```

## Troubleshooting

### Description Not Generated

If the automatic description isn't being generated:

1. **Check workflow status**: Go to Actions tab and check if the workflow ran
2. **Check permissions**: The workflow needs `pull-requests: write` permission
3. **Check content length**: Ensure the existing description is less than 50 characters
4. **Check actor**: Verify the PR isn't from a bot that's excluded

### Description Looks Wrong

If the generated description isn't accurate:

1. **Check commit messages**: The description quality depends on good commit messages
2. **Update manually**: You can always edit the PR description manually
3. **Adjust keywords**: Update the workflow file to better detect your change types

### Workflow Failures

If the workflow fails:

1. **Check logs**: Go to Actions → failed workflow → view logs
2. **Check API rate limits**: GitHub API has rate limits (rarely an issue)
3. **Retry**: Re-run the workflow or close and reopen the PR

## Best Practices

### For PR Authors

1. **Write clear commit messages**: The quality of auto-generated descriptions depends on commit messages
2. **Use descriptive PR titles**: Include keywords that help identify the change type
3. **Add manual details if needed**: The auto-description is a starting point; add more context if needed
4. **Keep commits focused**: Each commit should represent a logical unit of work

### For Maintainers

1. **Review generated descriptions**: Check that they accurately represent the changes
2. **Encourage good commit messages**: Better commits = better auto-descriptions
3. **Update keywords**: As patterns emerge, update the workflow to better detect change types
4. **Adjust thresholds**: If too many/few descriptions are generated, adjust the threshold

## Integration with Other Workflows

This workflow complements:

- **Label Issues** workflow: Auto-labels PRs based on content
- **Tests** workflow: Validates that changes don't break the build
- **Docs** workflow: Generates documentation from code

Together, these workflows help maintain a well-organized and documented codebase.

## Technical Details

### Workflow File

- Location: `.github/workflows/pr-auto-description.yml`
- Trigger: `pull_request` events (opened, edited)
- Uses: `actions/github-script@v8` for GitHub API interaction

### Permissions Required

- `pull-requests: write` - To update PR descriptions
- `contents: read` - To read repository files

### API Calls Made

1. `pulls.listCommits` - Fetch commit messages
2. `pulls.listFiles` - Get changed files and statistics
3. `pulls.update` - Update the PR description

## Support

For questions or issues with the auto-description system:
- Check the workflow logs in the Actions tab
- Review `.github/PR_AUTO_DESCRIPTION.md` (this file)
- Open an issue with the `build` or `enhancement` label

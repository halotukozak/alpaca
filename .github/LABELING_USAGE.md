# Automated Issue Labeling - Usage Guide

This guide explains how to use the automated issue labeling system for the Alpaca project.

## Overview

The Alpaca repository uses an automated system to categorize and label issues. This helps maintainers quickly identify issue types and prioritize work.

## Automatic Labeling

### For New Issues

When a new issue is created or edited, the system automatically:
1. Analyzes the issue title and description
2. Searches for relevant keywords
3. Applies appropriate labels

**This happens automatically** - no manual intervention is needed for most cases.

### Label Categories

The system applies labels from these categories:

#### Component Labels
- **Parser** - Parser functionality, grammar rules, AST, productions
- **Lexer** - Tokenization, regex patterns, lexical analysis

#### Type Labels
- **bug** - Something isn't working correctly
- **enhancement** - Feature requests or improvements
- **documentation** - Documentation improvements

#### Process Labels
- **testing** - Test-related issues
- **build** - Build system, CI/CD
- **performance** - Performance optimization
- **error-handling** - Error messages, diagnostics
- **API** - Public API or user-facing interfaces
- **refactoring** - Code quality improvements

## Labeling Existing Issues

### Using GitHub Actions UI

1. Go to the **Actions** tab in the repository
2. Select **"Label Existing Issues"** workflow from the left sidebar
3. Click **"Run workflow"** button
4. Choose the branch (usually `master`)
5. Select dry run mode:
   - `true` - Preview labels without applying them (recommended first)
   - `false` - Actually apply labels to issues
6. Click **"Run workflow"** to start

### Dry Run First (Recommended)

Always run in dry-run mode first to preview what labels would be applied:

```yaml
dry_run: true
```

Review the workflow logs to see which labels would be applied to each issue. If the results look good, run again with:

```yaml
dry_run: false
```

### Example Workflow Run

```
🔍 DRY RUN MODE - No labels will be applied
Found 25 open issues

Issue #151: "Overlapping regex in one lexer case"
  Existing labels: (none)
  Labels to add: Lexer
  👁️ Would add labels (dry run)

Issue #150: "Debug conflict resolution graph"
  Existing labels: (none)
  Labels to add: Parser, refactoring
  👁️ Would add labels (dry run)

...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Summary:
  Total issues processed: 25
  Issues that would be labeled: 18
  Mode: DRY RUN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Issue Templates

The repository provides structured issue templates that help guide users to provide the right information:

- **Bug Report** - For reporting bugs (auto-labeled with `bug`)
- **Feature Request** - For suggesting features (auto-labeled with `enhancement`)
- **Documentation Issue** - For documentation improvements (auto-labeled with `documentation`)

These templates include component dropdowns that help with automatic labeling.

## Manual Label Management

While the system is automated, maintainers can:

### Add Labels Manually
If the system misses a label or you want to add a specific label:
1. Go to the issue
2. Click the gear icon next to "Labels" on the right sidebar
3. Select the appropriate labels

### Remove Incorrect Labels
If the system applies an incorrect label:
1. Go to the issue
2. Click the gear icon next to "Labels"
3. Deselect the incorrect label

### Create New Labels
If you need a new label category:
1. Go to Issues → Labels
2. Click "New label"
3. Enter name, description, and color
4. Update `.github/workflows/label-issues.yml` to include the new label in the `labelRules` object

## Keyword Reference

Labels are applied when these keywords appear in issue titles or descriptions:

| Label | Keywords |
|-------|----------|
| Parser | parser, parse, parsing, shift-reduce, lr(1), syntax tree, ast, production, grammar, rule |
| Lexer | lexer, lexeme, token, tokenize, regex, pattern matching, lexical |
| bug | bug, error, fail, broken, issue, not work, crash, exception, incorrect |
| documentation | documentation, docs, readme, guide, tutorial, example, explain |
| enhancement | feature, enhancement, improve, add, support, should, would be nice, could |
| testing | test, coverage, unit test, integration test, spec |
| build | build, compile, ci, github actions, workflow, mill, sbt |
| performance | performance, slow, optimize, speed, efficient, benchmark |
| error-handling | error message, diagnostic, verbose, warning, better error |
| API | api, interface, public api, user-facing, usage |
| refactoring | refactor, clean, reorganize, restructure, code quality |

## Tips for Better Automatic Labeling

When creating issues, help the system by:

1. **Be specific in titles**: Include component names (Parser, Lexer)
2. **Use descriptive keywords**: Mention what type of issue it is
3. **Fill out templates**: Use the provided issue templates
4. **Mention components**: Explicitly state which component is affected

### Good Examples

✅ **Good**: "Parser fails for multiline action"
- Will be labeled: Parser, bug

✅ **Good**: "Add better error messages for lexer conflicts"
- Will be labeled: Lexer, error-handling, enhancement

❌ **Avoid**: "This doesn't work"
- Too vague for automatic labeling

## Troubleshooting

### Labels Not Applied

If labels aren't being applied automatically:

1. **Check workflow status**: Go to Actions tab and check if the workflow ran
2. **Check permissions**: The workflow needs `issues: write` permission
3. **Manual trigger**: Run the "Label Existing Issues" workflow manually
4. **Check keywords**: Ensure the issue contains relevant keywords

### Wrong Labels Applied

If incorrect labels are applied:

1. **Remove manually**: Click the label to remove it
2. **Update keywords**: Edit the issue to use better keywords
3. **Report issue**: If this happens frequently, open an issue to adjust the keyword rules

### Workflow Failures

If the workflow fails:

1. **Check logs**: Go to Actions → failed workflow → view logs
2. **Check rate limits**: GitHub API has rate limits (usually not an issue)
3. **Retry**: Click "Re-run jobs" to retry the workflow

## Maintenance

### Updating Label Rules

To modify the automatic labeling rules:

1. Edit `.github/workflows/label-issues.yml`
2. Update the `labelRules` object in the script
3. Add, remove, or modify keyword mappings
4. Commit and push changes
5. Test with a new issue or run "Label Existing Issues" in dry-run mode

### Adding New Labels

When adding a new label to the repository:

1. Create the label in GitHub (Issues → Labels → New label)
2. Update `.github/workflows/label-issues.yml` with the new label and keywords
3. Update `.github/workflows/label-existing-issues.yml` similarly
4. Update `.github/ISSUE_LABELING.md` documentation
5. Update this usage guide

## Best Practices

1. **Review periodically**: Check if labels are being applied correctly
2. **Update keywords**: As new patterns emerge, update keyword rules
3. **Use dry-run first**: Always test batch labeling with dry-run mode
4. **Manual adjustment**: Don't hesitate to manually adjust labels when needed
5. **Label consistency**: Try to maintain consistent labeling across similar issues

## Support

For questions or issues with the labeling system:
- Open an issue with the `documentation` label
- Check `.github/ISSUE_LABELING.md` for user-facing documentation
- Review workflow logs in the Actions tab

# Automated Issue Labeling - Usage Guide

This guide explains how to use the **AI-powered automated issue labeling system** for the Alpaca project, powered by **GitHub Copilot**.

## Overview

The Alpaca repository uses GitHub Copilot (GPT-4o model) to intelligently categorize and label issues and pull requests. Unlike simple keyword matching, the AI understands context and semantics to provide more accurate labeling.

## Automatic Labeling

### For New Issues

When a new issue or pull request is created or edited, the system automatically:
1. Sends the issue title and description to GitHub Copilot
2. AI analyzes the content semantically (not just keywords)
3. Suggests appropriate labels based on understanding the issue's intent
4. Applies the suggested labels to the issue

**This happens automatically** - no manual intervention is needed for most cases.

### Label Categories

The AI can apply labels from these categories:

#### Component Labels
- **Parser** - Parser functionality, grammar rules, AST, productions, LR parsing
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

## AI-Powered vs Keyword-Based

The system previously used keyword matching but now uses AI for better accuracy:

| Feature | Keyword-Based (Old) | AI-Powered (New) |
|---------|-------------------|------------------|
| Understanding | Matches specific words | Understands context and intent |
| Flexibility | Rigid patterns | Adapts to different phrasings |
| Accuracy | Can miss or over-label | More nuanced decisions |
| Context | No context awareness | Understands project-specific terms |

### Examples

**Issue**: "The tokenizer fails when processing strings with escape sequences"
- **Keyword system** might miss "tokenizer" → "Lexer" connection
- **AI system** understands tokenizer = lexer, and "fails" = bug → Labels: `Lexer`, `bug`

**Issue**: "Add documentation about handling ambiguous grammars"
- **Keyword system** matches "documentation" and "grammar"
- **AI system** also recognizes this is about Parser component → Labels: `Parser`, `documentation`

## Labeling Existing Issues

### Using GitHub Actions UI

1. Go to the **Actions** tab in the repository
2. Select **"Label Existing Issues and Pull Requests"** workflow from the left sidebar
3. Click **"Run workflow"** button
4. Choose the branch (usually `master`)
5. Select dry run mode:
   - `true` - Preview labels without applying them (recommended first)
   - `false` - Actually apply AI-suggested labels to issues
6. Click **"Run workflow"** to start

**Note**: The workflow uses GitHub Copilot to analyze each issue, so it may take some time for large repositories. The AI makes intelligent decisions about which labels to apply.

### Dry Run First (Recommended)

Always run in dry-run mode first to preview what labels the AI would apply:

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

[1/25] ISSUE #151: "Overlapping regex in one lexer case"
  Existing labels: (none)
  AI suggests adding: Lexer, bug
  👁️ Would add labels (dry run)

[2/25] ISSUE #150: "Debug conflict resolution graph"
  Existing labels: (none)
  AI suggests adding: Parser, refactoring
  👁️ Would add labels (dry run)

[3/25] ISSUE #148: "Make the parser faster for large grammars"
  Existing labels: Parser
  AI suggests adding: performance, enhancement
  👁️ Would add labels (dry run)

...

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Summary:
  Total items processed: 25
  Items that would be labeled: 18
  Mode: DRY RUN
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Issue Templates

The repository provides structured issue templates that help guide users to provide the right information:

- **Bug Report** - For reporting bugs
- **Feature Request** - For suggesting features
- **Documentation Issue** - For documentation improvements

These templates help provide context to the AI labeling system, improving its accuracy.

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
4. Update `.github/workflows/label-issues.yml` and `.github/workflows/label-existing-issues.yml` to include the new label in the `availableLabels` object with a clear description
5. The AI will automatically start recognizing and using the new label

## How the AI Works

The labeling system:
- Uses **GitHub Models API** with **GPT-4o** model
- Analyzes semantic meaning, not just keywords
- Understands project-specific terminology (Lexer, Parser, etc.)
- Considers the full context of the issue
- Only adds labels that aren't already present

### AI Prompt Engineering

The system provides the AI with:
- Clear descriptions of each available label
- Context about the Alpaca project (Scala 3 lexer/parser library)
- The issue title and body
- Instructions to return only applicable labels

This helps ensure accurate and consistent labeling across issues.

## Keyword Reference (Legacy Information)

**Note**: The system no longer uses simple keyword matching. The AI understands these concepts semantically, so you don't need to use specific keywords. However, mentioning relevant terms can help:

| Label | Related Concepts |
|-------|----------|
| Parser | parser, parsing, grammar, AST, syntax tree, shift-reduce, LR(1), production rules |
| Lexer | lexer, tokenization, regex, pattern matching, lexical analysis, tokenizer |
| bug | error, failure, crash, exception, broken, not working |
| documentation | docs, guide, tutorial, examples, explanation |
| enhancement | feature request, improvement, new feature, support for |
| testing | tests, test coverage, unit tests, specs |
| build | build system, compilation, CI/CD, Mill, SBT |
| performance | slow, optimization, efficiency, speed, benchmark |
| error-handling | error messages, diagnostics, warnings |
| API | API, interface, public methods, user-facing |
| refactoring | code quality, restructure, clean up |

## Tips for Better Automatic Labeling

The AI works best when issues are:

1. **Clear and descriptive**: Explain what the issue is about
2. **Specific**: Mention which component or area is affected
3. **Detailed**: Provide context, examples, or error messages
4. **Well-structured**: Use the issue templates when appropriate

### Good Examples

✅ **Good**: "Parser fails to handle left-recursive grammars correctly"
- Clear component (Parser), clear problem (fails), specific issue (left-recursive grammars)
- AI will likely label: Parser, bug

✅ **Good**: "Add examples showing how to use contextual lexing"
- Clear what's being requested (examples), clear component (lexing)
- AI will likely label: Lexer, documentation

✅ **Good**: "Performance degrades with deeply nested expressions"
- Clear issue (performance), provides context
- AI will likely label: Parser, performance

❌ **Avoid**: "This doesn't work"
- Too vague for AI to understand context
- Better: "Parser crashes when processing multiline comments"

## Troubleshooting

### Labels Not Applied

If labels aren't being applied automatically:

1. **Check workflow status**: Go to Actions tab and check if the workflow ran successfully
2. **Check permissions**: The workflow needs `issues: write` permission
3. **Check API access**: Ensure GitHub Models API is accessible
4. **Manual trigger**: Run the "Label Existing Issues and Pull Requests" workflow manually
5. **Review logs**: Check the workflow logs for any API errors

### Wrong Labels Applied

If incorrect labels are applied:

1. **Remove manually**: Click the label to remove it
2. **Adjust issue description**: Edit the issue to provide more context
3. **Report issue**: If the AI consistently mislabels, open an issue about it

### Workflow Failures

If the workflow fails:

1. **Check logs**: Go to Actions → failed workflow → view logs
2. **Check rate limits**: GitHub API and Models API have rate limits
3. **Retry**: Click "Re-run jobs" to retry the workflow
4. **Check API status**: Verify GitHub Models API is operational

### AI Not Understanding Issue

If the AI seems to misunderstand the issue:

1. **Be more explicit**: Use clearer language and specific terminology
2. **Add context**: Provide more details about what you're trying to accomplish
3. **Mention components**: Explicitly state if it's about Parser or Lexer
4. **Use examples**: Include code examples or error messages

## Maintenance

### Updating Label Descriptions

To help the AI better understand labels:

1. Edit `.github/workflows/label-issues.yml`
2. Update the `availableLabels` object with clearer descriptions
3. Edit `.github/workflows/label-existing-issues.yml` similarly
4. Commit and push changes
5. The AI will use the updated descriptions for future labeling

### Adding New Labels

When adding a new label to the repository:

1. Create the label in GitHub (Issues → Labels → New label)
2. Update `.github/workflows/label-issues.yml`:
   - Add entry to `availableLabels` with clear description
3. Update `.github/workflows/label-existing-issues.yml` similarly
4. Update `.github/ISSUE_LABELING.md` documentation
5. Update this usage guide
6. Test with a new issue or run the manual workflow in dry-run mode

### Monitoring AI Performance

Periodically review:
- Are labels being applied accurately?
- Are there patterns of mislabeling?
- Do label descriptions need refinement?
- Should new labels be added?

You can use the manual labeling workflow in dry-run mode to preview how the AI would label existing issues.

## Best Practices

1. **Trust the AI**: The system is trained to understand context, so detailed issues work better than keyword stuffing
2. **Provide context**: The more information you provide, the better the AI can categorize
3. **Use dry-run first**: Always test batch labeling with dry-run mode
4. **Manual adjustment**: Don't hesitate to manually adjust labels when the AI makes mistakes
5. **Monitor and improve**: Use feedback to refine label descriptions
6. **Be patient**: AI labeling may take a few seconds per issue

## Rate Limits

The GitHub Models API has rate limits:
- **Automatic labeling**: Happens once per issue creation/edit, so rate limits are rarely an issue
- **Batch labeling**: The workflow includes 1-second delays between requests to respect rate limits
- If you hit rate limits, the workflow will log errors but continue processing other issues

## Support

For questions or issues with the labeling system:
- Open an issue with the `documentation` label
- Check `.github/ISSUE_LABELING.md` for user-facing documentation
- Review workflow logs in the Actions tab
- For AI-specific issues, include the issue number and what labels you expected

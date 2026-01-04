# GitHub Copilot AI-Powered Issue Labeling

## Overview

This repository now uses **GitHub Copilot** (GPT-4o model via GitHub Models API) to automatically label issues and pull requests with intelligent, context-aware labels.

## What Changed?

### Before: Keyword-Based Labeling
The old system used simple keyword matching:
- Searched for specific words like "parser", "bug", "documentation"
- Could miss issues if keywords weren't used
- No understanding of context or synonyms

### After: AI-Powered Labeling
The new system uses GitHub Copilot:
- **Semantic understanding**: Understands meaning beyond keywords
- **Context-aware**: Recognizes project-specific terminology
- **Intelligent**: Handles synonyms and related concepts
- **Accurate**: Makes nuanced decisions about appropriate labels

## How It Works

### Automatic Labeling (On Issue/PR Creation)

When you create or edit an issue or PR:

1. GitHub Actions triggers the workflow
2. Issue content is sent to GitHub Copilot (GPT-4o)
3. AI analyzes the title and description semantically
4. AI suggests appropriate labels based on understanding
5. Labels are automatically applied to the issue

**Workflow File**: `.github/workflows/label-issues.yml`

### Manual Batch Labeling

You can also label all existing open issues at once:

1. Go to **Actions** tab → **"Label Existing Issues and Pull Requests"**
2. Click **"Run workflow"**
3. Select dry-run mode:
   - `true` - Preview labels (recommended first)
   - `false` - Apply labels
4. Click **"Run workflow"**

**Workflow File**: `.github/workflows/label-existing-issues.yml`

## Available Labels

The AI can apply these labels:

| Label | Description |
|-------|-------------|
| `Parser` | Parser functionality, grammar rules, AST, LR parsing |
| `Lexer` | Lexer functionality, tokenization, regex patterns |
| `bug` | Something isn't working correctly |
| `enhancement` | Feature requests or improvements |
| `documentation` | Documentation improvements |
| `testing` | Test-related issues |
| `build` | Build system, CI/CD issues |
| `performance` | Performance optimization |
| `error-handling` | Error messages, diagnostics |
| `API` | Public API or user-facing interfaces |
| `refactoring` | Code quality improvements |

## Examples of AI Understanding

### Example 1: Synonyms
**Issue**: "The tokenizer crashes when parsing Unicode characters"
- **AI understands**: tokenizer = Lexer component
- **Labels applied**: `Lexer`, `bug`

### Example 2: Context
**Issue**: "Add examples for handling shift-reduce conflicts"
- **AI understands**: shift-reduce conflicts are parser concepts, examples = documentation
- **Labels applied**: `Parser`, `documentation`

### Example 3: Multiple Categories
**Issue**: "Improve error messages when grammar has ambiguities"
- **AI understands**: grammar = Parser, error messages = error-handling, improve = enhancement
- **Labels applied**: `Parser`, `error-handling`, `enhancement`

## Technical Implementation

### GitHub Models API

The system uses:
- **Endpoint**: `https://models.inference.ai.azure.com/chat/completions`
- **Model**: `gpt-4o`
- **Authentication**: GitHub token (automatic in Actions)
- **Temperature**: 0.3 (for consistent, focused responses)
- **Max tokens**: 150 (sufficient for label suggestions)

### Prompt Engineering

The AI is given:
- **System prompt**: Project context (Scala 3 lexer/parser library) + available labels with descriptions
- **User prompt**: Issue title and body
- **Output format**: JSON array of label names

Example system prompt:
```
You are an expert issue triaging assistant for the Alpaca project, 
a Scala 3 lexer and parser library. Your task is to analyze issues 
and assign appropriate labels from a predefined list.

Available labels:
- Parser: Issues related to parser functionality, syntax analysis...
- Lexer: Issues related to lexer functionality, tokenization...
...

Return ONLY a JSON array of label names.
Example: ["Parser", "bug", "performance"]
```

### Error Handling

The workflows include robust error handling:
- **API failures**: Logged but don't fail the workflow
- **Invalid responses**: Gracefully handled with fallback to empty array
- **Rate limits**: 1-second delays between batch requests
- **Validation**: Only applies labels that exist in the repository

## Advantages Over Keyword Matching

| Feature | Keyword-Based | AI-Powered |
|---------|---------------|------------|
| **Flexibility** | Rigid patterns | Understands variations |
| **Context** | None | Full semantic understanding |
| **Synonyms** | Must be explicitly listed | Automatically recognized |
| **Accuracy** | Can over/under-label | More nuanced decisions |
| **Maintenance** | Update keyword lists | Update label descriptions |
| **Learning** | Static rules | Adapts to patterns |

## Configuration

### Adding New Labels

1. Create the label in GitHub (Issues → Labels → New label)
2. Edit `.github/workflows/label-issues.yml`:
   ```javascript
   const availableLabels = {
     'Parser': 'Issues related to parser...',
     'Lexer': 'Issues related to lexer...',
     'YourNewLabel': 'Clear description of what this label means...'
   };
   ```
3. Edit `.github/workflows/label-existing-issues.yml` similarly
4. Commit and push

The AI will automatically start using the new label!

### Modifying Label Descriptions

To improve AI accuracy, update the label descriptions:

```javascript
const availableLabels = {
  'Parser': 'Updated description that better describes when to use this label...',
  // ...
};
```

Clear, detailed descriptions help the AI make better decisions.

## Best Practices

### For Issue Authors

1. **Write clear titles**: Be specific about the problem or request
2. **Provide context**: Explain what you're trying to accomplish
3. **Include details**: Error messages, code snippets, expected behavior
4. **Be descriptive**: The AI understands natural language well

### For Maintainers

1. **Review AI suggestions**: Occasionally check if labels are accurate
2. **Adjust manually**: Don't hesitate to change labels if AI makes mistakes
3. **Update descriptions**: Refine label descriptions based on patterns
4. **Use dry-run**: Test batch labeling before applying
5. **Monitor API usage**: Be aware of rate limits for large batch operations

## Limitations

- **Rate limits**: GitHub Models API has rate limits (rarely hit in normal use)
- **API availability**: Requires GitHub Models API access (available in GitHub Actions)
- **Token costs**: Uses GitHub tokens (free in GitHub Actions)
- **Accuracy**: While very good, AI can occasionally mislabel (manual adjustment possible)

## Troubleshooting

### Labels not applied
- Check Actions tab for workflow run status
- Review workflow logs for API errors
- Ensure permissions are set correctly

### Wrong labels applied
- Remove incorrect labels manually
- Consider updating label descriptions for clarity
- Report persistent issues

### Workflow fails
- Check Actions logs for detailed error messages
- Verify GitHub Models API is accessible
- Retry the workflow

## Migration from Keyword-Based System

The upgrade is **backward compatible**:
- Old workflows are replaced with new AI-powered ones
- No changes needed to labels or issue templates
- Existing labels remain unchanged
- Can manually re-label existing issues using batch workflow

## Support

For issues or questions:
- Open an issue with the `documentation` label
- Review `.github/ISSUE_LABELING.md` for user guide
- Review `.github/LABELING_USAGE.md` for detailed usage
- Check workflow logs in the Actions tab

## Resources

- [GitHub Models Documentation](https://docs.github.com/en/github-models)
- [GitHub Actions github-script](https://github.com/actions/github-script)
- [Issue Labeling Guide](.github/ISSUE_LABELING.md)
- [Labeling Usage Guide](.github/LABELING_USAGE.md)

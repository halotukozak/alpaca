# Issue Labeling System

This repository uses an **AI-powered automated issue labeling system** powered by **GitHub Copilot** to help organize and triage issues intelligently.

## How It Works

When you create or edit an issue or pull request, GitHub Copilot (using GPT-4o) analyzes the content and automatically applies appropriate labels based on semantic understanding of the text, not just simple keyword matching. This provides more accurate and context-aware labeling.

## Available Labels

### Component Labels
- **Parser**: Issues related to parser functionality, syntax analysis, grammar rules, and AST generation
- **Lexer**: Issues related to lexer functionality, tokenization, regex patterns, and lexical analysis

### Type Labels
- **bug**: Something isn't working correctly
- **enhancement**: Feature requests or improvements
- **documentation**: Improvements or additions to documentation

### Process Labels
- **testing**: Issues related to tests, test coverage, or testing infrastructure
- **build**: Issues related to the build system, CI/CD, or compilation
- **performance**: Performance optimization issues
- **error-handling**: Issues related to error messages, diagnostics, or warnings
- **API**: Issues related to the public API or user-facing interfaces
- **refactoring**: Code quality improvements or restructuring

## AI-Powered Labeling

The labeling system uses GitHub's AI models to:
- **Understand context**: Goes beyond keyword matching to understand the actual intent and content
- **Analyze semantics**: Recognizes related concepts even if specific keywords aren't used
- **Handle ambiguity**: Better handles cases where the issue could fit multiple categories
- **Learn patterns**: Understands project-specific terminology and patterns

### Examples of AI Understanding

✅ **Issue**: "The tokenizer doesn't handle unicode properly"
- AI recognizes this is about the **Lexer** (tokenizer = lexer component) and a **bug**

✅ **Issue**: "Could we make the grammar rule conflict resolver faster?"
- AI understands this involves the **Parser** (grammar rules) and is a **performance** and **enhancement** request

✅ **Issue**: "Need examples for context-aware lexing"
- AI categorizes as **Lexer** and **documentation** based on understanding the request

## Manual Label Management

While labels are automatically applied by AI, maintainers can:
- Add additional labels manually if needed
- Remove incorrectly applied labels
- Create new labels for emerging categories
- Provide feedback to improve the AI labeling system

## Contributing

When creating a new issue:
- Write clear, descriptive titles and descriptions
- Include relevant details about your problem or request
- Mention which component is affected (Parser/Lexer) if known
- The AI system will intelligently categorize your issue, but maintainers may adjust labels as needed

The AI-powered labeling works best when issues include:
- Clear problem descriptions
- Context about what you're trying to do
- Expected vs. actual behavior (for bugs)
- Use cases or examples

## Technical Details

The labeling system uses:
- **GitHub Models API** with GPT-4o model
- Triggered automatically on issue/PR creation and edits
- Provides label suggestions based on semantic analysis
- Respects existing labels and only adds new ones

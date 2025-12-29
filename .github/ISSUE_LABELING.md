# Issue Labeling System

This repository uses an automated issue labeling system to help organize and triage issues.

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

## How Labeling Works

The automated labeling system analyzes issue titles and bodies for keywords and automatically applies appropriate labels when issues are opened or edited.

### Keyword Mapping

Labels are automatically applied based on the following keywords:

- **Parser**: parser, parse, parsing, shift-reduce, lr1, lr(1), syntax tree, ast, production, grammar
- **Lexer**: lexer, lexeme, token, tokenize, regex, pattern matching, lexical
- **bug**: bug, error, fail, broken, not work, crash, exception, incorrect, does not work, doesn't work
- **documentation**: documentation, docs, readme, guide, tutorial, example, explain
- **enhancement**: feature request, enhancement, improve, add support, should support, would be nice, could have, could support
- **testing**: test, coverage, unit test, integration test, spec
- **build**: build, compile, ci, github actions, workflow, mill, sbt
- **performance**: performance, slow, optimize, speed, efficient, benchmark
- **error-handling**: error message, diagnostic, verbose, warning, better error
- **API**: api, interface, public api, user-facing
- **refactoring**: refactor, clean up, reorganize, restructure, code quality

## Manual Label Management

While labels are automatically applied, maintainers can:
- Add additional labels manually if needed
- Remove incorrectly applied labels
- Create new labels for emerging categories

## Contributing

When creating a new issue:
- Use descriptive keywords in your title and description
- Be specific about which component (Parser/Lexer) is affected
- Clearly indicate if it's a bug, enhancement, or documentation issue
- The automated system will help categorize your issue, but maintainers may adjust labels as needed

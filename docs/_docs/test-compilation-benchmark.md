# Test Compilation Benchmark

## Overview

The Alpaca project includes automated benchmarking of test compilation times to help track performance changes over time. This feature allows you to compare how long it takes to compile tests on different branches.

## Features

### Automatic Timing in CI

Every build in the CI pipeline now automatically measures and reports test compilation time. This information is displayed in the GitHub Actions summary for each workflow run.

### Benchmark Comparison Workflow

A dedicated workflow (`test-compilation-benchmark.yml`) compares test compilation times between your current branch and the master branch. This workflow runs automatically on:

- Every push to any branch
- Pull requests targeting the master branch

### What It Measures

The benchmark measures the time taken to run `./mill test.compile`, which compiles all test source files in the project. The main project is compiled first with `./mill compile` to ensure the measurement captures only the test compilation time, not the combined main project and test compilation time.

## Viewing Results

### In the Test Workflow

For every test run, you can see the test compilation time in the workflow summary:

1. Go to the Actions tab in GitHub
2. Select a workflow run
3. Look for the "Report Test Compilation Time" section in the summary

### In the Benchmark Workflow

For detailed comparison with master:

1. Go to the Actions tab in GitHub
2. Find the "Test Compilation Benchmark" workflow
3. View the summary which shows:
   - Current branch compilation time
   - Master branch compilation time
   - Difference (faster/slower)
   - Percentage change

### On Pull Requests

For pull requests, the benchmark workflow automatically posts a comment with the compilation time comparison, making it easy to see if your changes have impacted compilation performance.

## Example Output

```
📊 Test Compilation Time Benchmark

| Branch | Compilation Time |
|--------|------------------|
| Current | 45s |
| Master | 50s |

Current branch is 5s faster (10.00% decrease) ✅
```

## Use Cases

- **Performance Regression Detection**: Identify when changes cause test compilation to slow down
- **Optimization Validation**: Verify that optimization efforts are improving compilation times
- **Impact Assessment**: Understand how new features or refactorings affect build performance

## Technical Details

- The main project is compiled first using `./mill compile` to isolate test compilation time
- Test compilation is performed on a clean slate (after running `./mill clean`)
- Times are measured using Unix timestamps
- Both branches use the same CI environment for fair comparison
- Caching is used for Mill dependencies to ensure consistent timing

## Disabling the Benchmark

If you want to disable the benchmark workflow for specific pushes, you can modify the workflow triggers in `.github/workflows/test-compilation-benchmark.yml`.

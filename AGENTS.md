# AGENTS.md

## Required Skill

Before making code changes in this repository, use `$architecture-guidelines`.

This is required for:

- package structure changes
- exception handling changes
- event, lifecycle, cancel, or error flow changes
- listener, registry, processor, handler, converter, factory, provider, or parameter resolver placement decisions
- architecture refactors
- new framework-level APIs or extension integration points

## Workflow

1. Inspect the relevant code and docs first.
2. Apply `$architecture-guidelines` before deciding package placement or exception behavior.
3. Prefer domain-first package structure.
4. Keep mechanism code separate from feature implementation code.
5. Do not create technical-role subpackages for a single class unless the package expresses a real boundary.
6. Do not use exceptions for normal flow control.
7. Avoid duplicate logging of the same failure.
8. After code changes, run the narrowest useful tests first, then broader build checks when appropriate.

## Project Notes

- The project targets Java 8 by default.
- Do not move unrelated files during refactors.
- Do not commit or normalize generated output, runtime data, caches, or IDE metadata unless explicitly requested.

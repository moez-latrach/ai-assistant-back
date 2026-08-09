# Claude Code Instructions

Before making any change to this project, read `AGENTS.md` at the repository root.

`AGENTS.md` is the primary source of project context and contains:

- project purpose and architecture;
- technology stack;
- important classes and packages;
- development conventions;
- build and test commands;
- current implementation state;
- known technical issues;
- roadmap;
- rules for AI agents modifying the codebase.

## Rules

- Treat `AGENTS.md` as the source of truth for project-level instructions.
- Do not duplicate project documentation in this file.
- Inspect the relevant existing code before making changes.
- Follow the architecture and development rules defined in `AGENTS.md`.
- Prefer small, testable changes.
- Run the appropriate tests after modifying code.
- If the code and `AGENTS.md` disagree, inspect the current implementation and report the discrepancy rather than silently assuming the documentation is correct.

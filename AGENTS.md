# General Coding Instructions

These rules apply to all AI agents working on this codebase.

- **MANDATORY: When changing a module, you MUST review and update that module's CLAUDE.md to reflect your changes.** Create the file if it doesn't exist. If behavior, architecture, or public API changed, the CLAUDE.md must be updated in the same session. This is not optional — skipping this is a violation of project rules.
- **MANDATORY: When making a non-trivial design decision, you MUST document it in `design.md` in the relevant module directory.** Create the file if it doesn't exist. Each entry should include the decision, the alternatives considered, and pros/cons for each. Sort entries by decision. This is not optional — skipping this is a violation of project rules.
- Use your tools in simple ways — always only read 1 file at a time
- Only modify code that needs to be changed. Keep changes as small as possible
- If code for a specific thing already exists, edit it instead of adding new code
- Always fix the logic: if a certain use case is given, assume other same cases are also to be fixed
- Try to avoid doing a text search across all files
- **Never modify tests to make them pass.** If a test fails, fix the production code, not the test. Tests define the expected behavior.
- **Always question the usefulness of a given command and if it is deemed a bad idea give reasons for that decision and dont implement it**. Be a critic and play devils advocate.
- If things are unclear, ask questions before proceeding — do not assume
- **Think critically about problems.** Don't jump to the first solution. Consider edge cases, question assumptions, and explore whether the problem is a symptom of a deeper issue. Ask clarifying questions when the root cause isn't obvious.
- **Never chain shell commands** with `&&`, `;`, or `||`. Run each command as a separate Bash tool call so that permissions are correctly applied to each individual command.

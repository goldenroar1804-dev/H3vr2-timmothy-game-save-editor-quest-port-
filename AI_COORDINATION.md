# AI Coordination - Timmy Quest Editor

## Project
Quest-compatible Android save editor for H3VR2. Do not assume save structure or fields without evidence.

## Shared Workspace
Repository: goldenroar1804-dev/H3vr2-timmothy-game-save-editor-quest-port-

## Roles
- **ChatGPT:** Primary coordinator, feature implementation, Android UI, GitHub maintenance, build troubleshooting.
- **Claude:** Independent reviewer, save-format investigator, bug finder, and implementation partner on explicitly assigned tasks.

## Safety / Branch Rules
- Keep main stable; do not push experimental work directly to main.
- Use task-specific branches and pull requests.
- Do not edit the same files concurrently unless explicitly coordinated.
- Preserve existing functionality; avoid broad rewrites.
- Never invent save fields, item IDs, paths, or formats.
- Back up saves before writes and preserve compression/format where verified.
- Report test evidence, not assumptions.

## Collaboration Protocol
1. Read this file and CLAUDE_TASK.md before beginning.
2. Claim only the task currently assigned in CLAUDE_TASK.md; don't duplicate ChatGPT's active work.
3. Work on a separate branch and open a PR; do not merge.
4. Include files changed, tests/results, risks, unresolved questions, and next recommendation in the PR.
5. ChatGPT reviews Claude's changes before merging.
6. Task ownership and status are updated in CLAUDE_TASK.md.

## Roadmap
1. Verify APK build.
2. Test with a genuine H3VR2 save.
3. Confirm save format/container/compression.
4. Establish verified editable fields.
5. Safe save-write/backup/restore validation.
6. Inventory and storage editing.
7. Store editing, if save data supports it.
8. Favorites/presets.
9. Searchable type-aware fields/tree.
10. Quest access/permissions testing.
11. Final Quest test and build.

## Reporting
Every task report must include: summary, files changed, test evidence, build status, risks, unresolved items, and proposed next task.

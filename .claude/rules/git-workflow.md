# Git and pull request workflow

These rules apply to every story cycle and complement the SDLC Orchestrator lifecycle and the shared rules in `CLAUDE.md`. They govern Git and GitHub actions only; phase order and artifacts stay as defined by the orchestrator.

The six lifecycle approval gates are unchanged: requirements, design, implementation plan, code review, verification defects, and **final PR approval**. Commit approval and push approval are separate Git-action approvals that precede the final PR approval; they are not additional lifecycle gates.

## Branch preflight (before any artifact or code is written)

Run this once per story cycle, after the story source and slug are resolved and before anything is written under `sdlc/` or `src/`.

1. Run `git status --short --branch`. Report the branch, uncommitted changes, and untracked files.
2. Never implement a story on `main`. If on `main`, move to a feature branch (steps 4-6) before writing anything.
3. If the working tree has uncommitted or untracked work that is not part of the active story, stop and ask how to proceed. Never discard, stash, reset, or overwrite it without explicit approval.
4. Run `git fetch origin main`. A fetch updates local remote-tracking refs, so it is a local state-changing preflight command, not a read-only one. Run it only here, once per preflight; never `git fetch --all`, `--prune`, `git pull`, or merge.
5. Check whether a branch for this story already exists: run `git for-each-ref refs/heads` and `git ls-remote --heads origin`, and look in their output for names starting with `feature/<story-key>-`.
6. Decide:

| Situation | Action |
|---|---|
| On `main`, clean, no story branch exists | Create the branch: `git switch --no-track -c feature/<story-key>-<short-description> origin/main`. Use `--no-track` so `origin/main` never becomes the push target. |
| Already on this story's `feature/<story-key>-*` branch | Resume. Uncommitted story artifacts and code are expected. |
| This story's branch exists locally or on the remote but is not checked out | Stop and ask. Do not recreate, reset, or overwrite it. |
| On an unrelated branch | Stop and ask. Do not branch off it silently. |
| Working tree has unrelated changes | Stop and ask (step 3). |

- Name the branch `feature/<story-key>-<short-description>`. `<story-key>` is the lowercase story identifier matching the story slug used for `sdlc/<story-slug>/` (for example `doc-123`; for a local story file, its slug). `<short-description>` is 2-5 lowercase, hyphen-separated words.
- Never use `git switch -C`, `git branch -f`, `git checkout -B`, `--force`, or `--discard-changes` to create or move to a branch. Never rename or delete an existing branch.
- Confirm `gh auth status` succeeds during preflight so a missing login is found before the PR phase.

## Workflow execution

- Run the existing SDLC workflow unchanged, with every human-in-the-loop approval gate. Git actions never replace or shortcut a gate.

## Final quality validation

- After verification passes and all approved fixes are applied, and immediately before preparing the PR, re-run the checks even if verification already ran them:
  1. `mvn test`
  2. `mvn package`
  3. `java -jar target/docguard-0.1.0-SNAPSHOT.jar --help`
- Run Maven without `-q` so test counts are visible, the same invocations CI uses (`mvn test`, `mvn package`). CI does not run the jar smoke check; this local run does.
- `.claude/hooks/quality-checks.ps1` (`sessionEnd` phase) covers the same three steps but runs Maven quietly and is not registered as a hook in `settings.json`. It is an optional manual equivalent and does not replace this run, because the PR needs the test counts.
- Record the exact commands, test counts, and build/package/CLI results from this run in the PR's Test Evidence. Do not copy numbers from earlier reports. If any file changes after this run, re-run it.
- Failures go through the verification-defect gate. Do not prepare a PR on failing checks unless the human explicitly accepts a documented limitation.

## Pull request evidence

- Generate `sdlc/<story-slug>/pull-request.md` with the sections the orchestrator defines: Summary, Changes Made, Test Evidence, Known Limitations, Reviewer Checklist, Agentic SDLC Evidence, Changelog. Keep the existing "Recommended PR Title" section at the end for the PR title.
- **Changes Made** comes from the actual changes, not the plan:
  - Before the commit: `git diff --stat origin/main`, `git diff origin/main`, and `git status --short` for untracked files.
  - After the commit: `git diff --stat origin/main...HEAD`.
- **Test Evidence** comes from the final quality validation above.
- **Agentic SDLC Evidence** records the approval status of the six lifecycle gates:
  - Requirements, design, implementation plan, code review, and verification: record each as approved only when actual evidence exists (an explicit approval in the conversation or a recorded artifact status), and cite that evidence. If it cannot be established, record "not established", never "approved".
  - Final PR approval: record as **Pending** in `pull-request.md`. It stays pending until the human explicitly grants it.
- Commit approval and push approval are not lifecycle gates. They are not recorded in `pull-request.md`.

## Commit approval, push approval, and final PR approval

Require explicit human approval before each of `git commit`, `git push`, and `gh pr create`. Approval for one does not authorize the next, and an approval does not carry over to changes made afterward. Ask for them one at a time, in this order. The commit, push, and PR commands always show the human the exact command through the approval prompt.

1. **Commit approval.** Show `git status --short --branch`, the diff summary, the exact files to stage by name (never `git add -A` or `git add .`), and the proposed commit message. By default make one commit that contains the story artifacts under `sdlc/<story-slug>/`, source code, tests, and `README.md` if changed. Never stage `target/` or root-level v1 SDLC files (unless explicitly asked). Check the staged files for secrets first.
2. **Push approval.** Show the branch, the commits to push (`git log --oneline origin/main..HEAD`), and the remote. Push only with `git push -u origin <feature-branch>`; never push `main`.
3. **Final PR approval.** After the push, verify `git diff --stat origin/main...HEAD` is non-empty and check for an existing PR with `gh pr list --head <branch> --state all`. If one exists, report its URL and ask; do not create another. Otherwise show the full title, body, base `main`, and Draft status. Approval to create the Draft PR is the final PR approval.

## Creating the PR

- Title: the "Recommended PR Title" section of `pull-request.md`.
- Body: the seven sections of `pull-request.md`, without the H1 and the title section, written to a temporary file outside the repository (never committed). In the body, update the final PR approval status to granted, with the date, only after the human has granted it, and note that commit approval and push approval were granted in this session. The committed `pull-request.md` keeps the status it had when it was written.
- Create: `gh pr create --draft --base main --head <branch> --title "<title>" --body-file <temp-file>`.
- The existing GitHub CI workflow (`.github/workflows/ci.yml`, triggered on `pull_request`, including drafts) validates the PR. Do not modify it. After creation, run `gh pr checks <pr-number>` once, report the result (checks may still be pending), and do not poll.
- Leave the PR in Draft. Never run `gh pr ready`, `gh pr merge`, or enable auto-merge. After creating the PR, report its URL and stop for human review.
- If `gh` is unavailable or unauthenticated, provide the complete title and body and state what is needed. If creation or push is rejected (branch protection, hooks, permissions), report the exact error and stop.

## Resuming after verification

Infer the position from `git status --short --branch`, `git log --oneline origin/main..HEAD`, `git ls-remote --heads origin` (look for your branch in the output), and `gh pr list --head <branch> --state all`:

- uncommitted changes -> at commit approval
- committed but not pushed -> at push approval
- pushed, no PR -> at final PR approval
- PR exists -> report its URL and stop

## Permissions: what `settings.json` does and does not do

- Allowlisted by exact command (no approval prompt): `git status --short --branch`, `git status --short`, `git branch --show-current`, `git for-each-ref refs/heads`, `git ls-remote --heads origin`, `git fetch origin main`, the `git diff` and `git log` forms used above, `gh auth status`, and the read-only `gh pr list --head *` and `gh pr checks *`. Prefer these exact commands and filter their output over asking for wider permissions.
- Everything else prompts, including `git switch`, `git add`, `git commit`, `git push`, and `gh pr create`. Do not request broad Git or GitHub write permissions.
- The `deny` rules are best-effort string patterns. They cover common force, delete, mirror/prune, push-to-`main` (including `HEAD:main` refspecs), hook-bypass, and global-option forms (`git -C … push`, `gh -R … pr merge`), plus `gh pr merge` and `gh pr ready`. The commit patterns match the bypass flag only as the first or last argument, so a commit message that merely mentions it is not blocked.
- They are **not complete protection**. Aliases, `gh api`, scripts, shell wrappers, and unusual option orderings can evade them. The prohibitions below apply regardless of what the patterns match.

## Prohibited

- Never force-push (`--force`, `-f`, `--force-with-lease`, `+refspec`).
- Never delete remote branches by any form (`--delete`, `-d`, `git push origin :branch`, `--mirror`, `--prune`).
- Never push to `main` by any form, including refspecs such as `HEAD:main`, and never use global options (`git -C`, `git -c`, `gh -R`) to run push, merge, or ready commands.
- Never bypass hooks or GitHub branch protection: no `--no-verify` (or `-n` on commit), no changing `core.hooksPath`, no admin overrides, no editing protection rules or required checks.
- Never merge a PR automatically or mark a Draft PR ready.
- Do not rewrite published history (`git commit --amend`, `rebase`, `reset --hard`) on a pushed branch without explicit human approval.

---
name: commit
description: Generate commit messages following AngularJS Commit Conventions
allowed-tools: Bash, Read, Grep, Glob
---

Analyze the staged changes (`git diff --staged`) and generate a commit message following the AngularJS Commit Conventions below. Then create the commit.

## Steps

1. Run `git diff --staged` to see what changes are staged.
2. If nothing is staged, inform the user and stop.
3. Analyze the changes and generate a commit message following the format rules below.
4. Show the generated commit message to the user and ask for confirmation before committing.
5. Once confirmed, create the commit with the message.

## Commit Message Format

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

- Any line cannot be longer than 100 characters.
- Body and footer are optional. Only include them when they add meaningful context.

### Subject Line

#### Allowed `<type>`
- **feat**: new feature
- **fix**: bug fix
- **docs**: documentation only
- **style**: formatting, missing semi colons, etc. (no logic change)
- **refactor**: code restructuring without feature or bug fix
- **test**: adding or correcting tests
- **chore**: maintenance tasks (build, CI, dependencies, etc.)

#### `<scope>`
- Specifies the place of the change (e.g. module, class, package name).
- Use lowercase.
- Examples: `product`, `order`, `auth`, `wishlist`, `build`

#### `<subject>` text
- Use imperative, present tense: "change" not "changed" nor "changes"
- Don't capitalize first letter
- No dot (.) at the end

### Message Body (optional)
- Use imperative, present tense.
- Include motivation for the change and contrast with previous behavior.
- Only include when the subject line alone is not sufficient to explain the change.

### Message Footer (optional)

#### Breaking changes
Prefix with `BREAKING CHANGE:` and describe the change, justification, and migration notes.

#### Referencing issues
Use `Closes #<issue>` (e.g. `Closes #234` or `Closes #123, #245`).

## Rules

- Write the subject in **English**.
- Keep the message concise and meaningful.
- Do NOT include a body or footer if the change is straightforward and the subject alone is clear enough.
- Choose the most accurate `<type>` based on the actual diff content.
- Choose a `<scope>` that best represents the area of the codebase being changed.

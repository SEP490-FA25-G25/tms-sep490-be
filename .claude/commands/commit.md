# Conventional Commit Helper

Analyzes your changes and generates a conventional commit message following the project's commit convention.

## Usage

```bash
/commit
```

## What it does

1. Runs `git status` to see all changes
2. Runs `git diff` to analyze staged and unstaged changes
3. Generates a conventional commit message following this format:

```
<type>(optional scope): <short description>

[optional body]

[optional footer(s)]
```

## Commit Types

| type         | Ý nghĩa                                                       |
| ------------ | ------------------------------------------------------------- |
| **feat**     | Thêm tính năng mới                                            |
| **fix**      | Sửa lỗi                                                       |
| **docs**     | Cập nhật tài liệu (README, docs/, comment code)               |
| **style**    | Không ảnh hưởng logic (format, prettier, spacing, semicolon…) |
| **refactor** | Thay đổi code nhưng **không** thêm tính năng, không fix bug   |
| **perf**     | Tối ưu hiệu năng                                              |
| **test**     | Thêm hoặc sửa test                                            |
| **chore**    | Việc linh tinh (update config, dependency, script…)           |
| **build**    | Thay đổi liên quan đến build system, CI/CD                    |
| **ci**       | Thay đổi file CI (Github Actions, Jenkins, Gitlab CI)         |
| **revert**   | Hoàn tác commit trước                                         |

## Output

The command will provide:
- A suggested conventional commit message
- Analysis of what changed
- NO automatic staging or committing - you review and commit manually

## Example Output

```
Suggested commit message:

feat(auth): add JWT refresh token rotation

- Implement automatic token rotation on refresh
- Add refresh token repository and service
- Update authentication filter to handle rotation

Closes #123
```

You can then review and use this message with:
```bash
git add .
git commit -m "feat(auth): add JWT refresh token rotation"
```
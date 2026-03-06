在 **GitHub Actions** 中，“Checks 项目名称”（也就是 PR 页面里显示的 **check run / job 名称**）通常来源于 **workflow 名称 + job 名称**。同时 GitHub 还提供大量 **环境变量（GITHUB_*）** 和 **context（github.*）** 可用于 CI/CD 控制。

我分三部分说明：

1️⃣ 如何获取 **Check 项目名称**
2️⃣ GitHub 常用环境变量
3️⃣ GitHub 常用 context 变量（比 env 更重要）

---

# 一、如何获取 GitHub Check 项目名称

在 PR 的 **Checks** 页面看到的名称一般是：

```
workflow_name / job_name
```

例如：

```
CI / build-linux
CI / test
```

来源：

```yaml
name: CI

jobs:
  build-linux:
    runs-on: ubuntu-latest
```

---

## 1 获取 workflow 名称

环境变量：

```bash
echo $GITHUB_WORKFLOW
```

或 expression：

```yaml
${{ github.workflow }}
```

---

## 2 获取 job 名称

```yaml
${{ github.job }}
```

例如：

```bash
echo "job=$GITHUB_JOB"
```

---

## 3 获取完整 Check 名称

GitHub 不直接提供，但可以组合：

```bash
echo "$GITHUB_WORKFLOW / $GITHUB_JOB"
```

例如输出：

```
CI / build-linux
```

---

# 二、GitHub 常用环境变量（GITHUB_*）

GitHub 在 runner 中自动注入很多变量。

官方最常用的如下。

---

## 1 仓库相关

| 变量                        | 含义                                               |
| ------------------------- | ------------------------------------------------ |
| `GITHUB_REPOSITORY`       | owner/repo                                       |
| `GITHUB_REPOSITORY_OWNER` | repo owner                                       |
| `GITHUB_SERVER_URL`       | [https://github.com](https://github.com)         |
| `GITHUB_API_URL`          | [https://api.github.com](https://api.github.com) |

示例：

```bash
echo $GITHUB_REPOSITORY
```

输出：

```
org/project
```

---

## 2 workflow 相关

| 变量                   | 含义              |
| -------------------- | --------------- |
| `GITHUB_WORKFLOW`    | workflow 名称     |
| `GITHUB_JOB`         | job 名称          |
| `GITHUB_RUN_ID`      | workflow run ID |
| `GITHUB_RUN_NUMBER`  | run 序号          |
| `GITHUB_RUN_ATTEMPT` | 重试次数            |

示例：

```bash
echo $GITHUB_WORKFLOW
echo $GITHUB_JOB
```

---

## 3 Git 相关

| 变量                | 含义           |
| ----------------- | ------------ |
| `GITHUB_SHA`      | commit SHA   |
| `GITHUB_REF`      | 当前 ref       |
| `GITHUB_REF_NAME` | 分支名          |
| `GITHUB_REF_TYPE` | branch / tag |

示例：

```bash
echo $GITHUB_SHA
echo $GITHUB_REF_NAME
```

输出：

```
main
```

---

## 4 事件相关

| 变量                  | 含义            |
| ------------------- | ------------- |
| `GITHUB_EVENT_NAME` | 触发事件          |
| `GITHUB_EVENT_PATH` | event JSON 文件 |

示例：

```bash
echo $GITHUB_EVENT_NAME
```

可能值：

```
push
pull_request
workflow_dispatch
schedule
release
```

---

## 5 runner 相关

| 变量                 | 含义                      |
| ------------------ | ----------------------- |
| `RUNNER_OS`        | Linux / macOS / Windows |
| `RUNNER_ARCH`      | X64 / ARM64             |
| `RUNNER_NAME`      | runner 名称               |
| `RUNNER_TEMP`      | 临时目录                    |
| `RUNNER_WORKSPACE` | 工作目录                    |

示例：

```bash
echo $RUNNER_OS
```

---

## 6 路径相关

| 变量                 | 含义          |
| ------------------ | ----------- |
| `GITHUB_WORKSPACE` | checkout 目录 |
| `HOME`             | 用户 home     |
| `PATH`             | PATH        |

示例：

```bash
cd $GITHUB_WORKSPACE
```

---

# 三、最常用 GitHub Context（更重要）

在 YAML 中通常使用：

```
${{ github.xxx }}
```

而不是 shell env。

---

## 1 github context

| 变量                  | 含义         |
| ------------------- | ---------- |
| `github.repository` | owner/repo |
| `github.workflow`   | workflow 名 |
| `github.job`        | job 名      |
| `github.sha`        | commit     |
| `github.ref`        | ref        |
| `github.ref_name`   | branch     |

示例：

```yaml
run: echo ${{ github.ref_name }}
```

---

## 2 PR 相关

| 变量                                   | 含义        |
| ------------------------------------ | --------- |
| `github.event.pull_request.title`    | PR 标题     |
| `github.event.pull_request.number`   | PR ID     |
| `github.event.pull_request.head.ref` | PR branch |

示例：

```yaml
${{ github.event.pull_request.title }}
```

---

## 3 commit message

push 事件：

```yaml
${{ github.event.head_commit.message }}
```

PR merge：

```yaml
${{ github.event.pull_request.title }}
```

---

# 四、查看所有环境变量

调试最简单的方法：

```yaml
- name: Debug env
  run: env | sort
```

或者：

```yaml
- name: Debug github context
  run: echo '${{ toJson(github) }}'
```

---

# 五、最常用的变量（90% CI 都会用）

| 变量                  | 用途            |
| ------------------- | ------------- |
| `github.ref_name`   | branch        |
| `github.sha`        | commit        |
| `github.repository` | repo          |
| `github.event_name` | event         |
| `github.run_number` | build version |
| `runner.os`         | OS            |

---

# 六、常见 CI/CD 使用示例

生成 Docker tag：

```yaml
env:
  IMAGE_TAG: ${{ github.ref_name }}-${{ github.run_number }}
```

输出：

```
main-123
```

---

# 七、生产环境最常用变量组合

构建版本号：

```yaml
VERSION=${{ github.ref_name }}-${{ github.run_number }}-${{ github.sha }}
```

例如：

```
main-124-a1b2c3d
```

---

💡 如果你需要，我可以再整理一份 **GitHub Actions 变量速查表（50+变量）**，包括：

* PR / push / tag 的变量区别
* merge commit 特殊变量
* matrix / strategy 变量
* artifact / cache 变量

做 CI/CD 时非常实用。



如果仓库是：

```
https://github.com/graalvm/setup-graalvm
```

你想获取 **项目名 `setup-graalvm`（repository name）**，在 **GitHub Actions** 里有几种最常用方法。

---

# 1️⃣ 推荐方法：`github.event.repository.name`

这是 **最准确的方法**。

```yaml
${{ github.event.repository.name }}
```

示例：

```yaml
- name: Get repo name
  run: echo "${{ github.event.repository.name }}"
```

输出：

```
setup-graalvm
```

---

# 2️⃣ 使用 `github.repository` 再拆分

GitHub 默认变量：

```yaml
${{ github.repository }}
```

返回：

```
graalvm/setup-graalvm
```

然后在 shell 中获取 repo name：

```yaml
- name: Extract repo name
  run: |
    REPO_NAME=$(basename "$GITHUB_REPOSITORY")
    echo $REPO_NAME
```

输出：

```
setup-graalvm
```

---

# 3️⃣ 直接使用 shell（最通用）

因为环境变量：

```
GITHUB_REPOSITORY=graalvm/setup-graalvm
```

所以：

```bash
basename "$GITHUB_REPOSITORY"
```

完整示例：

```yaml
- name: Get repo name
  run: |
    echo "Repo: $(basename $GITHUB_REPOSITORY)"
```

---

# 4️⃣ 在 YAML 变量中使用

```yaml
env:
  PROJECT_NAME: ${{ github.event.repository.name }}
```

使用：

```yaml
- run: echo $PROJECT_NAME
```

---

# 5️⃣ 在 matrix 或 tag 中使用

例如生成 Docker image：

```yaml
env:
  IMAGE_NAME: ghcr.io/${{ github.repository }}
```

输出：

```
ghcr.io/graalvm/setup-graalvm
```

如果只要 repo：

```yaml
env:
  IMAGE_NAME: ghcr.io/graalvm/${{ github.event.repository.name }}
```

---

# 6️⃣ 推荐总结

| 方法                                    | 推荐    |
| ------------------------------------- | ----- |
| `${{ github.event.repository.name }}` | ⭐⭐⭐⭐⭐ |
| `basename $GITHUB_REPOSITORY`         | ⭐⭐⭐⭐  |
| `${{ github.repository }}`            | ⭐⭐⭐   |

**最佳实践：**

```yaml
${{ github.event.repository.name }}
```

---

💡 **额外补充（很多人不知道）**

GitHub 其实还有几个相关变量：

| 变量                             | 示例                    |
| ------------------------------ | --------------------- |
| `github.repository`            | graalvm/setup-graalvm |
| `github.repository_owner`      | graalvm               |
| `github.event.repository.name` | setup-graalvm         |

组合可以得到：

```yaml
OWNER=${{ github.repository_owner }}
REPO=${{ github.event.repository.name }}
```

输出：

```
graalvm/setup-graalvm
```

---

如果你愿意，我可以再给你一份 **GitHub Actions 30个最常用变量速查表（CI/CD必备）**，很多高级用法（如 PR、tag、release 自动化）都会用到。

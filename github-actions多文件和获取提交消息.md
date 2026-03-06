在 **GitHub Actions** 中，`.github/workflows/` 目录下的 **两个 workflow yml 文件**如果需要 **关联触发**，通常有三种主流方式：

1️⃣ `workflow_run`（最常用）
2️⃣ `workflow_call`（可复用 workflow）
3️⃣ `repository_dispatch` / `workflow_dispatch`（API 或手动触发）

同时 **定时触发**使用 `schedule (cron)`。

我分别给你一个 **生产常用示例**。

---

# 一、workflow A 触发 workflow B（推荐：workflow_run）

适合场景：

* CI 构建完成后触发部署
* build → test → deploy pipeline

---

## 1 workflow A（build.yml）

`.github/workflows/build.yml`

```yaml
name: Build

on:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: checkout
        uses: actions/checkout@v4

      - name: build
        run: |
          echo "build project"
          sleep 5
```

---

## 2 workflow B（deploy.yml）

`.github/workflows/deploy.yml`

```yaml
name: Deploy

on:
  workflow_run:
    workflows: ["Build"]
    types:
      - completed

jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
    runs-on: ubuntu-latest

    steps:
      - name: deploy
        run: echo "deploy project"
```

---

### 触发流程

```
git push
   │
   ▼
Build workflow
   │
   ▼
Deploy workflow
```

---

# 二、workflow A 调用 workflow B（推荐企业级）

使用 **workflow_call**。

优点：

* B workflow 可复用
* 支持参数传递
* monorepo 常用

---

## workflow B（reusable workflow）

`.github/workflows/deploy.yml`

```yaml
name: Deploy Workflow

on:
  workflow_call:
    inputs:
      env:
        required: true
        type: string

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy to ${{ inputs.env }}"
```

---

## workflow A 调用

`.github/workflows/build.yml`

```yaml
name: Build

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "build"

  deploy:
    needs: build
    uses: ./.github/workflows/deploy.yml
    with:
      env: prod
```

---

# 三、API 触发 workflow（高级）

使用 `repository_dispatch`。

```yaml
on:
  repository_dispatch:
    types: [deploy]
```

触发：

```bash
curl -X POST \
  -H "Authorization: token <TOKEN>" \
  https://api.github.com/repos/owner/repo/dispatches \
  -d '{"event_type":"deploy"}'
```

---

# 四、定时触发 workflow

GitHub 使用 **cron**。

`.github/workflows/schedule.yml`

```yaml
name: Scheduled Job

on:
  schedule:
    - cron: "0 2 * * *"
```

---

## cron 规则

格式：

```
* * * * *
│ │ │ │ │
│ │ │ │ └─ day of week (0-6)
│ │ │ └── month
│ │ └──── day of month
│ └────── hour
└──────── minute
```

---

### 常见示例

每天凌晨 2 点

```yaml
cron: "0 2 * * *"
```

每 30 分钟

```yaml
cron: "*/30 * * * *"
```

每周一

```yaml
cron: "0 3 * * 1"
```

每月 1 号

```yaml
cron: "0 0 1 * *"
```

---

⚠️ 注意

* GitHub cron **使用 UTC 时间**
* 例如北京时间凌晨 2 点：

```
18:00 UTC
```

配置：

```yaml
cron: "0 18 * * *"
```

---

# 五、组合示例（生产推荐）

**CI 构建 → 成功后部署 → 每天定时检查**

```
build.yml
   │
   ▼
deploy.yml
   │
   ▼
schedule.yml
```

---

# 六、企业级最佳实践

推荐结构：

```
.github/workflows/

ci.yml
build.yml
deploy.yml
reusable-build.yml
reusable-deploy.yml
schedule-clean.yml
```

常见 pipeline：

```
push
 │
 ▼
build
 │
 ▼
test
 │
 ▼
docker build
 │
 ▼
deploy
```

---

# 七、很多人不知道的高级技巧（非常实用）

可以 **workflow A 触发 workflow B 并传递 artifact**：

```
build
   │
   ▼
upload artifact
   │
   ▼
workflow_run
   │
   ▼
download artifact
```

适合：

* docker image
* build jar
* release package

---

如果你愿意，我可以给你一套 **企业级 GitHub Actions CI/CD 模板**（非常完整）：

包含：

* Maven / Gradle build
* Docker build
* DockerHub push
* Kubernetes deploy
* 定时任务
* workflow 复用
* multi-environment（dev / test / prod）

一套 **生产级 `.github/workflows` 结构**。



在 **GitHub Actions** 中，如果你想让 **某个 workflow yml 不被触发**，有多种方式，取决于你的需求（临时关闭、条件控制、分支控制等）。下面是生产环境常用的几种方法。

---

# 一、最简单：禁用整个 workflow（推荐）

在 workflow 文件中加入：

```yaml
on: {}
```

例如：

```yaml id="wf_disable"
name: Deploy

on: {}

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy"
```

效果：

* 这个 workflow **永远不会自动触发**。

适合：

* 临时关闭 CI
* 暂停某个 pipeline

---

# 二、通过条件控制 `if`（推荐生产）

可以在 **job 级别**控制。

```yaml id="wf_if_disable"
jobs:
  deploy:
    if: false
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy"
```

效果：

* workflow 会触发
* 但 job **不会执行**

---

# 三、通过分支控制

只允许某些分支触发。

```yaml id="wf_branch_filter"
on:
  push:
    branches:
      - main
```

如果 push 到其他分支：

```text
dev
feature/*
```

workflow **不会触发**。

---

# 四、通过 `paths-ignore`

如果某些文件变更时不触发。

```yaml id="wf_paths_ignore"
on:
  push:
    paths-ignore:
      - README.md
      - docs/**
```

效果：

修改这些文件不会触发 workflow。

---

# 五、通过 commit message 控制（非常实用）

GitHub Actions **内置支持**：

```text
[skip ci]
```

或

```text
[ci skip]
```

例如：

```bash
git commit -m "update readme [skip ci]"
```

效果：

* **所有 workflow 都不会触发**

---

# 六、只允许手动触发

删除自动触发，只保留：

```yaml id="wf_manual"
on:
  workflow_dispatch:
```

效果：

* 只能在 GitHub UI 手动运行。

---

# 七、控制 workflow_run 触发

如果 workflow B 由 workflow A 触发：

```yaml
on:
  workflow_run:
    workflows: ["Build"]
```

可以通过条件阻止：

```yaml id="wf_run_if"
jobs:
  deploy:
    if: ${{ github.event.workflow_run.conclusion == 'success' }}
```

或者直接：

```yaml
if: false
```

---

# 八、企业级推荐方式（最优）

通常使用 **环境变量开关**。

```yaml id="wf_env_switch"
env:
  ENABLE_DEPLOY: false

jobs:
  deploy:
    if: env.ENABLE_DEPLOY == 'true'
    runs-on: ubuntu-latest
    steps:
      - run: echo "deploy"
```

开启时：

```yaml
ENABLE_DEPLOY: true
```

---

# 九、GitHub UI 禁用 workflow

在 GitHub 页面：

```
Actions
   ↓
选择 workflow
   ↓
Disable workflow
```

这样 **yml 仍存在，但不会运行**。

---

# 十、最佳实践总结

| 方法                  | 推荐程度  | 场景       |
| ------------------- | ----- | -------- |
| `on: {}`            | ⭐⭐⭐⭐⭐ | 完全禁用     |
| `if: false`         | ⭐⭐⭐⭐  | 临时禁用 job |
| `[skip ci]`         | ⭐⭐⭐⭐  | 单次提交     |
| `workflow_dispatch` | ⭐⭐⭐⭐  | 手动执行     |
| `paths-ignore`      | ⭐⭐⭐   | 忽略文件     |
| branch filter       | ⭐⭐⭐⭐  | 控制分支     |

---

✅ **生产最常见做法**

```yaml
if: github.ref == 'refs/heads/main'
```

只在 main 执行。

---

如果你愿意，我可以再给你一份 **企业级 GitHub Actions 控制触发的完整最佳实践（20+技巧）**，包括：

* monorepo 触发控制
* 多 workflow 依赖
* 环境隔离（dev/test/prod）
* 避免 CI 无限循环触发

这一套在大型项目里非常重要。


在 **GitHub Actions** 里，你可以通过 **commit message 内容** 来控制 workflow 是否执行，例如只有 commit message **包含 `deploy prod`** 时才触发部署。核心方法是使用 **`if` 条件 + github context**。

同时要区分：

* `push`
* `pull_request`
* `tag`
* `merge commit`

可以统一通过 **commit message / PR title / tag message** 进行判断。

下面给你一套 **生产可用的完整方案**。

---

# 一、基础 workflow 触发

先允许 workflow 在这些事件触发：

```yaml
on:
  push:
    branches:
      - main
    tags:
      - "*"

  pull_request:
    branches:
      - main
```

说明：

* push 到 main
* push tag
* pull request
* merge 也属于 push

---

# 二、通过 commit message 控制触发

关键点：

```yaml
if: contains(github.event.head_commit.message, 'deploy prod')
```

示例：

```yaml
jobs:
  deploy:
    if: contains(github.event.head_commit.message, 'deploy prod')
    runs-on: ubuntu-latest

    steps:
      - run: echo "Deploy to production"
```

---

# 三、区分 push / PR / tag / merge

不同事件 commit message 的路径不同。

| 事件           | message 位置                         |
| ------------ | ---------------------------------- |
| push         | `github.event.head_commit.message` |
| pull_request | `github.event.pull_request.title`  |
| tag push     | `github.ref`                       |
| merge commit | 仍然是 push                           |

---

# 四、完整生产示例

```yaml
name: Deploy

on:
  push:
    branches: [ main ]
    tags: [ "*" ]

  pull_request:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest

    if: |
      (
        github.event_name == 'push' &&
        contains(github.event.head_commit.message, 'deploy prod')
      )
      ||
      (
        github.event_name == 'pull_request' &&
        contains(github.event.pull_request.title, 'deploy prod')
      )
      ||
      (
        startsWith(github.ref, 'refs/tags/') &&
        contains(github.ref, 'deploy-prod')
      )

    steps:
      - name: checkout
        uses: actions/checkout@v4

      - name: deploy
        run: echo "Deploying to PROD"
```

---

# 五、示例触发方式

## push commit

```bash
git commit -m "fix bug deploy prod"
git push
```

触发 deploy。

---

## PR

PR 标题：

```
feature update deploy prod
```

PR workflow 执行。

---

## tag

```bash
git tag deploy-prod-v1.0
git push origin deploy-prod-v1.0
```

触发 deploy。

---

## merge commit

如果 merge commit message：

```
merge feature branch deploy prod
```

也会触发。

---

# 六、企业级推荐方式（更安全）

很多团队不会直接写 `"deploy prod"`，而是使用 **commit 标记**：

```
[deploy-prod]
```

判断：

```yaml
contains(github.event.head_commit.message, '[deploy-prod]')
```

示例 commit：

```
fix: update service [deploy-prod]
```

---

# 七、避免误触发（非常重要）

推荐增加 **branch 限制**：

```yaml
if: |
  github.ref == 'refs/heads/main' &&
  contains(github.event.head_commit.message, '[deploy-prod]')
```

这样：

| branch | deploy |
| ------ | ------ |
| main   | ✅      |
| dev    | ❌      |

---

# 八、PR merge 后触发 deploy（推荐生产）

```yaml
if: |
  github.event_name == 'push' &&
  github.ref == 'refs/heads/main' &&
  contains(github.event.head_commit.message, '[deploy-prod]')
```

流程：

```
PR → merge main → commit message包含[deploy-prod] → deploy
```

---

# 九、最佳实践 commit 规范

推荐：

```
feat: new feature
fix: bug fix
release: v1.2.0
deploy: prod
```

或者：

```
[deploy-prod]
[deploy-test]
[deploy-dev]
```

这样可以控制多个环境。

---

# 十、最推荐的生产配置

```yaml
if: |
  github.event_name == 'push' &&
  github.ref == 'refs/heads/main' &&
  contains(github.event.head_commit.message, '[deploy-prod]')
```

---

# 十一、企业级 CI/CD 流程（推荐）

```
PR
 │
 ▼
CI build
 │
 ▼
merge main
 │
 ▼
commit message: [deploy-prod]
 │
 ▼
Deploy workflow
```

---

如果你愿意，我可以给你一套 **企业级 GitHub Actions Deploy 控制方案**，非常适合生产：

包括：

* commit 控制 deploy
* tag release deploy
* PR preview deploy
* dev / test / prod 环境
* monorepo 触发优化
* 防止 workflow 循环触发

一整套 **大型项目 CI/CD 设计**。



在 **GitHub Actions** 中，跨 `.github/workflows/*.yml` 文件传递变量值主要有 **4 种生产常用方法**。不同方式适用于不同场景（是否同步、是否跨 workflow、是否需要 artifact）。

我按 **推荐程度 + 实际企业使用情况**给你讲清楚。

---

# 一、推荐方式：`workflow_call` 传递变量（最标准）

适合：

* 一个 workflow 调用另一个 workflow
* 需要传递参数
* 企业 CI/CD 常用

---

## 1 调用方（build.yml）

```yaml
name: Build

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      image: ${{ steps.vars.outputs.image }}

    steps:
      - name: set vars
        id: vars
        run: echo "image=my-app:1.0" >> $GITHUB_OUTPUT

  deploy:
    needs: build
    uses: ./.github/workflows/deploy.yml
    with:
      image: ${{ needs.build.outputs.image }}
```

---

## 2 被调用 workflow（deploy.yml）

```yaml
name: Deploy

on:
  workflow_call:
    inputs:
      image:
        required: true
        type: string

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploy image ${{ inputs.image }}"
```

---

### 执行效果

```
build.yml
   │
   ▼
deploy.yml
   │
   ▼
image=my-app:1.0
```

---

# 二、使用 `workflow_run` + artifact（跨 workflow 最常用）

适合：

* workflow A 完成后触发 workflow B
* 需要传递文件 / 变量

---

## workflow A

```yaml
name: Build

on:
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: save variable
        run: |
          echo "IMAGE=myapp:1.0" > build.env

      - uses: actions/upload-artifact@v4
        with:
          name: build-vars
          path: build.env
```

---

## workflow B

```yaml
name: Deploy

on:
  workflow_run:
    workflows: ["Build"]
    types: [completed]

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/download-artifact@v4
        with:
          name: build-vars

      - name: load vars
        run: |
          source build.env
          echo "Deploy $IMAGE"
```

---

# 三、使用 repository_dispatch 传递 payload

适合：

* API 触发
* 外部系统触发 CI

---

触发：

```bash
curl -X POST \
-H "Authorization: token TOKEN" \
https://api.github.com/repos/owner/repo/dispatches \
-d '{
  "event_type": "deploy",
  "client_payload": {
    "image": "myapp:1.0"
  }
}'
```

---

workflow：

```yaml
on:
  repository_dispatch:
    types: [deploy]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - run: echo ${{ github.event.client_payload.image }}
```

---

# 四、使用 GitHub ENV / Outputs（同 workflow）

如果只是 **job 之间**：

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    outputs:
      version: ${{ steps.v.outputs.version }}

    steps:
      - id: v
        run: echo "version=1.0" >> $GITHUB_OUTPUT

  deploy:
    needs: build
    runs-on: ubuntu-latest

    steps:
      - run: echo ${{ needs.build.outputs.version }}
```

---

# 五、企业级推荐架构

大型项目通常这样：

```
.github/workflows/

ci.yml
build.yml
test.yml
docker.yml
deploy.yml
release.yml
```

执行链：

```
push
 │
 ▼
build.yml
 │
 ▼
docker.yml
 │
 ▼
deploy.yml
```

变量通过：

```
workflow_call
artifact
```

传递。

---

# 六、变量传递能力对比

| 方法                      | 是否跨 workflow | 是否推荐  |
| ----------------------- | ------------ | ----- |
| workflow_call           | ✅            | ⭐⭐⭐⭐⭐ |
| workflow_run + artifact | ✅            | ⭐⭐⭐⭐  |
| repository_dispatch     | ✅            | ⭐⭐⭐   |
| job outputs             | ❌            | ⭐⭐⭐⭐  |

---

# 七、最佳实践（生产推荐）

如果是 **跨 workflow**

优先：

```
workflow_call
```

如果是 **workflow A → workflow B**

优先：

```
workflow_run + artifact
```

---

# 八、一个完整企业 CI/CD 示例

```
build.yml
   │
   │ image tag
   ▼
docker.yml
   │
   │ image:1.0
   ▼
deploy.yml
```

变量：

```
IMAGE_TAG
DOCKER_IMAGE
K8S_NAMESPACE
```

通过 `workflow_call` 传递。

---

💡 如果你愿意，我可以给你一套 **非常完整的 GitHub Actions 企业级 CI/CD 模板**，包含：

* **跨 yml 变量传递**
* **PR / push / tag 控制**
* **commit message 控制 deploy**
* **多环境 dev / test / prod**
* **Docker + Kubernetes 自动部署**
* **monorepo 触发优化**

这一套在大厂 CI/CD 里非常常见。

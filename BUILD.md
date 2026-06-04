# Vivid TV — 构建与打包指南

本指南包含两种打包方式：**GitHub Actions 全自动云端打包**（推荐）和 **本地开发环境手动打包**。

---

## 📦 方式一：GitHub Actions 全自动打包（推荐）

### 前置条件
1. 将代码推送至 GitHub 仓库
2. 设置 3 个 GitHub Actions Secrets（仅 Release 需要）

### 步骤

#### 1. 推送到 GitHub

```bash
# 创建 GitHub 仓库后
git remote add origin https://github.com/你的用户名/vivid-tv.git
git branch -M master
git push -u origin master
```

#### 2. 自动触发 Debug 构建

每次 `git push` 后，Actions 自动运行：
- **validate** job: Lint + 单元测试
- **build-debug** job: 输出 `vivid-tv-debug.apk`

> ⚡ 推代码 → 等 3-5 分钟 → 在 GitHub Actions 页面下载 APK

#### 3. Release 构建（签名 APK）

打 Tag 自动触发 Release 构建：

```bash
git tag v1.0.0
git push origin v1.0.0
```

**在此之前需要在 GitHub 仓库设置 Secrets：**

| Secret | 用途 |
|--------|------|
| `KEYSTORE_BASE64` | 签名文件 (.jks) 的 Base64 编码 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 签名密钥别名 |
| `KEY_PASSWORD` | 签名密钥密码 |

**生成签名 + 配置 Secrets 的方法：**

```bash
# 1. 生成签名文件（在任意 JDK 环境执行）
keytool -genkey -v -keystore vividtv.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias vividtv_release -storepass changeit -keypass changeit

# 2. 转为 Base64
base64 -w0 vividtv.jks  # macOS/Linux
# 或用 PowerShell: [Convert]::ToBase64String([IO.File]::ReadAllBytes("vividtv.jks"))

# 3. 把输出的完整字符串复制到 GitHub Secrets → KEYSTORE_BASE64
# 4. 把 storepass 值设到 KEYSTORE_PASSWORD
# 5. alias 设到 KEY_ALIAS
# 6. keypass 设到 KEY_PASSWORD
```

#### 4. 下载 APK

构建完成后：
1. 打开 GitHub 仓库 → Actions 页面
2. 点击最新的 workflow run
3. 在 **Artifacts** 区域下载 `vivid-tv-debug-apk` 或 `vivid-tv-release-apk`

---

## 🔧 方式二：本地环境手动打包

### 1. 部署 Android SDK 环境

**Windows:**
```powershell
# 安装 Chocolatey（管理员 PowerShell）
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# 安装 JDK 17 + Android SDK
choco install -y temurin17 android-sdk

# 设置环境变量
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "$env:USERPROFILE\AppData\Local\Android\Sdk", "User")
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot", "User")
```

**macOS:**
```bash
brew install --cask temurin17 android-sdk
export ANDROID_HOME=/usr/local/share/android-sdk
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
```

**Linux / WSL:**
```bash
# 安装 JDK 17
sudo apt update && sudo apt install -y openjdk-17-jdk unzip

# 安装 Android SDK Command Line Tools
mkdir -p ~/Android/Sdk && cd ~/Android/Sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-*.zip
mkdir cmdline-tools/latest && mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
# 正确结构：cmdline-tools/latest/bin/sdkmanager

# 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 下载 SDK 组件
yes | sdkmanager --sdk_root=$ANDROID_HOME \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "platform-tools"
```

### 2. 验证环境

```bash
java -version
echo $ANDROID_HOME
ls $ANDROID_HOME/platforms/android-35/
```

### 3. 编译 Debug APK

```bash
cd vivid-tv
chmod +x gradlew

# Debug 构建（不需要签名）
./gradlew assembleDebug

# 输出位置：
# app/build/outputs/apk/debug/vivid-tv-debug.apk
```

### 4. 运行单元测试

```bash
./gradlew testDebugUnitTest
# 测试报告在：app/build/reports/tests/testDebugUnitTest/index.html
```

### 5. 编译 Release APK（需要签名）

```bash
# 1. 生成签名
keytool -genkey -v -keystore ~/vividtv.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias vividtv_release

# 2. 构建
KEYSTORE_PATH=~/vividtv.jks \
KEYSTORE_PASSWORD=your_pwd \
KEY_ALIAS=vividtv_release \
KEY_PASSWORD=your_pwd \
./gradlew assembleRelease

# 输出位置：
# app/build/outputs/apk/release/vividtv-release.apk
```

---

## 📋 常见问题

### Q: `local.properties` 去哪里了？
A: 它被 `.gitignore` 排除。GitHub Actions 上不需要。本地首次编译 Gradle 会自动生成。

### Q: 编译报 `compileSdk 35` 找不到？
A: 需要下载 API 35：
```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

### Q: 哪些命令最常用？
```bash
./gradlew assembleDebug       # 编译 Debug APK（主要）
./gradlew assembleRelease     # 编译 Release APK
./gradlew testDebugUnitTest   # 运行单元测试
./gradlew lintDebug           # 代码静态分析
./gradlew clean               # 清理构建
```

### Q: Debug APK 可以直接装到电视上吗？
A: 可以。Debug 版本使用 Android SDK 自带的 debug.keystore 签名，可以直接通过 adb 安装：
```bash
adb install app/build/outputs/apk/debug/vivid-tv-debug.apk
```

---

## 📊 CI/CD 流程图

```
Git Push / PR
      │
      ▼
┌─────────────────────────────┐
│  validate (Lint + Tests)     │
│  ├── lintDebug               │
│  └── testDebugUnitTest       │
└─────────┬───────────────────┘
          │ passed
          ▼
┌─────────────────────────────┐
│  build-debug                 │
│  └── assembleDebug           │
│  └── 📦 APK Artifact         │
└─────────────────────────────┘
          │ tag push (v*)
          ▼
┌─────────────────────────────┐
│  build-release               │
│  ├── decode keystore         │
│  ├── assembleRelease         │
│  ├── 📦 APK Artifact         │
│  └── 🏷️ GitHub Release       │
└─────────────────────────────┘
```

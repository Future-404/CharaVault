# CharaVault 🎴 (角色卡珍藏馆)

一款专为 AI 角色扮演爱好者设计的轻量级 Android 角色卡本地管理应用。

## 🌟 核心特性
- **支持 Character Card Spec V3 / V2 规范**：完美解析与合成包含 JSON 数据的 PNG 角色卡。
- **书架/货架式画廊布局 (Grouped Horizontal Carousels)**：
  - 统一按 **🏷️ 标签 (Tags)** 拆分横向轮播展示列表
  - 支持竖向滚动的分层货架视觉体验
- **高效本地存储架构**：
  - 原汁原味保留物理 `.png` 角色卡文件于本地文件夹。
  - Jetpack Room 本地 SQLite 数据库实现毫秒级快速索引与搜索。
- **便携与导出**：
  - 支持选择 PNG/JSON 一键导入卡片。
  - 支持将编辑后的设定写回并导出为标准 PNG 角色卡。

## 🛠️ 技术栈
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose + Material Design 3
- **Database**: Jetpack Room
- **Async & Data**: Kotlin Coroutines & Flow
- **Image & JSON**: Coil + kotlinx.serialization / Custom PNG Chunk Parser

## 🚀 编译与构建
```bash
export ANDROID_HOME=/usr/lib/android-sdk
./gradlew assembleDebug
```
生成的 APK 文件存放在：`app/build/outputs/apk/debug/app-debug.apk`

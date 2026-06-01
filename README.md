# BookNext

Android 电子书阅读器，支持 EPUB/PDF/TXT/MOBI 格式。

## 技术栈

- Kotlin + Jetpack Compose + Hilt + Room + Retrofit
- Readium 3.1.2（EPUB 引擎）
- Android PdfRenderer（PDF 引擎）
- Coil（图片加载）
- DataStore（配置持久化）

## 项目结构

```
D:\code\BookNext\
├── app/build.gradle.kts         ← 构建配置（35个依赖）
├── gradle/libs.versions.toml    ← 版本目录
├── gradle.properties            ← android.useAndroidX=true
│
└── app/src/main/java/com/booknext/app/
    ├── BookNextApp.kt           ← @HiltAndroidApp
    ├── MainActivity.kt          ← FragmentActivity + Drawer框架
    │
    ├── data/
    │   ├── local/db/            ← Room（BookDao, AnnotationDao, AppDatabase, Entity）
    │   ├── local/prefs/         ← DataStore（UserPreferences）
    │   └── remote/
    │       ├── api/BookNextApi.kt   ← Retrofit接口（13个端点）
    │       ├── dto/                 ← 数据传输对象
    │       └── ApiClient.kt         ← 动态baseUrl + Auth拦截器
    │
    ├── di/                      ← Hilt模块（NetworkModule, DatabaseModule）
    ├── reader/ReadiumManager.kt ← Readium 3.1.2 初始化
    │
    └── ui/
        ├── NavGraph.kt          ← 导航（4页路由）
        ├── common/Theme.kt      ← 5色主题系统
        ├── drawer/              ← DrawerPage + DrawerContent（抽屉菜单）
        ├── bookshelf/           ← 书架 + BookshelfViewModel
        ├── recent/              ← 最近阅读
        ├── category/            ← 分类管理
        ├── local/               ← 本地文件（SAF导入）
        ├── cloud/               ← 云盘（HF Dataset用量）
        ├── reader/              ← 阅读器（ReaderScreen + ReaderViewModel）
        │   ├── epub/EpubReaderScreen.kt  ← Readium Fragment桥接
        │   ├── pdf/PdfReaderScreen.kt    ← PdfRenderer
        │   ├── txt/TxtReaderScreen.kt    ← LazyColumn
        │   ├── AnnotationDialog.kt       ← 标注弹窗
        │   ├── AnnotationSidebar.kt      ← 标注侧栏
        │   └── GestureReaderWrapper.kt   ← 手势系统
        ├── login/               ← 登录
        ├── settings/            ← 设置页
        ├── upload/              ← 上传书
        ├── stats/               ← 阅读统计
        ├── quotes/              ← 摘抄本
        ├── welcome/             ← 欢迎页
        └── widget/              ← 桌面Widget
```

## 构建环境

| 工具 | 路径 |
|------|------|
| JDK 21 | `D:\android-build\jdk\jdk-21.0.2+13` |
| Android SDK | `D:\android-build\android-sdk` |
| Gradle 8.7 | `D:\android-build\gradle\gradle-8.7` |
| 环境脚本 | `D:\android-build\env.ps1` |

## 构建命令

```powershell
. 'D:\android-build\env.ps1'
cd D:\code\BookNext
D:\android-build\gradle\gradle-8.7\bin\gradle.bat assembleDebug
```

APK 输出路径：`app/build/outputs/apk/debug/app-debug.apk`

## 关键功能点

| 功能 | 实现 |
|------|------|
| Readium 3.1.2 接入 | `EpubNavigatorFragment` + `CompositionLocal` Activity 注入 |
| FragmentActivity | `MainActivity` 继承 `FragmentActivity`，`LocalActivity` 全局可访问 |
| 夜间模式 | `Theme.DARK` / `Theme.LIGHT` 传入 `EpubPreferences` |
| 动态baseUrl | `ApiClient` 运行时读取 DataStore |
| MOBI转换 | 后端 `GET /api/convert/{id}` → EPUB → Readium |
| 阅读统计 | `totalReadingSeconds` Room字段聚合 |
| 主题商店 | 5色主题（`compositionLocalOf` 注入） |
| 自动推送 | `.git/hooks/post-commit` → `git push` |

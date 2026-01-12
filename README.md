# Android 课程表应用 (Course Schedule App)

这是一个基于 Android 的课程表应用，支持从教务系统导入课程表，并提供直观的课程查看功能。

## 📅 功能特性

*   **自动导入**: 通过内置浏览器登录教务系统，一键解析并导入课程表。
*   **日/周视图**: 清晰展示每天和每周的课程安排。
*   **本地存储**: 使用 Room 数据库并在本地保存数据，无网络也能查看。
*   **个性化设置**: (开发中/已有) 支持自定义背景、课程颜色等。

## 🏫 适用学校

本项目主要针对 **中国计量大学 (CJLU)** 的教务系统进行适配。

*   **默认地址**: `https://jwxt.cjlu.edu.cn/...`
*   **系统类型**: 适配了特定的 **正方教务系统** 网页结构。
*   **兼容性**: 如果其他学校也使用类似的网页结构（例如表格单元格 ID 为 `1-1`, `1-2` 格式，且内部包含 `div.timetable_con`），本应用可能也能兼容，但主要为 CJLU 优化。

## 📂 项目结构

项目采用标准的 Android 项目结构，主要代码位于 `app/src/main/java/com/example/course`：

*   **`ui`**: 包含所有的 Activity 和 Fragment，负责界面展示。
    *   `BrowserActivity.kt`: 内置浏览器，负责加载教务系统网页并触发提取课表逻辑。
    *   `ScheduleFragment.kt` (假设): 课程表主界面。
*   **`data`**: 数据层，包含 Room 数据库定义。
    *   `Course.kt`: 课程实体类。
    *   `AppDatabase.kt`: 数据库配置。
*   **`utils`**: 工具类。
    *   `CourseParser.kt`: 核心解析逻辑，使用 Jsoup 解析教务系统 HTML 提取课程信息。
*   **`worker`**: 后台任务（如适用）。

## 🛠️ 技术栈

*   **语言**: Kotlin
*   **UI**: Android Views (XML Layouts)
*   **网络/解析**: WebView, Jsoup
*   **数据库**: Jetpack Room
*   **架构**: MVVM (部分)

## 🚀 如何使用

1.  克隆本项目到本地。
2.  使用 Android Studio 打开项目。
3.  连接 Android 设备或模拟器运行应用。
4.  在应用中点击“导入课表”，登录教务系统，点击“获取课表”即可。

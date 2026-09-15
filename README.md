# Darcula Veil

> **状态：设计阶段。P1 已实现并构建通过，P2–P5 未实现。**
> 本文档主体是架构方案与技术调研结论。除第 5 节标注为"已实现"的部分外，其余均为设计稿，未落地为代码。
> P1 已通过 `./gradlew buildPlugin` 构建，但尚未在 IDE 中实际运行验证。

在 JetBrains IDE（Rider / IntelliJ IDEA）的 Tool Window 中嵌入外部内容，并让其呈现风格与 IDE 当前主题保持一致。

---

## 1. 目标与非目标

### 目标

- 在 Tool Window 中承载外部内容（网页内容 / 独立进程窗口）。
- 呈现层与 IDE 主题（Darcula / Light / 自定义配色）统一，不出现"贴进来一块异色矩形"的割裂感。
- 主题化强度运行时可调，可一键开关。
- 不破坏 IDE 自身的交互：焦点、快捷键、停靠与浮动切换都要正常。

### 非目标

- 不做 DRM 内容播放（见 §2.3 已知限制）。
- 不追求对外部进程渲染管线的像素级精确控制（见 §3.3 核心 trade-off）。
- 不做跨平台。当前设计仅覆盖 Windows；路线 A 天然跨平台，路线 B 深度依赖 Win32。

---

## 2. 路线 A：Web 内容（JCEF）

### 2.1 基本结构

IntelliJ Platform 自带 JCEF（Java Chromium Embedded Framework），无需任何 native hack：

```
ToolWindowFactory
  └─ JBCefBrowser              // 内嵌 Chromium
       ├─ component: JComponent  // 直接塞进 ToolWindow 的 ContentManager
       └─ executeJavaScript()    // 主题化注入通道
```

关键点：

- `JBCefApp.isSupported()` 必须先判，运行在不带 JCEF 的发行版上会直接抛异常。
- `JBCefBrowser` 必须显式 `Disposer.register(toolWindowDisposable, browser)`，否则 IDE 关闭时 CEF 子进程残留。
- 页面加载完成事件走 `CefLoadHandler.onLoadEnd`，注入必须挂在这个回调上——过早 `executeJavaScript` 会打进空文档。

### 2.2 主题化的三个档位

**档位一：CSS filter 注入（第一期实现）**

通过 `executeJavaScript` 往页面里塞一个 `<style>`，用 CSS filter 做全局调色：

```js
html {
  filter: invert(0.92) hue-rotate(180deg) saturate(0.85) brightness(0.95);
}
img, video, canvas, svg {
  filter: invert(1) hue-rotate(180deg);   /* 媒体元素反选回来 */
}
```

成本最低，副作用也最明显：媒体元素必须反选回来，否则图片全变负片；`filter` 会创建新的 containing block，`position: fixed` 的元素会错位。适合做"能用就行"的第一版，配一个强度滑杆（把 filter 各项参数做成变量）。

**档位二：色板重映射**

注入的 CSS 不做 filter，而是覆写 CSS 自定义属性，把页面配色直接映射到从 `JBColor` / `EditorColorsManager` 读出来的 IDE 实际色值。需要对目标页面的样式结构有先验知识，通用性差但效果远好于 filter。

**档位三：字符网格渲染（进阶方案）**

把整页当成一帧图像重新绘制：

```
逐帧 drawImage(source, offscreenCanvas)
  → ctx.getImageData() 取像素
  → 按 cell 尺寸降采样成字符网格
  → 每个 cell 按亮度选一个字形（从 " .:-=+*#%@" 这类 ramp 里取）
  → 用 JetBrains Mono + Darcula 色板渲染到 <pre>
```

这是最彻底的"归一化到 IDE 风格"，输出的东西在视觉上就是一块等宽文本区域，和编辑器天然同构。

**已知限制：**

- 依赖 `getImageData()`。如果媒体源跨域且没有 CORS header，canvas 会被 taint，`getImageData()` 直接抛 `SecurityError`。同源内容或带 `Access-Control-Allow-Origin` 的源才可行。
- 逐帧 `getImageData()` 在主线程上开销不低，高分辨率下需要先降分辨率再取像素，或把降采样搬到 WebGL。

### 2.3 绕开 canvas taint：走 CEF 的 OSR 层

如果要脱离 JS 沙箱的跨域限制，可以启用 CEF 的离屏渲染（Off-Screen Rendering）：

```
CefRenderHandler.onPaint(browser, type, dirtyRects, buffer, width, height)
  → buffer 是整页的 BGRA ByteBuffer
  → 后处理全部搬到 Java 侧
```

拿到的是 Chromium 合成后的完整像素，不受同源策略约束（同源策略约束的是 JS 读取，不是合成器输出）。代价是：

- OSR 模式下输入事件需要手动转发（`sendMouseEvent` / `sendKeyEvent`），JBCefBrowser 的默认窗口化模式不再适用。
- 帧率和延迟取决于 `onPaint` 回调频率，需要自己做节流。
- 这条路等于放弃了 IntelliJ 封装好的 JBCefBrowser，直接对 JCEF 底层 API 编程，维护成本显著上升。

**DRM：JCEF 不带 Widevine CDM。** 受 DRM 保护的流媒体在这条路线上放不了，这是发行版层面的限制，不是配置问题。本项目不尝试绕过，相关内容直接判为不支持。

---

## 3. 路线 B：原生窗口（HWND Reparent）

### 3.1 基本结构

```
ToolWindowFactory
  └─ java.awt.Canvas（heavyweight，必须有自己的 HWND）
       └─ JNA: Native.getComponentPointer(canvas) → HWND
            └─ SetParent(targetHwnd, canvasHwnd)
```

Reparent 后必须改目标窗口的 style，否则它会带着标题栏和边框挤在 Tool Window 里：

```
GWL_STYLE:    去掉 WS_POPUP | WS_CAPTION | WS_THICKFRAME，加上 WS_CHILD
GWL_EXSTYLE:  去掉 WS_EX_APPWINDOW，按需加 WS_EX_NOACTIVATE
SetWindowPos(..., SWP_FRAMECHANGED | SWP_NOZORDER)   // style 改完必须刷一次 frame
```

几何同步：监听 Canvas 的 `ComponentListener.componentResized`，在回调里 `SetWindowPos` 把子窗口拉到 Canvas 的 client rect。

焦点交接：IDE 进程和目标进程是两个线程输入队列，直接 `SetFocus` 跨进程无效，需要 `AttachThreadInput(ideThreadId, targetThreadId, TRUE)` 建立关联后再设焦点，并在失焦时解除关联——长期 attach 会让两个进程的输入状态互相污染。

### 3.2 为什么不能用 AWT 的绘制机制盖在上面

AWT 的 heavyweight / lightweight mixing（`sun.awt.mixing`）只对 JVM 自己创建的 peer 生效。外部进程的 HWND 不在 AWT 的组件树里，AWT 既不知道它的存在，也拿不到它的绘制时机，所以：

- 在 Canvas 上 `paint()` 画的任何东西都会被外部窗口的下一帧直接盖掉。
- 设 Canvas 透明、用 Swing glass pane 覆盖，同样无效——lightweight 组件永远画在 heavyweight 之下。

### 3.3 核心 trade-off：输入和呈现绑在同一个 HWND 上

| 方案 | 输入 | 像素控制 |
|---|---|---|
| **SetParent reparent** | ✅ 完美。窗口消息、RawInput、DirectInput 全部原生走通 | ❌ 拿不到。外部进程的 D3D / GDI 管线直接出像素，中间没有插入点 |
| **Windows Graphics Capture** | ❌ 断了。捕获出来的是纹理，不是窗口；RawInput / DirectInput 不走窗口消息队列，`PostMessage` 打不进去 | ✅ 完美。拿到 `IDirect3DSurface`，想上什么 shader 上什么 shader |

这是一个结构性矛盾，不是实现细节：**输入的正确性依赖"用户真的在操作那个 HWND"，而像素控制依赖"我们截走它的输出自己画"，两者互斥。**

尤其要注意 RawInput / DirectInput 这一条。很多目标程序（特别是游戏）不读 `WM_MOUSEMOVE` / `WM_KEYDOWN`，而是通过 `GetRawInputData` 或 DirectInput 设备直接取输入。这类输入路径完全绕过窗口消息队列，任何形式的消息合成（`PostMessage` / `SendInput` 定向投递）都无法覆盖。

### 3.4 折中方案：reparent + layered overlay 合成层

保留 reparent（输入完美），在其之上叠一个自己的子窗口作为主题化合成层：

```
Canvas HWND
  ├─ 目标窗口（reparent 进来，WS_CHILD）
  └─ overlay 窗口（自建，WS_CHILD | WS_EX_LAYERED | WS_EX_TRANSPARENT）
       ├─ WS_EX_LAYERED     → 可以用 UpdateLayeredWindow 做 per-pixel alpha
       └─ WS_EX_TRANSPARENT → 命中测试穿透，鼠标事件落到下层目标窗口
```

在 overlay 上绘制半透明的调色层（Darcula 主色 + 可调 alpha）、渐晕、扫描线之类的后处理效果。这不是真正的 shader 后处理——拿不到下层像素就没法做依赖源像素的效果（对比度调整、色彩重映射都做不了），只能做**叠加型**效果。但它完整保留了输入链路，并且 alpha 运行时可调。

Z-order 上 overlay 必须始终在目标窗口之上，目标窗口每次自己调 `SetWindowPos` 都可能打乱顺序，需要在几何同步的同时重新 `BringWindowToTop(overlay)`。

---

## 4. 风险清单

| 风险 | 说明 | 缓解方向 |
|---|---|---|
| **Anti-cheat 敏感** | 不少 anti-cheat 把 `SetParent` 到外部进程窗口、跨进程 `AttachThreadInput` 视为注入行为的特征，可能触发封禁或直接崩溃 | 路线 B 只对自己可控、无 anti-cheat 的目标程序启用；明确在文档里标注风险，不提供绕过手段 |
| **Per-monitor DPI 不一致** | IDE 进程和目标进程的 DPI awareness 不同时，`SetWindowPos` 传的坐标解释方式不同，会出现几何错位或缩放模糊 | 读 `GetDpiForWindow` 双端各取一次，在同步几何时做显式换算；跨屏拖动时重新计算 |
| **HWND 重建** | IDE 全屏切换、Tool Window 从停靠切到浮动、`Canvas` 被移除再添加，都会导致 AWT peer 销毁重建，旧 HWND 失效 | 监听 `HierarchyListener` 的 `DISPLAYABILITY_CHANGED`，在 `addNotify` 后重新取 HWND 并重新 reparent |
| **目标进程退出后残留黑区** | 子窗口销毁但 Canvas 不知道，留下一块没人绘制的区域 | 对目标进程句柄注册 `WaitForSingleObject` 或轮询 `IsWindow(targetHwnd)`，失效后切回占位面板 |
| **JCEF 子进程泄漏** | 未正确 dispose 的 `JBCefBrowser` 会留下 CEF helper 进程 | 全部走 `Disposer` 树，Tool Window 销毁即级联释放 |

---

## 5. 实施阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| **P1** | 路线 A 骨架：`ToolWindowFactory` + `JBCefBrowser` + 开关 Action + CSS filter 注入 + 老板键 | 已实现，构建通过，未运行验证 |
| **P2** | 主题化档位二：读 `EditorColorsManager` 色板，做 CSS 变量重映射；强度可调 UI | 设计中 |
| **P3** | 字符网格渲染；同源内容先行，跨域场景评估 OSR | 设计中 |
| **P4** | 路线 B 原型：reparent + 几何同步 + 焦点交接，限定无 anti-cheat 目标 | 设计中 |
| **P5** | Layered overlay 合成层，alpha 运行时可调 | 设计中 |

### P1 的两个 Action

| Action ID | 默认键位 | 行为 |
|---|---|---|
| `DarculaVeil.Toggle` | `Ctrl+Alt+Shift+V` | 开关主题化滤镜，页面不重新加载 |
| `DarculaVeil.BossKey` | **未绑定**，自行在 `Settings → Keymap` 挂 | 可见时：暂停所有 `<video>` / `<audio>` 后收起 tool window；已隐藏时：重新唤出。恢复不自动续播 |

老板键的媒体暂停只覆盖**主文档**里的媒体元素。跨域 iframe 内部（典型如第三方播放器嵌入）受同源策略限制，`querySelectorAll` 够不到，声音不会停。要覆盖这种情况得走 §2.3 的 OSR 层，或者对每个 frame 单独注入——P2 再处理。

---

## 6. 构建与运行

### 6.1 工具链

| 组件 | 版本 | 说明 |
|---|---|---|
| 目标平台 | 本地 Rider `RD-262.8665.400`（2026.2） | 走 `local()`，不从网络拉 IDE |
| JDK | 随 Rider 分发的 JBR 25 | 平台 2026.2 要求 Java 25，低于此 `verifyPluginProjectConfiguration` 会报 sourceCompatibility 过低 |
| Gradle | 9.7.1 | 8.x 不支持 Java 25 |
| Kotlin | 2.4.20 | |
| IntelliJ Platform Gradle Plugin | 2.19.0 | |

平台路径与版本号全部收在 `gradle.properties`，换机器只改 `platformLocalPath` 和 `pluginSinceBuild`。

### 6.2 命令

```bash
./gradlew buildPlugin        # 产出 build/distributions/darcula-veil-0.1.0.zip
./gradlew runIde             # 起一个带本插件的 Rider 沙箱实例
./gradlew verifyPluginProjectConfiguration verifyPluginStructure
```

本机已把 `JAVA_HOME` 设为用户级环境变量，指向 Rider 自带的 JBR：

```
JAVA_HOME = C:\Program Files\JetBrains\JetBrains Rider 2024.3.6\jbr
```

两个需要留意的点：

- **JBR 是纯 runtime，不带 `javac` / `jar`。** 当前工程没有 `.java` 源文件（`compileJava` 为 NO-SOURCE），所以不受影响；将来若加 Java 源码，或有别的工具靠 `JAVA_HOME` 找编译器，需要换一个完整 JDK 25。
- **目录名不等于版本号。** 安装目录叫 `JetBrains Rider 2024.3.6`，但 `build.txt` 是 `RD-262.8665.400`、`rider64.exe` 的 ProductVersion 是 `262.8665.400.0-RD`，实际是 2026.2。原因是 JetBrains 独立安装包走原地升级，目录名保留首次安装时的版本。**因此这个路径是稳定的**，日常升级不会失效；只有卸载重装或迁移到 Toolbox 管理时，才需要同步更新 `JAVA_HOME` 和 `platformLocalPath`。

### 6.3 JCEF 的依赖声明

平台 262 已经把 JCEF 从核心 lib 拆成 bundled plugin，类分布在两个 content module 里：

| 包 | 所在 jar |
|---|---|
| `org.cef.*` | `plugins/jcef-plugin/lib/modules/intellij.libraries.jcef.jar` |
| `com.intellij.ui.jcef.*` | `plugins/jcef-plugin/lib/modules/intellij.platform.ui.jcef.jar` |

因此 `build.gradle.kts` 里必须显式声明，只写 `local()` 会得到 `Unresolved reference 'jcef'`：

```kotlin
bundledPlugin("com.intellij.modules.jcef")
bundledModule("intellij.libraries.jcef")
bundledModule("intellij.platform.ui.jcef")
```

同时 `plugin.xml` 需要 `<depends>com.intellij.modules.jcef</depends>`，否则在不带 JCEF 的发行版上插件仍会加载并在运行时炸。

### 6.4 路线 B 的额外依赖

JNA（`net.java.dev.jna:jna-platform`），P4 阶段才引入，当前未加。

---

## 7. 参考

- [JCEF in IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/jcef.html)
- [Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [Windows Graphics Capture API](https://learn.microsoft.com/en-us/windows/uwp/audio-video-camera/screen-capture)
- [SetParent 与跨进程窗口层级](https://devblogs.microsoft.com/oldnewthing/20130412-00/?p=4683)

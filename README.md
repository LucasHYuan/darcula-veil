# Darcula Veil

> **状态：P1-P5 全部实现并构建通过。P1/P2 已实测并迭代过多轮，P4/P5 仅部分实测。**
> 第 5 节的状态表是各阶段的准确进度，标注"未运行验证"的部分只过了编译。
> 档位三（字符网格）与 §2.3 的 OSR 层仍是设计稿，未落地。

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
- **弹窗必须拦截。** 默认情况下 `target="_blank"` 和 `window.open()` 会让 CEF 尝试开一个新的 browser，在 tool window 里表现为弹出一个无法正常显示的窗口。修法是 `CefLifeSpanHandler.onBeforePopup` 返回 `true` 取消弹窗，并把 `targetUrl` 用 `loadURL` 加载回当前 browser。回调在 CEF 线程上，`loadUrl` 要 `invokeLater` 回 EDT。
- **只挂 `onLoadEnd` 会闪一下原始页面。** `onLoadEnd` 在文档加载完成后才触发，此时页面已经按原样绘制过一帧，视觉上就是"先正常渲染再变暗"。必须同时挂 `onLoadStart`：该回调在导航提交、文档开始解析时触发，此时 `document.documentElement` 已存在但内容尚未绘制，注入的样式能赶在首帧之前生效。`onLoadEnd` 保留作为兜底（SPA 路由、延迟注入的样式表）。
- **注入的元素会被页面弄丢。** SPA 替换 `body`、或页面自己清理 DOM，都会带走注入的 `<style>` 和承载 SVG filter 的 div。丢了 SVG 尤其危险：CSS `filter: url(#id)` 指向不存在的 filter 时，Chromium 会**直接不渲染该元素**，整页消失。因此注入脚本额外装了一个 `MutationObserver`，发现元素丢失就重新注入，并用 200ms 的调度标志节流，避免在高频 DOM 变动的页面上空转。

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

**档位二：色板量化（SVG feComponentTransfer）**

档位一是**连续变换**——页面原有多少种颜色，输出还是多少种，只是整体挪了位置。所以它产出的是"一个偏暗的网页"，而不是"一个 Darcula 风格的界面"。真正让内容融进 IDE 的是**离散量化**：不管原页面有几万种颜色，最终只允许出现 N 种，且这 N 种全部来自当前主题。

CSS filter 函数里没有量化能力，但 SVG filter 有，并且可以通过 `filter: url(#id)` 作用在 HTML 元素上：

```xml
<svg xmlns="http://www.w3.org/2000/svg" width="0" height="0">
  <filter id="darcula-veil-quantize" color-interpolation-filters="sRGB">
    <feColorMatrix type="saturate" values="0.00"/>
    <feComponentTransfer>
      <feFuncR type="discrete" tableValues="0.1176 0.2392 0.6588 0.8039"/>
      <feFuncG type="discrete" tableValues="0.1216 0.2627 0.5529 0.8471"/>
      <feFuncB type="discrete" tableValues="0.1333 0.3020 0.4510 0.7216"/>
    </feComponentTransfer>
  </filter>
</svg>
```

`type="discrete"` 把 0~1 的输入切成 N 段，每段输出 `tableValues` 里对应的固定值。三个通道各一张表，本质就是一个 **gradient map**：亮度 → 色阶索引 → 具体 RGB。表里的数值由 `EditorColorsScheme` 实时生成，换主题自动跟着变。

相比 canvas 方案的优势：纯 CSS/SVG，GPU 合成，**不读取像素数据，因此完全不受跨域 taint 限制**，对任意页面通用。

**两个必须知道的坑：**

- **`color-interpolation-filters="sRGB"` 不能省。** SVG filter 默认在 linearRGB 空间运算，不显式指定的话整体会发灰发错，这是最容易翻车的一行。
- **祖先元素上的 SVG filter 无法被后代撤销。** 档位一的 `invert(1)` 是自逆的，所以能对图片"反选回来"；量化没有逆运算。因此量化模式下图片和视频**一定**会被一起量化，无法豁免。设置面板里那个媒体还原开关只对档位一生效。

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

### 2.4 翻页：为什么必须注入到页面里

**JCEF 拿到焦点时会先吃掉按键，IDE 的 action 系统看不到。** 所以翻页快捷键如果只注册成 IDE action，在实际阅读时（焦点必然在网页上）大概率不触发。

因此按键处理注入到页面内：`document.addEventListener("keydown", ..., true)` 捕获阶段拦截，命中就调 `window.__darculaVeilTurn(direction)`。IDE action 同时保留，用于焦点不在网页时，两者调用同一个函数。

监听器有三道护栏：

- `!event.isTrusted` 直接返回。否则"发送方向键"这个翻页模式派发的合成事件会被自己的监听器接住，无限递归。
- `input` / `textarea` / `isContentEditable` 里不拦截，否则输入框里按方向键会翻页。
- 带 Ctrl / Alt / Meta 时不拦截，把组合键让给页面和 IDE。

翻页动作本身是可切换策略，与色板、呈现模式同一套模式：

| id | 行为 |
|---|---|
| `scroll` | 按视口高度百分比滚动，`scrollBy` 无效时直接写 `scrollTop` 兜底 |
| `arrow-keys` | 向 `document` / `body` / `activeElement` 派发方向键事件，交给页面自己的翻页逻辑 |
| `click-selector` | 按配置的 CSS 选择器找到翻页按钮并 `click()` |

### 2.5 扫码登录与滤镜的冲突

二维码是纯黑白图形，而色板量化会把它**整体反色**（白底映射到色板暗端、黑块映射到亮端），多数扫码器读不了反色二维码；边缘渐晕还会压暗静默区。

量化没有逆运算，也无法对单个元素豁免（祖先滤镜不可被后代撤销），所以没有"只放过二维码"的做法。扫码时按 `Ctrl+Alt+Shift+V` 关掉滤镜即可——该开关只改注入的样式，不重新加载页面，登录流程不受影响。

### 2.6 媒体播放的两层限制

**DRM：JCEF 不带 Widevine CDM。** 本机核对过 `plugins/jcef-plugin/jcef/` 下没有任何 widevine 组件。受 DRM 保护的流媒体在这条路线上放不了，这是发行版层面的限制，不是配置问题。本项目不尝试绕过，相关内容直接判为不支持。

**专有编解码器：H.264 / AAC / MP3 取决于 JCEF 的编译选项。** Chromium 可以带也可以不带专有编解码器，这在编译期就定了，任何命令行开关都改不了。网络上绝大多数 MP4 视频是 H.264 + AAC，所以一旦缺失，表现就是"H5 视频完全播不了"，而 WebM（VP8/VP9/Opus）仍然正常。

这件事不能靠猜，工具窗标题栏的 **Media Support Report** 按钮会用 `loadHTML` 打开一个本地探测页，现场跑 `canPlayType` 和 `MediaSource.isTypeSupported`。本机实测结果：

```
JCEF_VERSION_DETAILED=144.0.15-g72717cf-chromium-144.0.7559.172-api-1.21-262-b37
userAgent: ... Chrome/144.0.0.0 ...

H.264 baseline / high   canPlayType=no        MediaSource=false
AAC-LC                  canPlayType=no        MediaSource=false
HLS                     canPlayType=no        MediaSource=false
MP3                     canPlayType=probably  MediaSource=true
VP8 / VP9 / AV1         canPlayType=probably  MediaSource=true
Opus (ogg)              canPlayType=probably  MediaSource=false
```

**结论：这个 JCEF 构建不含专有编解码器。** 于是绝大多数站点的 H5 视频（H.264 + AAC，或 MSE 喂 fMP4）一律无法播放，WebM 源正常。

MP3 能放是因为其专利已于 2017 年到期，Chromium 的自由构建随后默认启用；H.264 / AAC 仍在授权期内，所以依旧缺席。

**OSR 解决不了这个问题。** §2.3 的离屏渲染仍然跑在同一个 CEF 二进制上，解码器缺失与渲染路径无关。真正可行的只有两条：改用 WebM 源，或走路线 B 直接嵌一个自带完整编解码器的外部浏览器 / 播放器窗口，由它负责解码，overlay 负责主题化。

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
| **P2** | 主题化档位二：SVG 色板量化 + 双色板来源 + 设置面板 | 已实现，构建通过，未运行验证 |
| **P3** | 字符网格渲染；同源内容先行，跨域场景评估 OSR | 设计中 |
| **P4** | 路线 B：reparent + 几何同步 + 焦点交接 + peer 生命周期 | 已实现，构建通过，未运行验证 |
| **P5** | overlay 合成层：layered 穿透子窗 + UpdateLayeredWindow 逐像素 tint/渐晕/扫描线 | 已实现，构建通过，未运行验证 |

### P4：原生窗口嵌入

独立的工具窗 `Darcula Veil Window`，标题栏三个按钮：Pick Window / Resync Geometry / Detach。

`Pick Window` 通过 `EnumWindows` 列出所有可见、无父窗口、标题非空的顶层窗口。**同进程的窗口被强制排除**——把 IDE 自己的窗口 reparent 进自己的 tool window 会立刻死锁。

嵌入流程：

```
GetWindowLong 保存 style / exStyle / parent
  → style 去掉 WS_POPUP|WS_CAPTION|WS_THICKFRAME|WS_SYSMENU|WS_MIN/MAXIMIZEBOX，加 WS_CHILD
  → exStyle 去掉 WS_EX_APPWINDOW，加 WS_EX_TOOLWINDOW
  → SetParent(target, canvasHwnd)
  → SetWindowPos(SWP_FRAMECHANGED)     // style 改完必须刷 frame，否则边框残留
```

几何同步不做 DPI 换算，而是直接对宿主 HWND 调 `GetClientRect` 取**设备像素**再 `SetWindowPos`。这样绕开了 AWT 逻辑像素与 HiDPI 缩放的换算问题，跨屏拖动也自然正确。

焦点交接在 Canvas 的 `mousePressed` 里做：`AttachThreadInput` 建立关联 → `SetFocus` → 立刻解除关联。长期 attach 会让两个进程的输入状态互相污染。

### 子窗口会随宿主 peer 一起被销毁

这是路线 B 最危险的一点，不是理论风险：**Win32 销毁父窗口时会连带销毁所有子窗口。** 工具窗停靠↔浮动切换、IDE 全屏切换，都会让 AWT 销毁并重建 Canvas 的 peer。如果此时目标窗口还挂在旧 HWND 下，它会被一起干掉——对游戏来说就是窗口没了、进程可能直接崩。

因此宿主必须是 `Canvas` 的子类，在 peer 销毁**之前**解绑：

```kotlin
private inner class HostCanvas : Canvas() {
    override fun addNotify() {
        super.addNotify()
        if (target != null && embedder == null) { attachToCanvas() }
    }

    override fun removeNotify() {
        releaseEmbedder()      // 必须在 super 之前，super 会销毁 peer
        super.removeNotify()
    }
}
```

`HierarchyListener` 的 `DISPLAYABILITY_CHANGED` 在这里**不够用**——它在 peer 已经销毁之后才触发，那时目标窗口已经没了。

同样的道理：**IDE 崩溃或被强杀时来不及解绑，嵌入的窗口会跟着消失。** 这个没有办法兜底，是路线 B 的固有代价。

### P5：overlay 合成层

在目标窗口之上叠一个自建的子窗口作为主题化合成层：

```
Canvas HWND
  ├─ 目标窗口（reparent 进来，WS_CHILD）
  └─ overlay（WS_CHILD | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE）
```

`WS_EX_TRANSPARENT` 让命中测试穿透，鼠标事件全部落到下层目标窗口，输入链完全不受影响。

**不需要注册窗口类。** 这是当初被高估的那部分工作量：用 `UpdateLayeredWindow` 时窗口内容完全由我们提供的 ARGB 位图决定，根本不走 `WM_PAINT`，所以可以直接拿系统自带的 `"STATIC"` 类建窗口，省掉 `RegisterClassEx` 和 WNDPROC 回调。

合成链路：

```
VeilOverlayRenderer 生成预乘 ARGB IntArray
  → CreateDIBSection 拿到 top-down 32 位 DIB 的裸指针（biHeight 传负值）
  → Pointer.write() 直接灌像素
  → UpdateLayeredWindow(..., AC_SRC_ALPHA, ULW_ALPHA)
```

**必须是预乘 alpha**，`AC_SRC_ALPHA` 要求如此；直接写非预乘值会得到发亮的错误结果。位图按 `signature` 缓存，只有尺寸或参数变化才重新渲染，几何同步时不会每秒重算百万像素。

Z 序上 overlay 必须始终在目标之上，目标窗口自己调 `SetWindowPos` 可能打乱顺序，因此每次几何同步都重新置顶一次。

可调项：tint 强度（混向 IDE 面板色）、渐晕强度、扫描线间距与强度。

**这仍然是叠加型效果。** 拿不到下层像素就做不了依赖源像素的运算——对比度调整、色彩重映射、网页那套色板量化，在这条路线上都不可能。想要真正的 shader 后处理只能换 Windows Graphics Capture，代价是输入链彻底断掉（见 §3.3）。

另外保留了一个独立开关：给目标窗口本身加 `WS_EX_LAYERED` 调整体透明度。默认关闭，因为它会强制窗口重定向，可能拖慢或破坏 D3D 渲染。

### 实测暴露的四个缺陷

| 症状 | 根因 | 修法 |
|---|---|---|
| 工具窗只能拉宽拉不窄 | `java.awt.Canvas.getMinimumSize()` 默认回落到 peer 的当前尺寸。窗口一旦变大，最小尺寸跟着变大，IDE 的分隔条就再也退不回去，形成棘轮 | `HostCanvas` 覆写 `getMinimumSize()` / `getPreferredSize()` 恒返回 `1x1`，面板同样覆写 |
| Detach 之后找不到窗口，也没法重新嵌入 | 还原时只恢复了 style 和 parent，**没有恢复位置尺寸**，窗口带着子窗口坐标变回顶层，通常落在屏幕左上角；而且 target 被清空，没有任何入口重来 | 嵌入前 `GetWindowRect` 存原始矩形，detach 时一并还原；新增 `lastTarget` 与 `Re-embed Last Window` 动作 |
| 工具窗收起后窗口弹出去、再打开回不来 | 收起会销毁 Canvas peer，`removeNotify` 正确解绑；但重新展开时 `addNotify` 里立刻取 HWND，peer 尚未完全就绪，取不到就**静默放弃且无人重试** | 改为 `invokeLater` 延迟挂载，并让 watchdog 定时器在 `target != null && embedder == null` 时持续重试，`componentShown` 也触发一次 |
| 网页偶发整页消失 | 注入顺序是先写 `filter: url(#id)` 再插 SVG。两者之间存在窗口期，且 SVG 被页面清掉后同样会命中——引用不存在的 filter 时 Chromium 直接不渲染该元素 | 调换顺序（先 SVG 后 style），并在写 CSS 前校验 filter 确实在文档里，不在就写空 CSS。失败模式从"整页不可见"降级为"没有主题化" |

### 嵌入后窗口尺寸为 0x0

`attach()` 末尾会 `syncGeometry()`，它用 `GetClientRect(host)` 取宿主尺寸。但把挂载改成 `invokeLater` 延迟执行之后出现了时序空档：**AWT 组件已经有尺寸了，底层 peer 的 HWND 还没被 resize**，`GetClientRect` 返回空矩形，于是目标窗口被 `SetWindowPos` 成 0x0。

之后 AWT 组件尺寸不再变化，`componentResized` 不会触发，没有任何人纠正这个值——窗口就永久停在 0x0，表现为"选完窗口什么都没有"。用 `canvas.width` 做守卫无效，那是 AWT 逻辑尺寸，和原生 HWND 的实际尺寸是两回事。

两处修：

- `syncGeometry()` 拿到 `0` 宽高直接跳过，不再把窗口设成 0x0；并且先比对当前矩形，尺寸没变就不调 `SetWindowPos`
- watchdog 每秒除了检查存活，**也重新对一次几何**。1 Hz 的 `SetWindowPos` 开销可以忽略，换来的是任何一次错过的 resize 都能自愈

诊断这类问题不能只看 `IsWindowVisible`——它对 0 尺寸窗口照样返回 `true`。必须沿 `GetParent` 链逐级打印 `GetWindowRect`，`tools/probe_window_chain.ps1` 就是干这个的。

### 恢复时的闪烁与"弹出去"

老板键恢复时会看到一下闪烁，原因是**显示早于几何就位**：`attach()` 之后窗口还保持着 detach 期间被还原的原始尺寸（比如 800x600），要等 `syncGeometry()` 才会缩到面板大小。如果那一刻 peer 尚未 resize，`syncGeometry()` 会跳过，窗口就以错误尺寸显示到下一次 watchdog tick 为止——视觉上就是闪一下再跳到正确大小。

修法是把"显示"从 `attach()` 里拆出来，改成**几何确认就位之后才显示**：

```kotlin
private fun revealWhenReady() {
    val current = embedder ?: return
    if (!current.syncGeometry()) { return }          // 几何没就位就不显示
    if (concealed || !awaitingGeometry) { return }
    current.setVisible(true)
    awaitingGeometry = false
}
```

`syncGeometry()` 因此改为返回 `Boolean`。`attach` / `componentResized` / watchdog / 手动 Resync 四个入口全部走这一个函数，谁先满足条件谁负责显示。

另外，detach 时若不需要显示（老板键隐藏路径），**还原 style 必须把 `WS_VISIBLE` 掩掉**：

```kotlin
private fun restoredStyle(showAfterDetach: Boolean): Int {
    if (showAfterDetach) { return originalStyle }
    return originalStyle and VeilUser32.WS_VISIBLE.inv()
}
```

原始 style 里通常带着 `WS_VISIBLE`，直接写回去会让样式位与实际可见性不一致，紧接着的 `SetWindowPos(SWP_FRAMECHANGED)` 有机会把它真的显示出来——表现就是按下老板键后目标窗口在桌面上闪出来。

### 原生窗口为什么没有主题化

只有整窗 alpha（默认关闭），没有色板量化。这不是未实现，是 §3.3 那条结构性矛盾的直接后果：reparent 保住了输入，代价是像素完全由对方进程的 D3D / GDI 管线产出，我们没有任何插入点。想要真正的 shader 后处理必须换 Windows Graphics Capture，而那会让输入链彻底断掉。

### 跨进程调用的两条硬规矩

嵌入外部窗口意味着在 EDT 上对**别的进程的窗口**发起 Win32 调用，有两处必须防死。

**一、`AttachThreadInput` 必须 try/finally 配对。** 它把两个进程的输入队列绑在一起，绑上之后如果解绑那行没能执行，绑定会**永久泄漏**——症状是 JVM 完好、后台线程照常打日志、`PerformanceWatcher` 也不报冻结，但 IDE 窗口再也不响应输入。这种卡死发生在 Win32 输入层，不在 JVM 层，所以任何 Java 侧的诊断都看不到它。

```kotlin
if (!user32.AttachThreadInput(currentThreadId, targetThreadId, true)) {
    return
}

try {
    user32.SetFocus(target)
} finally {
    user32.AttachThreadInput(currentThreadId, targetThreadId, false)
}
```

设置里另留了一个开关可以彻底关掉焦点转移，用于排除这条路径。

**二、对外部窗口的 `SetWindowPos` 一律加 `SWP_ASYNCWINDOWPOS`。** 默认行为是**同步**向目标窗口所属线程发送 `WM_WINDOWPOSCHANGING` / `WM_NCCALCSIZE`，若那个线程正忙或已卡住，调用方会一起阻塞。watchdog 每秒都会对目标做一次几何同步，一旦目标是个偶尔卡顿的游戏，IDE 就会跟着一起卡。加上这个标志后请求改为投递，调用立即返回。

overlay 的像素渲染也只在 watchdog 里做，不挂在 `componentResized` 上——否则拖动工具窗边界时会逐帧重算上百万像素，全部压在 EDT 上。

### 使用限制

- **目标必须是窗口化或无边框窗口化。** 独占全屏的 D3D 窗口 reparent 之后行为未定义，先在游戏里切到 Borderless。
- **Anti-cheat 风险自负。** `SetParent` 到外部进程窗口、跨进程 `AttachThreadInput`，都可能被 anti-cheat 判定为注入特征。本项目不提供任何规避手段，也不建议对带 EAC / BattlEye / VAC 的游戏使用。
- Detach 会把 style、exStyle、parent 全部还原回嵌入前的值。

### P1 的两个 Action

| Action ID | 默认键位 | 行为 |
|---|---|---|
| `DarculaVeil.Toggle` | `Ctrl+Alt+Shift+V` | 开关主题化滤镜，页面不重新加载 |
| `DarculaVeil.BossKey` | **未绑定**，自行在 `Settings → Keymap` 挂 | 同时作用于两个工具窗，见下 |

工具窗标题栏还有四个只在面板内生效的按钮：后退 / 前进 / 刷新 / 改地址。后退前进按 `CefBrowser.canGoBack()` / `canGoForward()` 自动置灰。

### 老板键为什么不能只是 hide 工具窗

一个键管两个工具窗，但两边的隐藏含义完全不同。

网页侧直接 `hide()` 即可，附带暂停主文档里的 `<video>` / `<audio>`。

**原生侧直接 `hide()` 会适得其反。** 收起工具窗会销毁 Canvas 的 peer，触发 §3 的保命解绑逻辑，嵌入的窗口于是 `SetParent(NULL)` 变回顶层并 `SW_SHOW` —— 按下老板键的结果是目标窗口**弹回桌面变得更显眼**。

所以原生侧的顺序必须是：先 `ShowWindow(target, SW_HIDE)` 并置 `concealed` 标志，再收工具窗。`concealed` 会贯穿整条生命周期：

- `releaseEmbedder()` 调 `detach(showAfterDetach = !concealed)`，解绑时不强制显示
- `attachToCanvas()` 结束时调 `setVisible(!concealed)`，重新挂载后也保持隐藏

恢复时先 `activate()` 工具窗，等重新挂载完成后再 `reveal()`。

哪些工具窗在按下时是可见的会记进 `project` 的 `UserData`，恢复时只还原这一组，不会把你本来就没开的那个也弹出来。

老板键的媒体暂停只覆盖**主文档**里的媒体元素。跨域 iframe 内部（典型如第三方播放器嵌入）受同源策略限制，`querySelectorAll` 够不到，声音不会停。要覆盖这种情况得走 §2.3 的 OSR 层，或者对每个 frame 单独注入——尚未处理。

### P2 的设置面板

`Settings → Tools → Darcula Veil`。改动在 `apply()` 里写回设置后，通过 `VeilStateListener.TOPIC` 广播，所有打开的面板立即重绘——**不需要重启 IDE**，因为 `refreshVeil()` 每次都是现读设置。

| 设置项 | 默认 | 说明 |
|---|---|---|
| Style mode | Palette quantization | 在量化与连续滤镜两种策略间切换 |
| Palette source | Editor monochrome | 见下方色板来源表 |
| Custom colors | 空 | `custom` 来源专用，逗号分隔的 hex 列表，不足两个有效色时回落到 monochrome |
| Palette steps | 6 | 最终允许出现的颜色数量，2~16 |
| Range floor / ceiling | 0 / 70 | 只取色带的 `[floor, ceiling]` 区间做采样。**天花板是控制"最亮能有多亮"的旋钮**，默认 70 是为了让页面里不出现接近前景色的亮块 |
| Pre-quantize saturation | 0 | 量化前的 `feColorMatrix saturate` 值。0 = 完全去色纯按亮度映射；调高则保留部分原始色相 |
| Reverse mapping | 开 | 见下方"映射方向" |
| Invert / Hue rotate / Saturate / Brightness / Contrast | 92 / 180 / 85 / 95 / 100 | 仅作用于连续滤镜模式 |
| Restore original colors on images and video | 开 | 仅作用于连续滤镜模式，量化模式下无效（见 §2.2） |

面板里有一条实时色板预览条，最左边那格就是"白色页面背景会变成什么颜色"。

**色板来源：**

| id | 取色 | 特征 |
|---|---|---|
| `mono` | 编辑器 `defaultBackground` → `defaultForeground` | 单色调，最不突兀，默认 |
| `ide-ui` | `UIUtil.getPanelBackground()` / `JBColor.border()` / `UIUtil.getLabelForeground()` | 对齐工具窗边框而非编辑器，页面边界几乎完全消失，代价是对比度低 |
| `syntax` | 额外取 comment / keyword / string / number / function / class 前景色 | 保留彩色层次，但显眼 |
| `custom` | 用户填的 hex 列表 | 完全手动控制 |

### 边缘渐晕：为什么量化还不够

量化只改颜色，不改形状。页面被压成暗色之后，它**仍然是一个边缘齐整的矩形**贴在工具窗里，轮廓一眼可辨。真正让内容"看不出来"的是渐晕——四周淡出融进面板底色，矩形边界随之消失。这一点路线 B 的 overlay 天然具备，路线 A 最初没有。

麻烦在于渐晕不能被量化：它用的是面板底色（暗色），而反向映射会把暗输入映射到色板**亮端**，渐晕会变成一圈发亮的光晕。祖先元素上的 filter 又无法被后代豁免，所以渐晕必须待在滤镜作用域之外。

解法是把滤镜从 `html` 下移到 `body`，渐晕挂在 `html::after` 上——它是 `html` 的伪元素，不是 `body` 的后代，因此不受滤镜影响：

```css
html { background-color: #1e1f22; }          /* 见下 */
body { filter: url(#darcula-veil-quantize); }
html::after {
  content: ""; position: fixed; left: 0; top: 0; right: 0; bottom: 0;
  pointer-events: none; z-index: 2147483647; opacity: 0.45;
  background: radial-gradient(ellipse at center, rgba(0,0,0,0) 35%, #1e1f22 100%);
}
```

`html` 的显式背景色不能省。滤镜移到 `body` 之后，视口画布的背景仍由背景传播机制决定，**不在 `body` 的滤镜作用域内**——不显式指定的话，页面原本的白色背景会原封不动地露出来。这里填的是色板中白色所映射到的那个颜色，即 `palette.last()`。

渐晕强度设为 0 时回退到直接滤镜 `html` 的旧结构，作为出问题时的退路。

### 映射方向：必须反向

`feFuncX type="discrete"` 的语义是「输入 0 取表首，输入 1 取表尾」。色带按亮度升序排列时，**白色页面背景（亮度≈1）会映射到色带末端，也就是最亮的前景色**——结果是整页变成浅灰，比原页面更扎眼，与目标完全相反。

所以量化必须把色板**反向**后再写进 `tableValues`：亮度高的页面背景 → 色板最暗端（编辑器背景色），亮度低的正文文字 → 色板最亮端。`paletteReversed` 因此默认为开。

`tools/preview_palette.py` 用 Python 复刻了同一套数学（`saturate 0` 去色 + `discrete` 取表），可以在不启动 IDE 的情况下渲染对照图，改算法时先用它验证比反复 `runIde` 快得多。

### 策略与色板的扩展点

模式分支没有写成 `if` / `when`，而是两组接口：

```
VeilStyleStrategy          VeilPaletteProvider
  ├─ PaletteStyleStrategy    ├─ MonochromePaletteProvider
  └─ FilterStyleStrategy     └─ SyntaxPaletteProvider
```

新增一种呈现模式或一种取色方案，只需实现接口并登记进 `VeilStyleStrategies.ALL` / `VeilPaletteProviders.ALL`，设置面板的下拉框由 `displayNames()` 自动生成，无需改动 UI 代码。

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

### 6.3 版本与热重载

版本号写在 `gradle.properties` 的 `pluginVersion`，`patchPluginXml` 会把它写进 `plugin.xml`，产物文件名也跟着变。

本插件**支持动态加载，更新不需要重启 IDE**。依据是用到的两个扩展点在平台里都声明为 `dynamic="true"`：

```xml
<extensionPoint name="toolWindow" beanClass="...ToolWindowEP" dynamic="true"/>
<extensionPoint name="applicationConfigurable" dynamic="true" beanClass="...ConfigurableEP"/>
```

工程里没有 `<application-components>`，actions 也是动态注册，因此整个插件是 unload-safe 的。

- **沙箱迭代**：`runIde` 的 `autoReload` 已显式打开。保持 `runIde` 运行，另开一个终端执行 `gradlew buildPlugin`，IDE 会自动卸载旧版本装上新的，不用重启也不用重新安装。
- **日常 IDE**：`Settings → Plugins → Install Plugin from Disk` 装新版 zip，IDE 会直接完成替换。如果它仍然要求重启，去 `idea.log` 搜 `not unload-safe`，日志会说明是哪个原因导致无法动态卸载。

### 6.4 JCEF 的依赖声明

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

### 6.5 路线 B 的额外依赖

JNA（`net.java.dev.jna:jna-platform`），P4 阶段才引入，当前未加。

---

## 7. 参考

- [JCEF in IntelliJ Platform](https://plugins.jetbrains.com/docs/intellij/jcef.html)
- [Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)
- [Windows Graphics Capture API](https://learn.microsoft.com/en-us/windows/uwp/audio-video-camera/screen-capture)
- [SetParent 与跨进程窗口层级](https://devblogs.microsoft.com/oldnewthing/20130412-00/?p=4683)

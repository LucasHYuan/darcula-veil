package com.lucashyuan.darculaveil

import com.intellij.openapi.util.text.StringUtil
import com.lucashyuan.darculaveil.pageturn.VeilPageTurnStrategies

object VeilPageTurnScript {

    private const val TURN_PROPERTY = "__darculaVeilTurn"
    private const val LISTENER_PROPERTY = "__darculaVeilTurnKeys"
    private const val SCROLLER_PROPERTY = "__darculaVeilScroller"
    private const val WHEEL_PROPERTY = "__darculaVeilTurnWheel"
    private const val DIAGNOSTIC_ELEMENT_ID = "darcula-veil-diagnostic"

    fun buildInstallScript(settings: VeilSettings.State): String {
        val body = VeilPageTurnStrategies.byId(settings.pageTurnStrategyId).buildTurnBody(settings)
        val forwardKey = toJsLiteral(settings.pageTurnForwardKey)
        val backwardKey = toJsLiteral(settings.pageTurnBackwardKey)

        return """
            (function() {
                window.$SCROLLER_PROPERTY = function() {
                    var fallback = document.scrollingElement || document.documentElement;
                    var best = fallback;
                    var bestGap = fallback.scrollHeight - fallback.clientHeight;
                    var candidates = document.querySelectorAll("div,main,section,article");

                    for (var i = 0; i < candidates.length; i++) {
                        var node = candidates[i];
                        if (node.clientHeight < window.innerHeight * 0.4) {
                            continue;
                        }
                        var overflow = window.getComputedStyle(node).overflowY;
                        if (overflow !== "auto" && overflow !== "scroll") {
                            continue;
                        }
                        var gap = node.scrollHeight - node.clientHeight;
                        if (gap > bestGap) {
                            best = node;
                            bestGap = gap;
                        }
                    }

                    return best;
                };

                window.$TURN_PROPERTY = function(direction) {
                    $body
                };

                if (window.$WHEEL_PROPERTY) {
                    document.removeEventListener("wheel", window.$WHEEL_PROPERTY, true);
                    window.$WHEEL_PROPERTY = null;
                }

                var wheelBindings = ${VeilKeymapBridge.buildWheelBindingsLiteral()};

                if (wheelBindings.length > 0) {
                    window.$WHEEL_PROPERTY = function(event) {
                        if (!event.isTrusted) {
                            return;
                        }
                        var up = event.deltaY < 0;
                        for (var i = 0; i < wheelBindings.length; i++) {
                            var binding = wheelBindings[i];
                            if (binding.up !== up) {
                                continue;
                            }
                            if (binding.shift !== event.shiftKey || binding.ctrl !== event.ctrlKey || binding.alt !== event.altKey) {
                                continue;
                            }
                            window.$TURN_PROPERTY(binding.direction);
                            event.preventDefault();
                            event.stopPropagation();
                            return;
                        }
                    };

                    document.addEventListener("wheel", window.$WHEEL_PROPERTY, { capture: true, passive: false });
                }

                if (window.$LISTENER_PROPERTY) {
                    document.removeEventListener("keydown", window.$LISTENER_PROPERTY, true);
                    window.$LISTENER_PROPERTY = null;
                }

                if (!${settings.pageTurnKeysEnabled}) {
                    return;
                }

                window.$LISTENER_PROPERTY = function(event) {
                    if (!event.isTrusted || event.defaultPrevented) {
                        return;
                    }
                    if (event.ctrlKey || event.altKey || event.metaKey) {
                        return;
                    }
                    var node = event.target;
                    var tag = node && node.tagName ? node.tagName.toLowerCase() : "";
                    if (tag === "input" || tag === "textarea" || (node && node.isContentEditable)) {
                        return;
                    }
                    if (event.key === $forwardKey) {
                        window.$TURN_PROPERTY(1);
                        event.preventDefault();
                        return;
                    }
                    if (event.key === $backwardKey) {
                        window.$TURN_PROPERTY(-1);
                        event.preventDefault();
                    }
                };

                document.addEventListener("keydown", window.$LISTENER_PROPERTY, true);

            })();
        """.trimIndent()
    }

    fun buildTurnCall(direction: Int): String {
        return "if (window.$TURN_PROPERTY) { window.$TURN_PROPERTY($direction); }"
    }

    fun buildDiagnosticScript(): String {
        return """
            (function() {
                var scroller = window.$SCROLLER_PROPERTY ? window.$SCROLLER_PROPERTY() : null;
                var describe = function(node) {
                    if (!node) {
                        return "none";
                    }
                    var name = node.tagName ? node.tagName.toLowerCase() : "?";
                    if (node.id) {
                        name = name + "#" + node.id;
                    }
                    if (node.className && typeof node.className === "string") {
                        name = name + "." + node.className.trim().split(/\s+/).slice(0, 2).join(".");
                    }
                    return name + "  scrollHeight=" + node.scrollHeight + " clientHeight=" + node.clientHeight;
                };

                var lines = [
                    "frame: " + (window.top === window ? "TOP" : "CHILD"),
                    "url: " + location.href,
                    "child frames: " + window.frames.length,
                    "turn fn installed: " + (typeof window.$TURN_PROPERTY === "function"),
                    "key listener installed: " + (typeof window.$LISTENER_PROPERTY === "function"),
                    "wheel listener installed: " + (typeof window.$WHEEL_PROPERTY === "function"),
                    "wheel bindings from keymap: " + ${VeilKeymapBridge.buildWheelBindingsLiteral()}.length,
                    "scroller: " + describe(scroller),
                    "document scroller: " + describe(document.scrollingElement || document.documentElement)
                ];

                var existing = document.getElementById("$DIAGNOSTIC_ELEMENT_ID");
                if (existing && existing.parentNode) {
                    existing.parentNode.removeChild(existing);
                }

                var box = document.createElement("div");
                box.id = "$DIAGNOSTIC_ELEMENT_ID";
                box.setAttribute("style", "position:fixed;left:8px;top:8px;z-index:2147483647;max-width:90vw;padding:10px 14px;background:#1e1f22;color:#bcbec4;font:12px/1.6 Consolas,monospace;white-space:pre;border:1px solid #4a4b4f;pointer-events:none;");
                box.textContent = lines.join("\n");
                (document.body || document.documentElement).appendChild(box);

                setTimeout(function() {
                    if (box.parentNode) {
                        box.parentNode.removeChild(box);
                    }
                }, 12000);
            })();
        """.trimIndent()
    }

    private fun toJsLiteral(value: String): String = "\"" + StringUtil.escapeStringCharacters(value) + "\""
}

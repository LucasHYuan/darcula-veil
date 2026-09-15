package com.lucashyuan.darculaveil

object VeilMediaScript {

    fun buildPauseScript(): String {
        return """
            (function() {
                var elements = document.querySelectorAll("video, audio");
                for (var i = 0; i < elements.length; i++) {
                    try {
                        elements[i].pause();
                    } catch (ignored) {
                    }
                }
            })();
        """.trimIndent()
    }
}

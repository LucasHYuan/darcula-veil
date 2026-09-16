package com.lucashyuan.darculaveil

object VeilMediaReport {

    private val PROBES = listOf(
        "video/mp4; codecs=\"avc1.42E01E\"" to "H.264 baseline (proprietary)",
        "video/mp4; codecs=\"avc1.640028\"" to "H.264 high (proprietary)",
        "audio/mp4; codecs=\"mp4a.40.2\"" to "AAC-LC (proprietary)",
        "audio/mpeg" to "MP3 (proprietary)",
        "video/webm; codecs=\"vp8\"" to "VP8 (open)",
        "video/webm; codecs=\"vp9\"" to "VP9 (open)",
        "video/webm; codecs=\"av01.0.05M.08\"" to "AV1 (open)",
        "audio/ogg; codecs=\"opus\"" to "Opus (open)",
        "application/vnd.apple.mpegurl" to "HLS (native)"
    )

    fun buildHtml(): String {
        val rows = PROBES.joinToString("\n") { probe ->
            """        { type: "${probe.first.replace("\"", "\\\"")}", label: "${probe.second}" },"""
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <title>Darcula Veil media support</title>
              <style>
                body { font-family: "JetBrains Mono", Consolas, monospace; font-size: 13px; padding: 20px; }
                h1 { font-size: 16px; margin: 0 0 4px 0; }
                p { margin: 4px 0 16px 0; }
                table { border-collapse: collapse; width: 100%; }
                th, td { text-align: left; padding: 6px 10px; border-bottom: 1px solid currentColor; }
                th { font-weight: bold; }
                .ok { font-weight: bold; }
                .no { opacity: 0.55; }
                pre { white-space: pre-wrap; word-break: break-all; margin-top: 18px; }
              </style>
            </head>
            <body>
              <h1>Media codec support in this JCEF build</h1>
              <p>canPlayType plus MediaSource.isTypeSupported, probed live.</p>
              <table id="report">
                <tr><th>Codec</th><th>canPlayType</th><th>MediaSource</th></tr>
              </table>
              <pre id="env"></pre>
              <script>
                var probes = [
$rows
                ];

                var video = document.createElement("video");
                var table = document.getElementById("report");
                var hasMediaSource = typeof window.MediaSource !== "undefined";

                for (var i = 0; i < probes.length; i++) {
                    var probe = probes[i];
                    var direct = video.canPlayType(probe.type);
                    var viaSource = hasMediaSource ? String(window.MediaSource.isTypeSupported(probe.type)) : "n/a";
                    var row = document.createElement("tr");
                    var cells = [probe.label + "  [" + probe.type + "]", direct === "" ? "no" : direct, viaSource];

                    for (var c = 0; c < cells.length; c++) {
                        var cell = document.createElement("td");
                        cell.textContent = cells[c];
                        if (c > 0) {
                            cell.className = (cells[c] === "no" || cells[c] === "false") ? "no" : "ok";
                        }
                        row.appendChild(cell);
                    }

                    table.appendChild(row);
                }

                document.getElementById("env").textContent =
                    "MediaSource: " + hasMediaSource + "\n" +
                    "mediaCapabilities: " + (typeof navigator.mediaCapabilities !== "undefined") + "\n" +
                    "userAgent: " + navigator.userAgent;
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}

(function () {
    "use strict";

    // index.html이 서빙되는 경로에서 뷰어 base path를 유도한다 (viewer-path가 커스텀되어도 동작하도록).
    var BASE_PATH = window.location.pathname.replace(/\/[^/]*$/, "");
    var API_TRACES = BASE_PATH + "/api/traces";

    var listEl = document.getElementById("trace-list");
    var detailEl = document.getElementById("trace-detail");
    var refreshBtn = document.getElementById("refresh-btn");
    var clearBtn = document.getElementById("clear-btn");
    var autoRefreshToggle = document.getElementById("auto-refresh-toggle");

    var selectedTraceId = null;
    var autoRefreshTimer = null;

    function fetchJson(url, options) {
        return fetch(url, options).then(function (res) {
            if (!res.ok) {
                throw new Error("HTTP " + res.status);
            }
            if (res.status === 204) {
                return null;
            }
            return res.json();
        });
    }

    function durationClass(ms) {
        if (ms >= 500) return "slow";
        if (ms >= 100) return "warn";
        return "";
    }

    function formatTime(iso) {
        try {
            var d = new Date(iso);
            return d.toLocaleTimeString();
        } catch (e) {
            return iso;
        }
    }

    // ---------- 목록 ----------

    function renderList(summaries) {
        listEl.innerHTML = "";
        summaries.forEach(function (s) {
            var li = document.createElement("li");
            li.dataset.traceId = s.traceId;
            if (s.traceId === selectedTraceId) {
                li.classList.add("selected");
            }
            var isError = s.status >= 400;
            li.innerHTML =
                '<div class="row1">' +
                '<span><span class="method-badge">' + escapeHtml(s.method) + '</span>' + escapeHtml(s.path) + '</span>' +
                '<span class="status-badge' + (isError ? ' error' : '') + '">' + s.status + '</span>' +
                '</div>' +
                '<div class="row2">' +
                '<span>' + formatTime(s.startedAt) + '</span>' +
                '<span>' + s.durationMs + ' ms</span>' +
                '</div>';
            li.addEventListener("click", function () {
                selectTrace(s.traceId);
            });
            listEl.appendChild(li);
        });
    }

    function selectTrace(traceId) {
        selectedTraceId = traceId;
        Array.from(listEl.children).forEach(function (li) {
            li.classList.toggle("selected", li.dataset.traceId === traceId);
        });
        fetchJson(API_TRACES + "/" + encodeURIComponent(traceId))
            .then(renderDetail)
            .catch(function (err) {
                detailEl.innerHTML = '<p class="empty-state">트레이스를 불러오지 못했습니다: ' + escapeHtml(err.message) + '</p>';
            });
    }

    // ---------- 상세(트리) ----------

    function renderDetail(trace) {
        var html = '<div class="trace-meta">' +
            '<code>' + escapeHtml(trace.traceId) + '</code>' +
            '<span class="method-badge">' + escapeHtml(trace.method) + '</span>' +
            '<span class="trace-path">' + escapeHtml(trace.path) + '</span>' +
            '<span class="trace-meta-sep">·</span>' +
            '<span class="status-badge' + (trace.status >= 400 ? ' error' : '') + '">status ' + trace.status + '</span>' +
            '<span class="trace-meta-sep">·</span>' +
            '<span>' + trace.durationMs + ' ms</span>' +
            '</div>';

        var roots = trace.roots || [];
        if (roots.length === 0) {
            html += '<p class="empty-state">계측된 빈 호출이 없습니다 (basePackages 설정을 확인하세요).</p>';
        } else {
            html += roots.map(function (root, i) {
                var label = roots.length > 1
                    ? '<div class="root-label">호출 흐름 #' + (i + 1) + (i > 0 ? ' — 원래 흐름과 별개로 실행됨 (예: 예외 핸들러)' : '') + '</div>'
                    : '';
                return label + renderNode(root, true);
            }).join('');
        }

        detailEl.innerHTML = html;
        bindNodeToggles();
    }

    function renderNode(node, isRoot) {
        var hasException = !!node.exception;
        var simpleClass = shortClassName(node.className);

        var html = '<div class="node' + (isRoot ? ' root' : '') + '">' +
            '<div class="node-header">' +
            '<span class="node-toggle">▸</span>' +
            '<span class="node-title" title="' + escapeHtml(node.className) + '">' +
            '<span class="node-class">' + escapeHtml(simpleClass) + '</span>' +
            '<span class="node-dot">.</span>' +
            '<span class="node-method">' + escapeHtml(node.methodName) + '()</span>' +
            '</span>' +
            (hasException ? '<span class="node-exception-badge">EXCEPTION</span>' : '') +
            '<span class="node-duration ' + durationClass(node.durationMs) + '">' + node.durationMs + ' ms</span>' +
            '</div>' +
            '<div class="node-body">' + renderNodeBody(node) + '</div>';

        if (node.children && node.children.length > 0) {
            html += '<div class="node-children">' +
                node.children.map(function (child) {
                    return renderNode(child, false);
                }).join("") +
                '</div>';
        }
        html += '</div>';
        return html;
    }

    function renderNodeBody(node) {
        var parts = [];

        parts.push(
            '<div class="section">' +
            '<div class="section-label">Class</div>' +
            '<code class="fqcn">' + escapeHtml(node.className) + '</code>' +
            '</div>'
        );

        parts.push(
            '<div class="section">' +
            '<div class="section-label">Args' + (node.args ? ' (' + node.args.length + ')' : ' (0)') + '</div>' +
            renderArgs(node.args) +
            '</div>'
        );

        if (node.exception) {
            parts.push(
                '<div class="section">' +
                '<div class="section-label section-label-error">Exception</div>' +
                '<div class="exception-block">' +
                '<div class="exception-type">' + escapeHtml(node.exception.type) + '</div>' +
                '<div class="exception-message">' + escapeHtml(node.exception.message || '') + '</div>' +
                '<pre class="exception-stack">' + escapeHtml((node.exception.stackTraceTop || []).join('\n')) + '</pre>' +
                '</div>' +
                '</div>'
            );
        } else if (node.returnValue) {
            parts.push(
                '<div class="section">' +
                '<div class="section-label">Return</div>' +
                renderReturn(node.returnValue) +
                '</div>'
            );
        }

        return parts.join("");
    }

    function renderArgs(args) {
        if (!args || args.length === 0) {
            return '<p class="empty-inline">없음</p>';
        }
        return '<div class="args-list">' + args.map(function (a) {
            return '<div class="arg-item">' +
                '<div class="arg-head">' +
                '<span class="arg-name">' + escapeHtml(a.name) + '</span>' +
                '<span class="type-badge">' + escapeHtml(a.type) + '</span>' +
                (a.truncated ? '<span class="truncated-badge">truncated</span>' : '') +
                '</div>' +
                '<div class="value-slot">' + renderValue(a.value) + '</div>' +
                '</div>';
        }).join('') + '</div>';
    }

    function renderReturn(rv) {
        return '<div class="arg-item">' +
            '<div class="arg-head">' +
            '<span class="type-badge">' + escapeHtml(rv.type) + '</span>' +
            (rv.truncated ? '<span class="truncated-badge">truncated</span>' : '') +
            '</div>' +
            '<div class="value-slot">' + renderValue(rv.value) + '</div>' +
            '</div>';
    }

    // value: 서버가 이미 JSON으로 내려준 값(객체/배열/문자열/숫자/불리언/null) — 필드별로 펼쳐서 보여준다.
    function renderValue(value) {
        if (value === null || value === undefined) {
            return '<span class="val val-null">null</span>';
        }
        if (typeof value === "string") {
            if (value === "***MASKED***") {
                return '<span class="val val-masked">MASKED</span>';
            }
            return '<span class="val val-string">"' + escapeHtml(value) + '"</span>';
        }
        if (typeof value === "number") {
            return '<span class="val val-number">' + value + '</span>';
        }
        if (typeof value === "boolean") {
            return '<span class="val val-boolean">' + value + '</span>';
        }
        if (Array.isArray(value)) {
            if (value.length === 0) {
                return '<span class="val val-empty">[ ]</span>';
            }
            return '<div class="val-tree">' + value.map(function (v, i) {
                return '<div class="kv-row"><span class="kv-key kv-index">' + i + '</span>' + renderValue(v) + '</div>';
            }).join('') + '</div>';
        }
        if (typeof value === "object") {
            var keys = Object.keys(value);
            if (keys.length === 0) {
                return '<span class="val val-empty">{ }</span>';
            }
            return '<div class="val-tree">' + keys.map(function (k) {
                return '<div class="kv-row"><span class="kv-key">' + escapeHtml(k) + '</span>' + renderValue(value[k]) + '</div>';
            }).join('') + '</div>';
        }
        return '<span class="val">' + escapeHtml(String(value)) + '</span>';
    }

    function shortClassName(fqcn) {
        if (!fqcn) return "";
        var idx = fqcn.lastIndexOf(".");
        return idx === -1 ? fqcn : fqcn.substring(idx + 1);
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    function bindNodeToggles() {
        detailEl.querySelectorAll(".node-header").forEach(function (header) {
            header.addEventListener("click", function () {
                var node = header.parentElement;
                var body = header.nextElementSibling;
                if (body && body.classList.contains("node-body")) {
                    body.classList.toggle("open");
                    node.classList.toggle("expanded", body.classList.contains("open"));
                }
            });
        });
    }

    // ---------- 액션 ----------

    function refreshList() {
        fetchJson(API_TRACES).then(renderList).catch(function () {
            // 목록 갱신 실패는 조용히 무시 (다음 새로고침에서 재시도)
        });
    }

    refreshBtn.addEventListener("click", refreshList);

    clearBtn.addEventListener("click", function () {
        fetchJson(API_TRACES, {method: "DELETE"}).then(function () {
            selectedTraceId = null;
            detailEl.innerHTML = '<p class="empty-state">왼쪽 목록에서 요청을 선택하세요.</p>';
            refreshList();
        });
    });

    autoRefreshToggle.addEventListener("change", function () {
        if (autoRefreshToggle.checked) {
            autoRefreshTimer = setInterval(refreshList, 3000);
        } else if (autoRefreshTimer) {
            clearInterval(autoRefreshTimer);
            autoRefreshTimer = null;
        }
    });

    refreshList();
})();

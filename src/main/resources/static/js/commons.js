export const commons =
{
    // 테마 정보 저장
    applyTheme()
    {
        const root = document.documentElement;
        root.classList.add('no-transition');

        const saved = localStorage.getItem('theme');
        const prefersDark = window.matchMedia?.('(prefers-color-scheme: dark)')?.matches;
        const theme = saved || (prefersDark ? 'dark' : 'light');

        root.setAttribute('data-theme', theme);

        requestAnimationFrame(() =>
        { requestAnimationFrame(() => { root.classList.remove('no-transition'); }); });
    },

    // 다크모드 전환
    toggleUi()
    {
        const root = document.documentElement;
        const isDark = root.getAttribute('data-theme') === 'dark';
        const next = isDark ? 'light' : 'dark';

        root.setAttribute('data-theme', next);
        localStorage.setItem('theme', next);
    },

    // 드로어 토글
    toggleDrawer()
    {
        const drawer = document.querySelector('.drawer');
        const btn = document.querySelector('.hamburger');
        const willOpen = !drawer.classList.contains('open');

        btn.classList.toggle('open', willOpen);
        drawer.classList.toggle('open', willOpen);
        btn.textContent = willOpen ? '✕' : '☰';
    },

    // 내 회원정보 페이지로 이동
    async goMyInfo()
    {
        const me = await this.fetchJson(
            "/api/auth/me",
            { method: "GET" },
            { parseJson: true }
        );

        if (!me || !me.userId) { this.showToast("회원 정보를 불러올 수 없습니다."); return; }

        location.href = `/user/${me.userId}`;
    },

    // 스크롤 제어
    scrollCtr(position)
    {
        if (position === 'top') { window.scrollTo({ top: 0, behavior: 'smooth' }); return; }
        if (position === 'bottom')
        { window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'smooth' }); return; }

        const target = document.getElementById(position);
        if (!target) { this.showToast?.('이동할 섹션을 찾을 수 없습니다.'); return; }

        const header = document.querySelector('header');
        const offset = header ? header.getBoundingClientRect().height : 0;
        const y = target.getBoundingClientRect().top + window.scrollY - offset - 8;

        window.scrollTo({ top: y, behavior: 'smooth' });
    },

    // 날짜 포맷
    formatDateTime(iso)
    {
        if (!iso) return '';
        const d = new Date(iso);
        const yy = d.getFullYear();
        const mm = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        const hh = String(d.getHours()).padStart(2, '0');
        const mi = String(d.getMinutes()).padStart(2, '0');
        return `${yy}.${mm}.${dd}. ${hh}:${mi}`;
    },

    // XSS 방어
    escapeHtml(str)
    {
        return String(str)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    },

    // 토스트 전역 변수
    toast: { duration: 5000, timer: null, progressTimer: null, remaining: 5000, start: null, paused: false },

    // 토스트 오픈
    showToast(text)
    {
        const toast = document.querySelector('.toast');
        const textEl = document.getElementById('toastText');
        const bar = document.querySelector('.progressBar');

        this.closeToast();

        textEl.textContent = text;
        toast.style.transform = 'translateX(0)';
        bar.style.width = '100%';

        const s = this.toast;
        s.remaining = s.duration;
        s.start = Date.now();
        s.paused = false;

        s.progressTimer = setInterval(() =>
        {
        if (s.paused) return;
        const left = s.remaining - (Date.now() - s.start);
        bar.style.width = `${Math.max(left / s.duration * 100, 0)}%`;
        if (left <= 0) this.closeToast();
        }, 50);

        s.timer = setTimeout(() => this.closeToast(), s.remaining);

        toast.onmouseenter = () => this.pauseToast();
        toast.onmouseleave = () => this.resumeToast();
    },

    // 토스트 정지
    pauseToast()
    {
        const s = this.toast;
        if (s.paused) return;
        s.paused = true;
        s.remaining -= Date.now() - s.start;
        clearTimeout(s.timer);
    },

    // 토스트 타이머
    resumeToast()
    {
        const s = this.toast;
        if (!s.paused) return;
        s.paused = false;
        s.start = Date.now();
        s.timer = setTimeout(() => this.closeToast(), s.remaining);
    },

    // 토스트 닫기
    closeToast()
    {
        const toast = document.querySelector('.toast');
        const bar = document.querySelector('.progressBar');
        const s = this.toast;

        toast.style.transform = 'translateX(100%)';

        clearTimeout(s.timer);
        clearInterval(s.progressTimer);

        Object.assign(s, { timer: null, progressTimer: null, remaining: s.duration, start: null, paused: false });
        if (bar) bar.style.width = '0%';
    },

    // 모달 열기
    openModal(modalId)
    {
        const overlay = document.querySelector('.modalOveray');
        const modal = document.getElementById(modalId);

        if (!overlay || !modal) { this.showToast?.('모달을 찾을 수 없습니다.'); return; }
        overlay.querySelectorAll('.modal').forEach(m => { m.style.display = 'none'; });

        overlay.style.display = 'flex';
        modal.style.display = 'flex';
    },

    // 모달 닫기
    closeModal(modalId)
    {
        const overlay = document.querySelector('.modalOveray');
        const modal = document.getElementById(modalId);

        if (!overlay || !modal)
        { this.showToast?.('모달을 찾을 수 없습니다.'); return; }

        modal.style.display = 'none';

        const hasVisibleModal = [...overlay.querySelectorAll('.modal')]
        .some(el => el.style.display === 'flex' || getComputedStyle(el).display !== 'none');

        if (!hasVisibleModal) overlay.style.display = 'none';
    },

    // 모달 confirm 버튼 핸들러를 항상 교체
    bindModalConfirm(modalId, handler)
    {
        const modal = document.getElementById(modalId);
        const confirmBtn = modal?.querySelector('button.btn.teal');

        if (!modal || !confirmBtn)
        { this.showToast?.('모달 확인 버튼을 찾을 수 없습니다.'); return null; }

        confirmBtn.onclick = null;
        confirmBtn.onclick = handler;

        return confirmBtn;
    },

    // 관리자 삭제 모달 입력값 초기화
    resetAdminDeleteModal()
    {
        const modal = document.getElementById('adminDeletedModal');
        const reasonInput = modal?.querySelector('input[name="reason"]');
        if (reasonInput) reasonInput.value = '';
    },

    // 입력값 가져오기
    getValueEl(inputEl) { return (inputEl?.value ?? '').trim(); },

    // 원샷 검증
    validate(inputEl, inputName, regex = null, failMsg = '', minLength = 0, maxLength = 0)
    {
        const value = this.getValueEl(inputEl);

        // 빈 값
        if (value === '')
        {
            this.showToast(`${inputName}을(를) 입력하세요.`);
            inputEl?.focus();
            return false;
        }

        // 최소 길이(0이면 무시)
        if (minLength > 0 && value.length < minLength)
        {
            this.showToast(`${inputName}은(는) 최소 ${minLength}자 이상 입력하세요.`);
            inputEl?.focus();
            return false;
        }

        // 최대 길이(0이면 무시)
        if (maxLength > 0 && value.length > maxLength)
        {
            this.showToast(`${inputName}은(는) ${maxLength}자 이내로 입력하세요.`);
            inputEl?.focus();
            return false;
        }

        // 정규식
        if (regex && !regex.test(value))
        {
            this.showToast(failMsg || `${inputName} 형식이 올바르지 않습니다.`);
            inputEl?.focus();
            return false;
        }

        return true;
    },

    // API 응답 실패
    async handleApiError(res, defaultMessage = "요청 처리 중 문제가 발생했습니다.")
    {
        if (res.status === 429) { this.showToast("잠시 후 다시 시도해주세요."); return; }
        try
        {
            const data = await res.json();
            if (data && data.message) { this.showToast(data.message); return; }
        }
        catch (_) { }

        this.showToast(defaultMessage);
    },

    // JSON API 래퍼
    async fetchJson(url, options = {},
    {
        defaultErrorMessage = "요청 처리 중 문제가 발생했습니다.",
        toastOnSuccess = null, parseJson = true
    } = {})
    {
        const opts = { credentials: "include", ...options };
        if (opts.body && typeof opts.body === "string")
        { opts.headers = { "Content-Type": "application/json", ...(opts.headers || {}) }; }
        try
        {
            const res = await fetch(url, opts);

            if (!res.ok) { await this.handleApiError(res, defaultErrorMessage); return null; }

            if (toastOnSuccess) this.showToast(toastOnSuccess);

            if (!parseJson) return { ok: true };

            const ct = (res.headers.get("content-type") || "").toLowerCase();
            if (!ct.includes("application/json")) return { ok: true };

            try { return await res.json(); } catch (_) { return { ok: true }; }
        }
        catch (_) { this.showToast("네트워크 오류가 발생했습니다."); return null; }
    },

    // POST JSON 편의 함수
    postJson(url, bodyObj, opts = {})
    { return this.fetchJson(url, { method: "POST", body: JSON.stringify(bodyObj) }, opts); },

    // 통합 검색
    searchUniv()
    {
        const f = document.forms?.univSearch;
        if (!f) { this.showToast('검색 폼을 찾을 수 없습니다.'); return; }

        const keywordEl = f.elements?.namedItem('keyword');
        if (!keywordEl) { this.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
        if (!this.validate(keywordEl, '검색어')) return;

        const keyword = this.getValueEl(keywordEl);

        const originalName = keywordEl.name;
        keywordEl.name = 'q';
        keywordEl.value = keyword;

        f.submit();
        keywordEl.name = originalName;
    },

    // 공통 페이지 유틸
    renderPagination(container, currentPage, totalPages, onPageClick, opts = {})
    {
        if (!container) return;

        const
        {
            maxVisible = 5,
            showFirstLast = false,
            scrollToTop = false
        } = opts;

        container.innerHTML = "";

        const clamp = (n, min, max) => Math.max(min, Math.min(max, n));

        const safeTotal = Math.max(1, Number(totalPages) || 1);
        const safeCurrent = clamp(Number(currentPage) || 0, 0, safeTotal - 1);

        const createBtn = (label, page, { isNow = false, disabled = false } = {}) =>
        {
            const a = document.createElement("a");
            a.className = "page" + (isNow ? " now" : "") + (disabled ? " disabled" : "");
            a.textContent = label;
            a.href = "javascript:void(0)";

            if (!disabled)
            {
                a.onclick = () =>
                {
                    if (scrollToTop) window.scrollTo({ top: 0, behavior: "smooth" });
                    onPageClick?.(page);
                };
            }
            else
            {
                a.onclick = e => e.preventDefault();
                a.setAttribute("aria-disabled", "true");
                a.setAttribute("tabindex", "-1");
            }
            return a;
        };

        const prevDisabled = (safeTotal <= 1) || (safeCurrent <= 0);
        const nextDisabled = (safeTotal <= 1) || (safeCurrent >= safeTotal - 1);

        // 처음
        if (showFirstLast) { container.appendChild(createBtn("<<", 0, { disabled: prevDisabled })); }

        // 이전
        container.appendChild(createBtn("<", Math.max(0, safeCurrent - 1), { disabled: prevDisabled }));

        // 번호 범위 계산
        if (safeTotal <= 1)
        { container.appendChild(createBtn("1", 0, { isNow: true, disabled: true })); }
        else
        {
            const mv = Math.max(1, Number(maxVisible) || 5);
            const half = Math.floor(mv / 2);

            let start = safeCurrent - half;
            let end = safeCurrent + half;

            if (mv % 2 === 0) end -= 1; // 짝수면 좌측 우선

            if (start < 0) { end += -start; start = 0; }
            if (end > safeTotal - 1) { start -= (end - (safeTotal - 1)); end = safeTotal - 1; }
            start = Math.max(0, start);

            for (let i = start; i <= end; i++)
            { container.appendChild(createBtn(String(i + 1), i, { isNow: i === safeCurrent })); }
        }

        // 다음
        container.appendChild(createBtn(">", Math.min(safeTotal - 1, safeCurrent + 1), { disabled: nextDisabled }));

        // 끝
        if (showFirstLast)
        { container.appendChild(createBtn(">>", safeTotal - 1, { disabled: nextDisabled })); }
    },
};

// 바인딩
export function bindCommons(commons)
{
    // 전역 함수
    window.toggleUi = () => commons.toggleUi();
    window.toggleDrawer = () => commons.toggleDrawer();
    window.goMyInfo = () => commons.goMyInfo();
    window.scrollCtr = (pos) => commons.scrollCtr(pos);

    window.showToast = (text) => commons.showToast(text);
    window.closeToast = () => commons.closeToast();
    window.openModal = (id) => commons.openModal(id);
    window.closeModal = (id) => commons.closeModal(id);

    window.getValueEl = (el) => commons.getValueEl(el);
    window.searchUniv = () => commons.searchUniv();

    // 공용 폼 바인딩(있을 때만)
    const f = document.forms?.univSearch;
    if (f)
    {
        f.addEventListener('submit', (e) =>
        {
            e.preventDefault();
            commons.searchUniv();
        });
    }
}
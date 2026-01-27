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

    // 통합 검색
    searchUniv()
    {
        const f = document.forms?.univSearch;
        const keywordEl = f?.keyword;

        if (!keywordEl) { this.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
        if (!this.validate(keywordEl, '검색어')) return;

        const keyword = this.getValueEl(keywordEl);

        this.showToast(`"${keyword}" 검색`);
    }
};
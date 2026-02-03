// 알람 페이지 초기화
function initAlarmPage(commons)
{
    if (!document.querySelector('[data-alarm-page="1"]')) return;

    const listEl = document.getElementById('alarmList');
    const paginationEl = document.getElementById('alarmPagination');
    const unreadOnlyEl = document.getElementById('alarmUnreadOnly');

    if (!listEl || !paginationEl || !unreadOnlyEl) return;

    const state =
    {
        page: 0,
        totalPages: 1,
        unreadOnly: false
    };

    const applyFromQuery = () =>
    {
        const q = new URLSearchParams(location.search);
        const p = Number(q.get('page') ?? '0');
        const u = q.get('unreadOnly');

        state.page = Number.isFinite(p) ? Math.max(0, p) : 0;
        state.unreadOnly = (u === '1' || u === 'true');
        unreadOnlyEl.checked = state.unreadOnly;
    };

    const pushQuery = () =>
    {
        const q = new URLSearchParams(location.search);
        q.set('page', String(state.page));
        q.set('unreadOnly', state.unreadOnly ? '1' : '0');
        history.replaceState({}, '', `${location.pathname}?${q.toString()}`);
    };

    const clickAlarmAndGo = async (alarmId) =>
    {
        if (!alarmId) return;

        try
        {
            const res = await fetch(`/api/user/alarm/${alarmId}/click`, { method: 'POST', credentials: 'include' });
            if (!res.ok) { await commons.handleApiError(res, '알람 처리에 실패했습니다.'); return; }

            const ct = (res.headers.get('content-type') || '').toLowerCase();
            let targetUrl = '';

            if (ct.includes('application/json'))
            {
                const json = await res.json();
                targetUrl = json?.targetUrl || '';
            }
            else { targetUrl = (await res.text()).trim(); }

            if (!targetUrl) { commons.showToast('이동할 위치를 찾지 못했습니다.'); return; }
            location.href = targetUrl;
        }
        catch (_) { commons.showToast('네트워크 오류가 발생했습니다.'); }
    };

    const renderRows = (items) =>
    {
        // 헤더 row 유지
        const head = listEl.querySelector('.row');
        listEl.innerHTML = '';
        if (head) listEl.appendChild(head);

        if (!items.length)
        {
            const empty = document.createElement('div');
            empty.className = 'pdSm lightText textCenter';
            empty.textContent = '알람이 없습니다.';
            listEl.appendChild(empty);
            return;
        }

        for (const it of items)
        {
            const a = document.createElement('a');
            a.className = 'row widthFull flex alignCenter';
            a.href = '#';

            const unread = (it.unread === true);
            const lightClass = unread ? '' : ' lightText';

            const actor = (it.actorName && String(it.actorName).trim().length) ? it.actorName : '알 수 없음';
            const actionText = (it.type === 'RECOMMENT') ? `${actor} 님의 대댓글` : `${actor} 님의 댓글`;

            a.innerHTML = `
                <div class="widthFull flexColumn">
                    <p class="text2${lightClass}">${actionText}</p>
                    <p class="text1${lightClass}">${commons.escapeHtml(it.preview || '')}</p>
                </div>
                <p class="text1 textCenter date${lightClass}">${commons.formatDateTime(it.alarmAt)}</p>
            `;

            a.onclick = (e) =>
            {
                e.preventDefault();
                clickAlarmAndGo(it.alarmId);
            };

            listEl.appendChild(a);
        }
    };

    const renderPager = () =>
    {
        commons.renderPagination(
            paginationEl,
            state.page,
            state.totalPages,
            (p) =>
            {
                state.page = p;
                pushQuery();
                load();
            },
            { maxVisible: 5, showFirstLast: false, scrollToTop: true }
        );
    };

    const load = async () =>
    {
        const data = await commons.fetchJson(
            `/api/user/alarm?page=${state.page}&unreadOnly=${state.unreadOnly}`,
            { method: 'GET' },
            { defaultErrorMessage: '알람을 불러오지 못했습니다.', parseJson: true }
        );

        if (!data) return;

        const content = data.content ?? [];
        state.totalPages = Number(data.totalPages ?? 1) || 1;
        state.page = Number(data.number ?? state.page) || 0;

        renderRows(content);
        renderPager();
    };

    unreadOnlyEl.onchange = () =>
    {
        state.unreadOnly = unreadOnlyEl.checked;
        state.page = 0;
        pushQuery();
        load();
    };

    applyFromQuery();
    pushQuery();
    load();
}


// 안 읽은 알람 갯수 표시
function initAlarmHeader(commons) { refreshHeaderUnread(commons); }

// 토스트 알림
async function refreshHeaderUnread(commons)
{
    const data = await commons.fetchJson(
        `/api/user/alarm/unread-count`,
        { method: 'GET' },
        { parseJson: true }
    );

    if (!data) return;

    const n = (typeof data === 'number')
    ? data : (Number(data?.unreadCount ?? 0) || 0);
    applyHeaderUnread(n);

    if (n > 0) { commons.showToast(`안 읽은 알람이 ${n}개 있습니다.`); }
}

// 헤더 렌더링
function applyHeaderUnread(unreadCount)
{
    const links =
    [
        document.getElementById('alarmLinkTop'),
        document.getElementById('alarmLinkDrawer'),
        ...document.querySelectorAll('a[href="/user/alarm"]')
    ].filter(Boolean);

    for (const a of links)
    {
        const n = Number(unreadCount) || 0;
        a.textContent = (n > 0) ? `알람(${n})` : '알람';
        a.classList.toggle('bold', n > 0);
        a.classList.toggle('mainColorText', n > 0);
    }
}

// 실시간 통신
function initAlarmRealtime(commons)
{
    // STOMP 라이브러리 없으면 종료
    if (typeof SockJS === 'undefined' || typeof StompJs === 'undefined') return;

    let lastCount = null;

    const sock = new SockJS('/ws');
    const client = new StompJs.Client({
        webSocketFactory: () => sock,
        reconnectDelay: 3000
    });

    client.onConnect = () =>
    {
        client.subscribe('/user/queue/alarm/unread-count', (msg) =>
        {
            let data = null;
            try { data = JSON.parse(msg.body); } catch (_) { return; }

            const n = Number(data?.unreadCount);
            if (!Number.isFinite(n)) return;

            applyHeaderUnread(n);

            // 증가했을 때만 토스트
            if (lastCount != null && n > lastCount)
            { commons.showToast(`읽지 않은 알람이 ${n}개 있습니다.`); }
            lastCount = n;
        });
    };

    client.activate();
}

// 바인딩
export function bindAlarm(commons)
{
    initAlarmPage(commons);

    const header = document.querySelector('header.header[data-auth]');
    const isAuth = header?.dataset?.auth === 'true';

    if (!isAuth) return;

    initAlarmHeader(commons);
    initAlarmRealtime(commons);
}
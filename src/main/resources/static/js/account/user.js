// 정규식/메시지
const NAME_REGEX = /^.{2,10}$/;
const NAME_MSG = '닉네임 형식이 올바르지 않습니다.';

let __selectedSanctionId = null;

// 상태 판단
const loginLogState =
{
    page: 0,
    size: 10,
    totalPages: 0,
    loaded: false
};

// 사용자
const authState =
{
    loaded: false,
    userId: null,
    isAdmin: false,
};

// 활동 기록
const myPostState = { page: 0, size: 10 };
const myCommentState = { page: 0, size: 10 };


// 포맷
function toLongText(n)
{
    const v = Number(n);
    if (v === 0) return '영구';
    if (v === 1) return '1일';
    if (v === 7) return '7일';
    if (v === 30) return '30일';
    return `${v}일`;
}


// 회원정보 조회
async function bindUserInfoPage(commons)
{
    // 관리자 여부 판별
    const header = document.querySelector('header.header[data-auth]');
    const isAdmin = header?.dataset?.admin === 'true';

    // 로그인 사용자 정보 1회 로딩
    if (!authState.loaded)
    {
        const isAuth = header?.dataset?.auth === 'true';

        if (isAuth)
        {
            const me = await commons.fetchJson(
                '/api/auth/me',
                { method: 'GET' },
                { parseJson: true, defaultErrorMessage: null }
            );

            authState.userId = me?.userId ?? me?.id ?? null;
            authState.isAdmin = isAdmin;
        }
        else
        {
            authState.userId = null;
            authState.isAdmin = false;
        }

        authState.loaded = true;
    }

    // userInfo.html 아니면 아무것도 안 함
    if (!document.getElementById('userInfo')) return;

    // URL에서 /user/{userId} 추출
    const parts = (location.pathname || '').split('/').filter(Boolean);
    const idx = parts.indexOf('user');
    const targetUserId = (idx !== -1 ? parts[idx + 1] : null);

    if (!targetUserId)
    { commons.showToast('회원정보를 불러올 수 없습니다.'); return; }

    // 카드 API 호출
    const card = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/card`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: '회원 정보를 불러올 수 없습니다.' }
    );
    if (!card) return;

    // 값 바인딩
    const setText = (id, v) =>
    {
        const el = document.getElementById(id);
        if (!el) return;
        el.textContent = (v ?? '');
    };

    setText('oldName', card.name);

    // ID: 권한 없으면 숨김 처리
    const idWrap = document.getElementById('userIdText')?.closest('.cardItem');
    if (card.userId && idWrap)
    {
        setText('userIdText', card.userId);
        idWrap.style.display = '';
    }
    else if (idWrap) idWrap.style.display = 'none';

    // 메일: 권한 없으면 숨김 처리
    const mailWrap = document.getElementById('userMailText')?.closest('.cardItem');
    if (card.mail && mailWrap)
    {
        setText('userMailText', card.mail);
        mailWrap.style.display = '';
    }
    else if (mailWrap) mailWrap.style.display = 'none';

    // 가입 IP: 권한 없으면 숨김 처리
    const ipWrap = document.getElementById('joinIpText')?.closest('.cardItem');
    if (card.joinIp && ipWrap)
    {
        setText('joinIpText', card.joinIp);
        ipWrap.style.display = '';
    }
    else if (ipWrap) ipWrap.style.display = 'none';

    // 가입일
    setText('joinedAtText', card.joinedAt ? String(card.joinedAt).replace('T', ' ') : '');

    // 상태
    const status = card.blocked ? '차단됨' : '정상';
    setText('statusText', status);

    // 버튼 노출 정책
    const btnToggle = document.getElementById('btnNameToggle');
    const btnApply = document.getElementById('btnNameApply');
    const form = document.forms?.nameForm;

    // 닉네임 변경: 본인 or 관리자만
    const canEditName = !!card.mail || !!card.joinIp;
    if (btnToggle) btnToggle.style.display = canEditName ? '' : 'none';
    if (btnApply) btnApply.style.display = canEditName ? '' : 'none';
    if (form) form.style.display = 'none';

    // 차단
    bindBlockModal(commons, targetUserId, card);
    if (isAdmin) bindBlockCancelModal(commons, targetUserId);
    await bindSanctionLogList(commons, targetUserId, isAdmin);

    // 로그인 기록
    const okLoginLog = canViewLoginLogs(targetUserId);
    if (okLoginLog) await bindLoginLogList(commons, targetUserId, 0);
    else
    {
        const loginWrap = document.getElementById('loginLog');
        if (loginWrap) loginWrap.style.display = 'none';
    }

    // 활동 기록
    bindMyPostList(commons, targetUserId, 0);
    bindMyCommentList(commons, targetUserId, 0);
}

// 닉네임 변경 폼 토글
function toggleForm()
{
    const form = document.forms?.nameForm;
    if (!form) { window.showToast?.('닉네임 변경 폼을 찾을 수 없습니다.'); return; }

    const nameText  = document.getElementById('oldName');
    const toggleBtn = document.getElementById('btnNameToggle');
    const nameInput = form.elements?.namedItem('name');

    const isOpen = getComputedStyle(form).display !== 'none';

    if (isOpen)
    {
        form.style.display = 'none';
        if (nameText) nameText.style.display = '';
        if (toggleBtn) toggleBtn.textContent = '변경';
        form.reset?.();
        return;
    }

    form.style.display = 'flex';
    if (nameText) nameText.style.display = 'none';
    if (toggleBtn) toggleBtn.textContent = '취소';

    if (nameInput && nameText)
    { nameInput.value = nameText.textContent.trim(); nameInput.focus(); nameInput.select?.(); }
}

// 닉네임 변경
async function subChangeName(commons, e)
{
    e?.preventDefault?.();

    const form = document.forms?.nameForm;
    if (!form) return;

    const nameInput = form.elements?.namedItem('name');
    if (!nameInput) return;

    if (!commons.validate(nameInput, '닉네임', NAME_REGEX, NAME_MSG, 2, 10)) return;

    const parts = (location.pathname || '').split('/').filter(Boolean);
    const idx = parts.indexOf('user');
    const targetUserId = idx !== -1 ? parts[idx + 1] : null;
    if (!targetUserId) { commons.showToast('회원정보를 불러올 수 없습니다.'); return; }

    const newName = commons.getValueEl(nameInput);

    const ok = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/name`,
        {
            method: 'PATCH',
            body: JSON.stringify({ name: newName })
        },
        {
            parseJson: true,
            defaultErrorMessage: '닉네임 변경에 실패했습니다.'
        }
    );

    if (!ok) return;

    commons.showToast('닉네임이 변경됐습니다.');

    // 폼 닫기
    form.style.display = 'none';

    const nameText = document.getElementById('oldName');
    if (nameText) nameText.style.display = '';

    const toggleBtn = document.getElementById('btnNameToggle');
    if (toggleBtn) toggleBtn.textContent = '변경';
    form.reset?.();

    await bindUserInfoPage(commons);
}

// 차단 모달 바인딩
function bindBlockModal(commons, targetUserId, card)
{
    if (!document.getElementById('userInfo')) return;
    const btnBlock = document.getElementById('btnBlock');
    if (!btnBlock) return;
    if (card?.blocked) return;

    const modal = document.getElementById('blockModal');
    if (!modal) return;

    const longEl = modal.querySelector('select[name="long"]');
    const reasonEl = modal.querySelector('input[name="reason"]');

    // confirm 버튼 핸들러 교체
    commons.bindModalConfirm('blockModal', async () =>
    {
        if (!targetUserId) { commons.showToast('대상 사용자를 찾을 수 없습니다.'); return; }
        if (!longEl || !reasonEl) { commons.showToast('차단 입력 폼을 찾을 수 없습니다.'); return; }

        // 기간
        const sanctionLong = Number((longEl.value ?? '').trim());
        if (![0, 1, 7, 30].includes(sanctionLong))
        { commons.showToast('차단 기간이 올바르지 않습니다.'); return; }

        // 사유
        if (!commons.validate(reasonEl, '차단 사유', null, '', 1, 100)) return;
        const reason = commons.getValueEl(reasonEl);

        const res = await commons.fetchJson(
            `/api/admin/user/${encodeURIComponent(targetUserId)}/sanction`,
            {
                method: 'POST',
                body: JSON.stringify({ sanctionLong, reason })
            },
            {
                parseJson: true,
                defaultErrorMessage: '차단 처리에 실패했습니다.'
            }
        );

        if (!res) return;

        commons.showToast('차단 처리 완료');
        commons.closeModal('blockModal');

        // 입력값 초기화
        reasonEl.value = '';

        // 카드 재조회
        await bindUserInfoPage(commons);
    });

    btnBlock.addEventListener('click', () =>
    {
        if (reasonEl) reasonEl.value = '';
        if (longEl) longEl.value = '1';
    }, { once: false });
}

// 목록 불러오기
async function bindSanctionLogList(commons, targetUserId, isAdmin)
{
    const wrap = document.getElementById('sanctionLog');
    const listEl = document.getElementById('sanctionLogList');
    if (!wrap || !listEl) return;
    wrap.style.display = '';

    // 헤더 row만 남기고 싹 지움
    const rows = Array.from(listEl.querySelectorAll('.row'));
    for (let i = 1; i < rows.length; i++) rows[i].remove();

    const data = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/sanction/logs`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: null }
    );

    if (!data) { wrap.style.display = 'none'; return; }

    const items = Array.isArray(data.items) ? data.items : [];

    // 0건이면 섹션 자체를 숨김
    if (items.length === 0) { wrap.style.display = 'none'; return; }

    // 데이터 있으면 렌더링
    for (const it of items)
    {
        const ip = it.ip ?? '';
        const region = it.region ?? 'UNKNOWN';
        const loginAt = commons.formatDateTime(it.loginAt);
        const logoutAt = it.logoutAt ? commons.formatDateTime(it.logoutAt) : '';

        const statusText = logoutAt ? `로그아웃: ${logoutAt}` : '세션 유지 중';
        const ipText = `${ip}(${region})`;

        const row = document.createElement('div');
        row.className = 'row widthFull flex alignCenter';

        row.innerHTML =
        `
            <div class="widthFull flexColumn">
                <p class="text2 lightText">${commons.escapeHtml(statusText)}</p>
                <p class="text1">${commons.escapeHtml(ipText)}</p>
            </div>
            <p class="text1 textCenter date">${commons.escapeHtml(loginAt)}</p>
        `;

        listEl.appendChild(row);
    }
}

// 제재 내역 삭제 모달 바인딩
function openBlockCancelModal(commons, btnEl)
{
    const row = btnEl?.closest?.('.blockLog');
    const sid = row?.dataset?.sanctionId;

    if (!sid || !/^\d+$/.test(String(sid)))
    { commons.showToast('차단 기록 정보를 찾을 수 없습니다.'); return; }

    __selectedSanctionId = Number(sid);
    commons.openModal('blockCancelModal');
}

// 제재 내역 삭제
function bindBlockCancelModal(commons, targetUserId)
{
    const modal = document.getElementById('blockCancelModal');
    if (!modal) return;

    commons.bindModalConfirm('blockCancelModal', async () =>
    {
        if (!targetUserId) { commons.showToast('대상 사용자를 찾을 수 없습니다.'); return; }
        if (!__selectedSanctionId) { commons.showToast('삭제할 차단 기록이 없습니다.'); return; }

        const ok = await commons.fetchJson(
            `/api/admin/user/${encodeURIComponent(targetUserId)}/sanction/logs/${encodeURIComponent(__selectedSanctionId)}`,
            { method: 'DELETE' },
            { parseJson: true, defaultErrorMessage: '차단 기록 삭제에 실패했습니다.' }
        );

        if (!ok) return;

        commons.showToast('차단 기록을 삭제했습니다.');
        __selectedSanctionId = null;
        commons.closeModal('blockCancelModal');

        // 카드/상태/리스트까지 한 번에 최신화
        await bindUserInfoPage(commons);
    });
}

// 로그인 기록 열람 권한 판별
function canViewLoginLogs(targetUserId)
{
    // 관리자면 바로 허용
    if (authState.isAdmin) return true;

    // 로그인 안 했으면 불가
    if (!authState.userId) return false;

    // 본인만 허용
    return String(authState.userId) === String(targetUserId);
}

// 로그인 기록 조회
async function bindLoginLogList(commons, targetUserId, page)
{
    const wrap = document.getElementById('loginLog');
    const listEl = document.getElementById('loginLogList');
    const pagerEl = document.getElementById('loginLogPagination');
    if (!wrap || !listEl || !pagerEl) return;

    const safePage = Math.max(0, page | 0);
    loginLogState.page = safePage;

    const params = new URLSearchParams();
    params.set('page', String(safePage));
    params.set('size', String(loginLogState.size));

    const data = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/login/logs?${params.toString()}`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: null }
    );

    // 권한 없거나 실패면 섹션 숨김
    if (!data)
    { wrap.style.display = 'none'; return; }

    const items = Array.isArray(data.content) ? data.content : [];
    const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
    const currentPage = Number.isFinite(data.number) ? data.number : safePage;

    // 0건이면 섹션 통째로 숨김
    if (items.length === 0)
    { wrap.style.display = 'none'; return; }

    wrap.style.display = '';

    const rows = Array.from(listEl.querySelectorAll('.row'));
    for (let i = 1; i < rows.length; i++) rows[i].remove();

    for (const it of items)
    {
        const ip = it.ip ?? '';
        const region = it.region ?? 'UNKNOWN';
        const loginAt = commons.formatDateTime(it.loginAt);
        const logoutAt = it.logoutAt ? commons.formatDateTime(it.logoutAt) : '';

        const row = document.createElement('div');
        row.className = 'row widthFull flex alignCenter';

        row.innerHTML =
        `
            <div class="widthFull flexColumn">
                <p class="text2 lightText">${logoutAt ? `${commons.escapeHtml(logoutAt)}에 로그아웃` : '세션 유지 중'}</p>
                <p class="text1">${commons.escapeHtml(ip)} (${commons.escapeHtml(region)})</p>
            </div>
            <p class="text1 textCenter date">${commons.escapeHtml(loginAt)}</p>
        `;

        listEl.appendChild(row);
    }

    commons.renderPagination(pagerEl, currentPage, totalPages, (p) =>
    { bindLoginLogList(commons, targetUserId, p); });
}

// 게시글 조회
async function bindMyPostList(commons, targetUserId, page)
{
    const wrap = document.getElementById('myPost');
    const listEl = document.getElementById('myPostList');
    const pagerEl = document.getElementById('myPostPagination');
    if (!wrap || !listEl || !pagerEl) return;

    const safePage = Math.max(0, page | 0);
    myPostState.page = safePage;

    const params = new URLSearchParams();
    params.set('page', String(safePage));
    params.set('size', String(myPostState.size));

    const data = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/activity/posts?${params.toString()}`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: null }
    );

    if (!data) { wrap.style.display = 'none'; return; }

    const items = Array.isArray(data.content) ? data.content : [];
    const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
    const currentPage = Number.isFinite(data.number) ? data.number : safePage;

    if (items.length === 0) { wrap.style.display = 'none'; return; }

    wrap.style.display = '';

    const rows = Array.from(listEl.querySelectorAll('.row'));
    for (let i = 1; i < rows.length; i++) rows[i].remove();

    for (const it of items)
    {
        const boardName = it.boardName ?? '';
        const categoryName = it.categoryName ?? '';
        const title = it.title ?? '';
        const createdAt = commons.formatDateTime(it.createdAt);

        const boardId = it.boardId;
        const postId = it.postId;
        const href = (boardId && postId != null)
        ? `/${encodeURIComponent(boardId)}/post/${postId}` : '#';

        const sub = categoryName ? `${boardName} - ${categoryName}` : boardName;

        const a = document.createElement('a');
        a.className = 'row widthFull flex alignCenter';
        a.href = href;

        a.innerHTML =
        `
            <div class="widthFull flexColumn">
                <p class="text2 lightText">${commons.escapeHtml(sub)}</p>
                <p class="text1">${commons.escapeHtml(title)}</p>
            </div>
            <p class="text1 textCenter date">${commons.escapeHtml(createdAt)}</p>
        `;

        listEl.appendChild(a);
    }

    commons.renderPagination(pagerEl, currentPage, totalPages, (p) =>
    { bindMyPostList(commons, targetUserId, p); });
}

// 작성 댓글 조회
async function bindMyCommentList(commons, targetUserId, page)
{
    const wrap = document.getElementById('myComment');
    const listEl = document.getElementById('myCommentList');
    const pagerEl = document.getElementById('myCommentPagination');
    if (!wrap || !listEl || !pagerEl) return;

    const safePage = Math.max(0, page | 0);
    myCommentState.page = safePage;

    const params = new URLSearchParams();
    params.set('page', String(safePage));
    params.set('size', String(myCommentState.size));

    const data = await commons.fetchJson(
        `/api/user/${encodeURIComponent(targetUserId)}/activity/comments?${params.toString()}`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: null }
    );

    if (!data) { wrap.style.display = 'none'; return; }

    const items = Array.isArray(data.content) ? data.content : [];
    const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
    const currentPage = Number.isFinite(data.number) ? data.number : safePage;

    // 0건이면 섹션 숨김
    if (items.length === 0) { wrap.style.display = 'none'; return; }

    wrap.style.display = '';

    // 헤더 row만 남기고 싹 지움
    const rows = Array.from(listEl.querySelectorAll('.row'));
    for (let i = 1; i < rows.length; i++) rows[i].remove();

    for (const it of items)
    {
        const boardName = it.boardName ?? '';
        const categoryName = it.categoryName ?? '';
        const content = it.content ?? '';
        const createdAt = commons.formatDateTime(it.createdAt);

        const boardId = it.boardId;
        const postId = it.postId;
        const href = (boardId && postId != null)
            ? `/${encodeURIComponent(boardId)}/post/${postId}` : '#';

        const sub = categoryName ? `${boardName} - ${categoryName}` : boardName;

        const a = document.createElement('a');
        a.className = 'row widthFull flex alignCenter';
        a.href = href;

        a.innerHTML =
        `
            <div class="widthFull flexColumn">
                <p class="text2 lightText">${commons.escapeHtml(sub)}</p>
                <p class="text1">${commons.escapeHtml(content)}</p>
            </div>
            <p class="text1 textCenter date">${commons.escapeHtml(createdAt)}</p>
        `;

        listEl.appendChild(a);
    }

    commons.renderPagination(pagerEl, currentPage, totalPages, (p) =>
    { bindMyCommentList(commons, targetUserId, p); });
}

// 바인딩
export function bindUser(commons)
{
    window.toggleForm = toggleForm;
    window.subChangeName = (e) => subChangeName(commons, e || window.event);
    window.openBlockCancelModal = (btnEl) => openBlockCancelModal(commons, btnEl);
    bindUserInfoPage(commons);
}
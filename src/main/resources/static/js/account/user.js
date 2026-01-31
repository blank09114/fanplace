// 정규식/메시지
const NAME_REGEX = /^.{2,10}$/;
const NAME_MSG = '닉네임 형식이 올바르지 않습니다.';

// 회원정보 조회
async function bindUserInfoPage(commons)
{
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
    await bindSanctionLogList(commons, targetUserId);
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

// 날짜 포맷
function formatDateTime(iso)
{
    if (!iso) return '';
    const d = new Date(iso);
    const yy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    const hh = String(d.getHours()).padStart(2, '0');
    const mi = String(d.getMinutes()).padStart(2, '0');
    return `${yy}.${mm}.${dd}. ${hh}:${mi}`;
}

// 포맷
function toLongText(n)
{
    const v = Number(n);
    if (v === 0) return '영구';
    if (v === 1) return '1일';
    if (v === 7) return '7일';
    if (v === 30) return '30일';
    // 혹시 모르는 값 방어
    return `${v}일`;
}

// 목록 불러오기
async function bindSanctionLogList(commons, targetUserId)
{
    const wrap = document.getElementById('sanctionLog');
    const listEl = document.getElementById('sanctionLogList');
    if (!wrap || !listEl) return;
    wrap.style.display = '';

    // 헤더 row만 남기고 싹 지움
    const rows = Array.from(listEl.querySelectorAll('.row'));
    for (let i = 1; i < rows.length; i++) rows[i].remove();

    const data = await commons.fetchJson(
        `/api/admin/user/${encodeURIComponent(targetUserId)}/sanction/logs`,
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
        const row = document.createElement('div');
        row.className = 'blockLog row widthFull flex alignCenter';
        row.dataset.sanctionId = it.sanctionId;

        const longText = toLongText(it.sanctionLong);
        const reason = (it.reason ?? '');
        const atText = formatDateTime(it.sanctionedAt);

        row.innerHTML =
        `
            <button class="blockCancle textBtn" onclick="openModal('blockCancelModal')">X</button>
            <div class="widthFull flexColumn">
                <p class="text2 lightText">${escapeHtml(longText)}</p>
                <p class="text1">${escapeHtml(reason)}</p>
            </div>
            <p class="text1 textCenter date">${escapeHtml(atText)}</p>
        `;

        listEl.appendChild(row);
    }
}

// XSS 방어
function escapeHtml(str)
{
    return String(str)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

// 바인딩
export function bindUser(commons)
{
    window.toggleForm = toggleForm;
    window.subChangeName = (e) => subChangeName(commons, e || window.event);
    bindUserInfoPage(commons);
}
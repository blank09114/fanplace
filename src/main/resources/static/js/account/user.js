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

    const btnBlock = document.getElementById('btnBlock');
    if (btnBlock)
    {
        const show = isAdmin && !card.blocked;
        btnBlock.style.display = show ? '' : 'none';
    }
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
function subChangeName(commons, e)
{
    e?.preventDefault?.();

    const form = document.forms?.nameForm;
    if (!form) return;

    const nameInput = form.elements?.namedItem('name');
    if (!nameInput) return;

    if (!commons.validate(nameInput, '닉네임', NAME_REGEX, NAME_MSG, 2, 10)) return;

    const newName = commons.getValueEl(nameInput);
    commons.showToast(`닉네임이 "${newName}"(으)로 변경됐습니다.`);

    const nameText  = document.getElementById('oldName');
    const toggleBtn = document.getElementById('btnNameToggle');

    if (nameText) { nameText.textContent = newName; nameText.style.display = ''; }

    form.style.display = 'none';
    if (toggleBtn) toggleBtn.textContent = '변경';
    form.reset?.();
}

// 바인딩
export function bindUser(commons)
{
    window.toggleForm = toggleForm;
    window.subChangeName = (e) => subChangeName(commons, e || window.event);
    bindUserInfoPage(commons);
}

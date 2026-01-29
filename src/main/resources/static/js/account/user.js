// 정규식/메시지
const NAME_REGEX = /^.{2,10}$/;
const NAME_MSG = '닉네임 형식이 올바르지 않습니다.';

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
}

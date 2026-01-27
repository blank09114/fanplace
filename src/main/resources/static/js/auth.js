// 정규식/메시지
const REGEX =
{
    name: /^.{2,10}$/,
    mail: /^[a-zA-Z0-9._%+-]+@naver\.com$/,
    id: /^[a-zA-Z][a-zA-Z0-9]{3,19}$/,
    pw: /^[a-zA-Z0-9]{8,40}$/
};
const MSG =
{
    name: '닉네임 형식이 올바르지 않습니다.',
    mail: '이메일 형식이 올바르지 않습니다.',
    id: 'ID 형식이 올바르지 않습니다.',
    pw: 'PW 형식이 올바르지 않습니다.'
};

let checkedId = '';

// ID 중복 검사 상태 초기화
function resetIdCheck()
{
    checkedId = '';
    const ok = document.getElementById('ok');
    const no = document.getElementById('no');
    if (ok) ok.style.display = 'none';
    if (no) no.style.display = 'none';
}

// ID 중복 검사
async function usingCheck(commons)
{
    const f = document.forms.joinForm;
    const idEl = f?.id;
    if (!idEl) return;

    if (!commons.validate(idEl, 'ID', REGEX.id, MSG.id, 6, 20)) return;

    const id = commons.getValueEl(idEl);
    const ok = document.getElementById('ok');
    const no = document.getElementById('no');

    const data = await commons.postJson("/api/auth/join/check-id", { userId: id });
    if (!data) return;

    const available = !!data.available;

    if (ok) ok.style.display = available ? 'block' : 'none';
    if (no) no.style.display = available ? 'none' : 'block';

    commons.showToast(available ? '사용 가능한 ID입니다.' : '이미 사용 중인 ID입니다.');
    checkedId = available ? id : '';
}

// ID 중복 검사 여부 확인
function isIdUsingCheck(commons, idEl)
{
    if (commons.getValueEl(idEl) !== checkedId)
    { commons.showToast('ID 중복 확인을 진행해주세요.'); return false; }
    return true;
}

// 비밀번호 보기
function togglePw(formName, checkboxEl)
{
    const form = document.forms?.[formName];
    if (!form) return;

    const pwInputs = form.querySelectorAll('.pwInput');

    pwInputs.forEach(input => { input.type = checkboxEl.checked ? 'text' : 'password'; });
}

// 회원가입
async function join(commons)
{
    const f = document.forms.joinForm;
    if (!f) return;

    if (!commons.validate(f.name, '닉네임', REGEX.name, MSG.name, 2, 10)) return;
    if (!commons.validate(f.id, 'ID', REGEX.id, MSG.id, 6, 20)) return;
    if (!isIdUsingCheck(commons, f.id)) { f.id.focus(); return; }
    if (!commons.validate(f.pw, '비밀번호', REGEX.pw, MSG.pw, 8, 40)) return;
    if (!commons.validate(f.mail, '메일 주소', REGEX.mail, MSG.mail)) return;

    const checks = f.querySelectorAll("input[type='checkbox']");
    for (const c of checks)
    { if (!c.checked) { commons.showToast('필수 항목에 모두 동의하세요.'); c.focus?.(); return; } }

    const body =
    {
        userId: commons.getValueEl(f.id),
        userName: commons.getValueEl(f.name),
        userPw: commons.getValueEl(f.pw),
        userMail: commons.getValueEl(f.mail),
    };

    commons.showToast("메일 발송 중….");
    const res = await commons.postJson("/api/auth/join/request", body,
    {
        toastOnSuccess: "회원가입 인증 메일을 발송했습니다.",
        parseJson: true
    });

    if (!res) return;
}

// 토큰 재발급
async function token(commons)
{
    const f = document.forms.tokenForm;
    if (!f) return;

    if (!commons.validate(f.mail, '메일 주소', REGEX.mail, MSG.mail)) return;

    const body = { userMail: commons.getValueEl(f.mail) };

    commons.showToast("메일 발송 중….");
    const res = await commons.postJson("/api/auth/join/resend", body,
    {
        toastOnSuccess: "회원가입 인증 메일을 재발송했습니다.",
        parseJson: true
    });

    if (!res) return;
}

// 계정 찾기
function findAccount(commons)
{
    const f = document.forms.findAccountForm;
    if (!f) return;

    if (!commons.validate(f.mail, '메일 주소', REGEX.mail, MSG.mail)) return;

    commons.showToast('메일을 발송했습니다.');
}

// 로그인
function login(commons)
{
    const f = document.forms.loginForm;
    if (!f) return;

    if (!commons.validate(f.id, 'ID', REGEX.id, MSG.id, 6, 20)) return;
    if (!commons.validate(f.pw, '비밀번호', REGEX.pw, MSG.pw, 8, 40)) return;

    commons.showToast('유효성 검사 통과!');
}

// 비밀번호 변경
function changePw(commons)
{
    const f = document.forms.changePwForm;
    if (!f) return;

    if (!commons.validate(f.oldPw, '기존 비밀번호', REGEX.pw, MSG.pw, 8, 40)) return;
    if (!commons.validate(f.newPw, '새 비밀번호', REGEX.pw, MSG.pw, 8, 40)) return;

    commons.showToast('비밀번호 변경 유효성 검사 통과!');
}

// 회원 탈퇴
function withdraw(commons)
{
    const f = document.forms.withdrawForm;
    if (!f) return;

    if (!commons.validate(f.pw, '비밀번호', REGEX.pw, MSG.pw, 8, 40)) return;

    commons.showToast('회원 탈퇴 메일을 발송했습니다.');
}


// 함수 등록
export function bindAuth(commons)
{
    window.togglePw = (formName, checkboxEl) => togglePw(formName, checkboxEl);
    if (document.forms?.joinForm) resetIdCheck();
    window.resetIdCheck = resetIdCheck;
    window.usingCheck = () => usingCheck(commons);
    
    window.join = () => join(commons);
    window.token = () => token(commons);
    window.login = () => login(commons);
    window.findAccount = () => findAccount(commons);
    window.changePw = () => changePw(commons);
    window.withdraw = () => withdraw(commons);
}
// 리다이렉트 토스트
async function handleRedirectToasts(commons)
{
    const params = new URLSearchParams(window.location.search);
    let handled = false;

    // 회원가입 인증 결과
    const joined = params.get('joined');
    if (joined === '1')
    {
        commons.showToast('회원가입이 완료됐습니다.');
        handled = true;
    }
    else if (joined === 'expired')
    {
        commons.showToast('링크가 만료됐거나 유효하지 않습니다. 다시 시도해주세요.');
        handled = true;
    }

    // 로그인/로그아웃
    const login = params.get('login');
    const logout = params.get('logout');

    if (login === '1')
    {
        const data = await commons.fetchJson('/api/auth/me', { method: 'GET' }, { parseJson: true });
        if (data && data.userName) commons.showToast(`${data.userName}님, 환영합니다.`);
        else commons.showToast('로그인 되었습니다.');
        handled = true;
    }
    else if (logout === '1')
    {
        commons.showToast('로그아웃 됐습니다.');
        handled = true;
    }

    // 계정 복구
    const reset = params.get('reset');
    if (reset === 'done')
    {
        commons.showToast('임시 비밀번호가 적용됐습니다.');
        handled = true;
    }
    else if (reset === 'expired')
    {
        commons.showToast('링크가 만료됐거나 유효하지 않습니다.');
        handled = true;
    }

    // 비밀번호 변경
    const pw = params.get('pw');
    if (pw === 'changed')
    {
        commons.showToast('비밀번호가 변경됐습니다. 다시 로그인해주세요.');
        handled = true;
    }

    // 회원 탈퇴
    const withdraw = params.get('withdraw');
    if (withdraw === 'done')
    {
        commons.showToast('회원 탈퇴가 완료되었습니다.');
        handled = true;
    }
    else if (withdraw === 'expired')
    {
        commons.showToast('링크가 만료됐거나 유효하지 않습니다.');
        handled = true;
    }

    // 처리한 경우에만 쿼리 제거
    if (handled) history.replaceState({}, '', window.location.pathname);
}

function toggleMoon()
{
    const week = document.getElementById('weekReport');
    const moon = document.getElementById('moonReport');
    const titleUnit = document.getElementById('reportTitleUnit');
    const btn = document.getElementById('reportToggleBtn');

    if (!week || !moon || !titleUnit || !btn) return;

    const moonVisible = moon.style.display !== 'none';

    if (moonVisible)
    {
        // 월간 -> 주간
        moon.style.display = 'none';
        week.style.display = 'block';
        titleUnit.textContent = '주간';
        btn.textContent = '월간 리포트 보기';
    }
    else
    {
        // 주간 -> 월간
        week.style.display = 'none';
        moon.style.display = 'block';
        titleUnit.textContent = '월간';
        btn.textContent = '주간 리포트 보기';
    }
}

// 바인딩
export async function bindMain(commons)
{
    await handleRedirectToasts(commons);
    window.toggleMoon = () => toggleMoon();
}
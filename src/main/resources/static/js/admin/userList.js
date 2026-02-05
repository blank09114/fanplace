const userListState = { page: 0, size: 10 };

// 회원정보 바인딩
async function bindUserListPage(commons, page)
{
    const wrap = document.getElementById('userList');
    const listEl = document.getElementById('userListCards');
    const pagerEl = document.getElementById('userListPagination');
    if (!wrap || !listEl || !pagerEl) return;

    const safePage = Math.max(0, page | 0);
    userListState.page = safePage;

    const params = new URLSearchParams();
    params.set('page', String(safePage));
    params.set('size', String(userListState.size));

    const data = await commons.fetchJson(
        `/api/admin/users?${params.toString()}`,
        { method: 'GET' },
        { parseJson: true, defaultErrorMessage: null }
    );

    if (!data) { wrap.style.display = 'none'; return; }

    const items = Array.isArray(data.content) ? data.content : [];
    const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
    const currentPage = Number.isFinite(data.number) ? data.number : safePage;

    // 목록 비었으면 섹션 숨김
    if (items.length === 0) { wrap.style.display = 'none'; return; }

    wrap.style.display = '';
    listEl.innerHTML = '';

    for (const it of items)
    {
        const userId = it.userId ?? it.id ?? '';
        const name = it.name ?? '';
        const mail = it.mail ?? '';
        const joinedAt = commons.formatDateTime(it.joinedAt || it.createdAt);

        const withdraw = !!it.withdraw;
        const blocked = !!it.blocked;

        let status = '정상';
        if (withdraw) status = '탈퇴';
        else if (blocked) status = '차단';

        const card = document.createElement('div');
        card.className = 'card widthFull flexColumn gapSm pdMd';

        card.innerHTML =
        `
            <div class="cardItem flexColumnMov gapXs">
                <p class="title2Text bold">ID</p>
                <p class="title2Text">${commons.escapeHtml(userId)}</p>
            </div>

            <div class="cardItem flexColumnMov gapXs">
                <p class="title2Text bold">닉네임</p>
                <p class="title2Text">${commons.escapeHtml(name)}</p>
            </div>

            <div class="cardItem flexColumnMov gapXs">
                <p class="title2Text bold">메일</p>
                <p class="title2Text">${commons.escapeHtml(mail)}</p>
            </div>

            <div class="cardItem flexColumnMov gapXs">
                <p class="title2Text bold">가입일</p>
                <p class="title2Text">${commons.escapeHtml(joinedAt)}</p>
            </div>

            <div class="cardItem flexColumnMov gapXs">
                <p class="title2Text bold">상태</p>
                <p class="title2Text">${commons.escapeHtml(status)}</p>
            </div>
        `;

        // 카드 클릭 시 회원정보 페이지로 이동
        card.style.cursor = 'pointer';
        card.addEventListener('click', () =>
        {
            if (!userId) return;
            window.location.href = `/user/${encodeURIComponent(userId)}`;
        });

        listEl.appendChild(card);
    }

    commons.renderPagination(pagerEl, currentPage, totalPages, (p) =>
    { bindUserListPage(commons, p); });
}

// 함수 등록
export function bindUserList(commons)
{
    if (!document.getElementById('userList')) return;
    bindUserListPage(commons, 0);
}
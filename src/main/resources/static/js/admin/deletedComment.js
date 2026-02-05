// 삭제된 댓글/대댓글
const deletedComment =
{
    state: { keyword: null, },

    async load(commons, page)
    {
        const listEl = document.getElementById('deletedCommentList');
        const pagerEl = document.getElementById('deletedCommentPagination');
        if (!listEl || !pagerEl) return;

        const safePage = Math.max(0, page | 0);

        const params = new URLSearchParams();
        params.set('page', String(safePage));

        let url = `/api/admin/deleted-comments?${params.toString()}`;

        if (this.state.keyword && this.state.keyword.trim())
        {
            params.set('q', this.state.keyword.trim());
            url = `/api/admin/deleted-comments/search?${params.toString()}`;
        }

        const data = await commons.fetchJson(url, { method: "GET" }, { parseJson: true });
        if (!data) return;

        const items = Array.isArray(data.content) ? data.content : [];
        const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
        const currentPage = Number.isFinite(data.number) ? data.number : safePage;

        this.renderList(commons, listEl, items);

        commons.renderPagination(
            pagerEl,
            currentPage,
            totalPages,
            (p) => this.load(commons, p),
            { scrollToTop: true }
        );
    },

    // 렌더링
    renderList(commons, listEl, items)
    {
        // 헤더 row는 유지
        const header = listEl.querySelector('.row');
        listEl.innerHTML = '';
        if (header) listEl.appendChild(header);

        if (!items.length)
        {
            const empty = document.createElement('div');
            empty.className = 'pdSm lightText textCenter';
            empty.textContent = this.state.keyword ? '검색 결과가 없습니다.' : '삭제된 댓글이 없습니다.';
            listEl.appendChild(empty);
            return;
        }

        for (const it of items)
        {
            // API DTO 필드명 기준
            const type = it.type ?? ''; // COMMENT / RECOMMENT
            const postId = it.postId;
            const boardId = it.boardId ?? '';
            const boardName = it.boardName ?? '게시판';
            const categoryName = it.categoryName ?? '';

            const authorName = it.authorName ?? '탈퇴 회원';
            const authorUserId = it.authorUserId ?? '';

            const content = it.content ?? '';
            const createdAt = it.createdAt ?? '';
            const deletedAt = it.deletedAt ?? '';
            const deletedReason = it.deletedReason ?? '본인 삭제';

            const postUrl = `/${encodeURIComponent(boardId)}/post/${postId}`;

            const rowA = document.createElement('a');
            rowA.className = 'row widthFull flex alignCenter';
            rowA.href = postUrl;

            const left = document.createElement('div');
            left.className = 'widthFull flexColumn';

            const meta = document.createElement('p');
            meta.className = 'text2 lightText';

            const cdt = commons.formatDateTime ? commons.formatDateTime(createdAt) : createdAt;
            const ddt = commons.formatDateTime ? commons.formatDateTime(deletedAt) : deletedAt;

            const typeLabel = (type === 'RECOMMENT') ? '대댓글' : '댓글';
            meta.textContent = `${boardName}${categoryName ? ` - ${categoryName}` : ''} | ${cdt} 작성 · ${ddt} 삭제됨(사유: ${deletedReason}) · ${typeLabel}`;

            const line = document.createElement('p');
            line.className = 'text1';
            line.textContent = `${authorName} - ${content}`;

            left.appendChild(meta);
            left.appendChild(line);

            const right = document.createElement('p');
            right.className = 'text1 textCenter date';
            right.textContent = ddt;

            rowA.appendChild(left);
            rowA.appendChild(right);

            listEl.appendChild(rowA);
        }
    }
};

// 삭제된 댓글 검색
function searchDeletedComment(commons)
{
    const f = document.forms?.deletedCommentSearch;
    if (!f) { commons.showToast('검색 폼을 찾을 수 없습니다.'); return; }

    const keywordEl = f.keyword;
    if (!keywordEl) { commons.showToast('검색 입력창을 찾을 수 없습니다.'); return; }

    // 빈 값이면 검색 해제
    const keyword = commons.getValueEl(keywordEl).trim();
    deletedComment.state.keyword = keyword ? keyword : null;

    deletedComment.load(commons, 0);
}

// 함수 등록
export function bindDeletedComment(commons)
{
    const listEl = document.getElementById('deletedCommentList');
    const pagerEl = document.getElementById('deletedCommentPagination');
    if (!listEl || !pagerEl) return;

    window.searchDeletedComment = () => searchDeletedComment(commons);

    const f = document.forms?.deletedCommentSearch;
    if (f)
    {
        f.addEventListener('submit', (e) =>
        {
            e.preventDefault();
            window.searchDeletedComment();
        });
    }

    deletedComment.load(commons, 0);
}
// 삭제된 글
const deletedPost =
{
    state: { keyword: null,  },

    async load(commons, page)
    {
        const listEl = document.getElementById('deletedPostList');
        const pagerEl = document.getElementById('deletedPostPagination');
        if (!listEl || !pagerEl) return;

        const safePage = Math.max(0, page | 0);

        const params = new URLSearchParams();
        params.set('page', String(safePage));

        let url = `/api/admin/deleted-posts?${params.toString()}`;

        // 검색 상태면 검색 API로
        if (this.state.keyword && this.state.keyword.trim())
        {
            params.set('q', this.state.keyword.trim());
            url = `/api/admin/deleted-posts/search?${params.toString()}`;
        }

        const data = await commons.fetchJson(url, { method: "GET" }, { parseJson: true });
        if (!data) return;

        const items = Array.isArray(data.content) ? data.content : [];
        const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
        const currentPage = Number.isFinite(data.number) ? data.number : safePage;

        this.renderDeletedPostList(commons, listEl, items);

        commons.renderPagination(
            pagerEl,
            currentPage,
            totalPages,
            (p) => this.load(commons, p),
            { scrollToTop: true }
        );
    },

    renderDeletedPostList(commons, listEl, items)
    {
        listEl.innerHTML = '';

        if (!items.length)
        {
            const empty = document.createElement('div');
            empty.className = 'pdSm lightText textCenter';
            empty.textContent = this.state.keyword
                ? '검색 결과가 없습니다.'
                : '삭제된 게시글이 없습니다.';
            listEl.appendChild(empty);
            return;
        }

        for (const it of items)
        {
            const postId = it.postId;
            const boardId = it.boardId ?? '';
            const boardName = it.boardName ?? '게시판';
            const title = it.title ?? '';
            const authorName = it.authorName ?? '';
            const authorUserId = it.authorUserId ?? '';
            const createdAt = it.createdAt ?? '';
            const deletedAt = it.deletedAt ?? '';

            const viewCount = it.viewCount ?? 0;
            const likeCount = it.likeCount ?? 0;
            const commentCount = it.commentCount ?? 0;

            const postUrl = `/${encodeURIComponent(boardId)}/post/${postId}`;

            const postDiv = document.createElement('div');
            postDiv.className = 'post widthFull pdSm';

            const topRow = document.createElement('div');
            topRow.className = 'flex alignCenter gapXs';

            const tabSpan = document.createElement('span');
            tabSpan.className = 'postTab textCenter';
            tabSpan.textContent = boardName;

            const titleA = document.createElement('a');
            titleA.className = 'text1';
            titleA.href = postUrl;
            titleA.textContent = title;

            topRow.appendChild(tabSpan);
            topRow.appendChild(titleA);

            const writerA = document.createElement('a');
            writerA.className = 'text1 writer';
            writerA.textContent = authorName;

            // 작성자 정보 없으면 링크 막기
            if (authorUserId)
            { writerA.href = `/user/${encodeURIComponent(authorUserId)}`; }
            else
            {
                writerA.href = 'javascript:void(0)';
                writerA.style.pointerEvents = 'none';
            }

            const metaP = document.createElement('p');
            metaP.className = 'text1 lightText';

            const cdt = commons.formatDateTime ? commons.formatDateTime(createdAt) : createdAt;
            const ddt = commons.formatDateTime ? commons.formatDateTime(deletedAt) : deletedAt;

            metaP.textContent = `작성 ${cdt} · 삭제 ${ddt} · 조회 ${viewCount} · 좋아요 ${likeCount} · 댓글 ${commentCount}`;

            postDiv.appendChild(topRow);
            postDiv.appendChild(writerA);
            postDiv.appendChild(metaP);

            listEl.appendChild(postDiv);
        }
    }
};

// 삭제된 글 검색
function searchDeletedPost(commons)
{
    const f = document.forms?.deletedPostSearch;
    if (!f) { commons.showToast('검색 폼을 찾을 수 없습니다.'); return; }

    const keywordEl = f.keyword;
    if (!keywordEl) { commons.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
    if (!commons.validate(keywordEl, '검색어')) return;

    const keyword = commons.getValueEl(keywordEl);

    deletedPost.state.keyword = keyword;
    deletedPost.load(commons, 0);
}

// 함수 등록
export function bindDeletedPost(commons)
{
    const listEl = document.getElementById('deletedPostList');
    const pagerEl = document.getElementById('deletedPostPagination');
    if (!listEl || !pagerEl) return;

    window.searchDeletedPost = () => searchDeletedPost(commons);

    // Enter로도 검색되게
    const f = document.forms?.deletedPostSearch;
    if (f)
    {
        f.addEventListener('submit', (e) =>
        {
            e.preventDefault();
            window.searchDeletedPost();
        });
    }

    deletedPost.load(commons, 0);
}
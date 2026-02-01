export const board =
{
    // 상태 모듈
    state:
    {
        boardId: null,
        page: 0,
        categoryId: null,
        hot: false,
        q: null,
        mode: null,
        univ: false,
    },

    // 초기화
    initBoardPage(commons)
    {
        const listEl = document.getElementById('postList');
        const pagerEl = document.getElementById('pagination');
        if (!listEl || !pagerEl) return;

        // 통합검색 여부)
        const section = document.querySelector('section');
        this.state.univ = (section?.dataset?.univSearch === 'true');

        // 통합 검색 페이지
        if (this.state.univ)
        {
            const params = new URLSearchParams(location.search);
            const q = (params.get('q') || '').trim();
            this.state.q = q || null;

            this.loadPage(commons, 0);
            return;
        }

        // 일반 게시판 페이지
        const tabs = document.querySelector('.tabs');
        if (!tabs) return;

        const boardId = tabs.dataset?.boardId;
        if (!boardId) return;
        this.state.boardId = boardId;

        // 탭 클릭 이벤트
        tabs.addEventListener('click', (e) =>
        {
            const tab = e.target.closest('.tab');
            if (!tab) return;

            tabs.querySelectorAll('.tab').forEach(t => t.classList.remove('now'));
            tab.classList.add('now');

            const type = tab.dataset.tabType;

            if (type === 'all')
            {
                this.state.hot = false;
                this.state.categoryId = null;
            }
            else if (type === 'hot')
            {
                this.state.hot = true;
                this.state.categoryId = null;
            }
            else if (type === 'category')
            {
                this.state.hot = false;
                this.state.categoryId = tab.dataset.categoryId || null;
            }

            this.loadPage(commons, 0);
        });

        // 초기 로딩
        const nowTab = tabs.querySelector('.tab.now') || tabs.querySelector('.tab[data-tab-type="all"]');
        if (nowTab)
        {
            const type = nowTab.dataset.tabType;
            this.state.hot = (type === 'hot');
            this.state.categoryId = (type === 'category') ? (nowTab.dataset.categoryId || null) : null;
        }

        this.loadPage(commons, 0);
    },

    // 페이지 로드
    async loadPage(commons, page)
    {
        const listEl = document.getElementById('postList');
        const pagerEl = document.getElementById('pagination');
        if (!listEl || !pagerEl) return;

        const safePage = Math.max(0, page | 0);
        this.state.page = safePage;

        let url;

        if (this.state.univ)
        {
            const params = new URLSearchParams();
            params.set('page', String(safePage));
            if (this.state.q) params.set('q', this.state.q);

            url = `/api/board/search/posts?${params.toString()}`;
        }
        else
        {
            const boardId = this.state.boardId;
            if (!boardId) return;

            const params = new URLSearchParams();
            params.set('page', String(safePage));
            if (this.state.categoryId) params.set('categoryId', this.state.categoryId);
            if (this.state.hot) params.set('hot', 'true');

            if (this.state.q)
            {
                params.set('q', this.state.q);
                url = `/api/board/${encodeURIComponent(boardId)}/posts/search?${params.toString()}`;
            }
            else { url = `/api/board/${encodeURIComponent(boardId)}/posts?${params.toString()}`; }
        }

        const data = await commons.fetchJson(url, { method: "GET" }, { parseJson: true });
        if (!data) return;

        const items = Array.isArray(data.content) ? data.content : [];
        const totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
        const currentPage = Number.isFinite(data.number) ? data.number : safePage;

        this.renderList(commons, listEl, this.state.boardId, items);
        this.renderPager(commons, pagerEl, currentPage, totalPages);
    },

    // 렌더링
    renderList(commons, listEl, boardId, items)
    {
        listEl.innerHTML = '';

        if (!items.length)
        {
            const empty = document.createElement('div');
            empty.className = 'pdSm lightText textCenter';
            empty.textContent = '게시글이 없습니다.';
            listEl.appendChild(empty);
            return;
        }

        for (const it of items)
        {
            const postId = it.postId;
            const title = it.title ?? '';
            const categoryName = it.categoryName ?? '';
            const itemBoardId = it.boardId ?? boardId;
            const itemBoardName = it.boardName ?? '';
            const authorName = it.authorName ?? '';
            const createdAt = it.createdAt ?? '';
            const viewCount = it.viewCount ?? 0;
            const likeCount = it.likeCount ?? 0;
            const commentCount = it.commentCount ?? 0;

            const postUrl = `/${encodeURIComponent(itemBoardId)}/post/${postId}`;

            const postDiv = document.createElement('div');
            postDiv.className = 'post widthFull pdSm';

            const topRow = document.createElement('div');
            topRow.className = 'flex alignCenter gapXs';

            const tabSpan = document.createElement('span');
            tabSpan.className = 'postTab textCenter';
            tabSpan.textContent = this.state.univ
            ? (itemBoardName || '게시판'): (categoryName || (this.state.hot ? '인기' : '전체'));

            const titleA = document.createElement('a');
            titleA.className = 'text1';
            titleA.href = postUrl;
            titleA.textContent = title;

            topRow.appendChild(tabSpan);
            topRow.appendChild(titleA);

            const writerA = document.createElement('a');
            writerA.className = 'text1 writer';
            writerA.textContent = authorName;
            writerA.href = `/user/${encodeURIComponent(it.authorUserId)}`;

            const metaP = document.createElement('p');
            metaP.className = 'text1 lightText';

            const dt = (commons.formatDateTime ? commons.formatDateTime(createdAt) : createdAt);

            metaP.textContent = `${dt} · 조회 ${viewCount} · 좋아요 ${likeCount} · 댓글 ${commentCount}`;

            postDiv.appendChild(topRow);
            postDiv.appendChild(writerA);
            postDiv.appendChild(metaP);

            listEl.appendChild(postDiv);
        }
    },

    // 페이지네이션
    renderPager(commons, pagerEl, currentPage, totalPages)
    {
        commons.renderPagination(pagerEl, currentPage, totalPages, (p) =>
        { this.loadPage(commons, p); });
    },

    // 게시판 내 검색
    searchBoard(commons)
    {
        const f = document.forms?.search;
        if (!f) { commons.showToast('검색 폼을 찾을 수 없습니다.'); return; }

        const keywordEl = f.keyword;
        if (!keywordEl) { commons.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(keywordEl, '검색어')) return;

        const keyword = commons.getValueEl(keywordEl);

        this.state.q = keyword;
        this.state.mode = null;

        this.loadPage(commons, 0);
    }
};
export const comment =
{
    state:
    {
        postId: null,
        page: 0,
        totalPages: 0,
        loaded: false,
        isAuth: false,
        isAdmin: false,

        // 삭제 컨텍스트
        deleteTarget: null,          // { type: 'comment'|'recomment', id }
        lastReloadStrategy: 'current' // 'last' | 'current'
    },

    // URL/DOM에서 postId 추출
    _getPostId()
    {
        const parts = (location.pathname || '').split('/').filter(Boolean);
        const idx = parts.indexOf('post');
        if (idx !== -1 && parts[idx + 1] && /^\d+$/.test(parts[idx + 1])) return parts[idx + 1];

        const likeBtn = document.querySelector('button.btn.like[data-post-id]');
        const pid = likeBtn?.dataset?.postId;
        if (pid && /^\d+$/.test(pid)) return pid;

        return null;
    },

    // 댓글 영역 초기화 + 첫 렌더(마지막 페이지)
    initPostComment(commons)
    {
        const section = document.querySelector('section');
        this.state.isAuth = (section?.dataset?.auth === 'true');
        this.state.isAdmin = (section?.dataset?.admin === 'true');

        const listEl = document.getElementById('commentList');
        const pagerEl = document.getElementById('commentPagination');
        if (!listEl || !pagerEl) return;

        this.state.postId = this._getPostId();
        if (!this.state.postId) return;

        // 삭제 모달 confirm은 "열 때만" 바인딩(충돌 방지)
        this._unbindDeleteModals();

        const params = new URLSearchParams(location.search);
        const cpRaw = params.get('cp');
        const cp = (cpRaw != null && /^\d+$/.test(cpRaw)) ? parseInt(cpRaw, 10) : null;

        if (cp != null) this.loadPage(commons, cp);
        else this._loadLastPage(commons);
    },

    // 마지막 페이지 기준으로 최초 로드
    async _loadLastPage(commons)
    {
        const postId = this.state.postId;
        if (!postId) return;

        const first = await commons.fetchJson(
            `/api/post/${encodeURIComponent(postId)}/comments?page=0`,
            { method: "GET" },
            { parseJson: true }
        );

        if (!first) return;

        const totalPages = Number.isFinite(first.totalPages) ? first.totalPages : 0;
        this.state.totalPages = totalPages;

        if (totalPages <= 1)
        {
            this._render(commons, first);
            this.state.loaded = true;
            return;
        }

        const lastPage = Math.max(0, totalPages - 1);
        await this.loadPage(commons, lastPage);
        this.state.loaded = true;
    },

    // 특정 페이지 로드
    async loadPage(commons, page)
    {
        const listEl = document.getElementById('commentList');
        const pagerEl = document.getElementById('commentPagination');
        if (!listEl || !pagerEl) return;

        const postId = this.state.postId;
        if (!postId) return;

        const safePage = Math.max(0, page | 0);
        this.state.page = safePage;

        const data = await commons.fetchJson(
            `/api/post/${encodeURIComponent(postId)}/comments?page=${safePage}`,
            { method: "GET" },
            { parseJson: true }
        );

        if (!data) return;

        this.state.totalPages = Number.isFinite(data.totalPages) ? data.totalPages : 0;
        this._render(commons, data);
    },

    // 댓글/대댓글 렌더링
    _render(commons, pageData)
    {
        const listEl = document.getElementById('commentList');
        const pagerEl = document.getElementById('commentPagination');
        if (!listEl || !pagerEl) return;

        const items = Array.isArray(pageData.content) ? pageData.content : [];
        const totalPages = Number.isFinite(pageData.totalPages) ? pageData.totalPages : 0;
        const currentPage = Number.isFinite(pageData.number) ? pageData.number : this.state.page;

        listEl.innerHTML = '';

        if (!items.length)
        {
            const empty = document.createElement('div');
            empty.className = 'pdSm lightText textCenter';
            empty.textContent = '댓글이 없습니다.';
            listEl.appendChild(empty);
        }
        else
        {
            for (const c of items)
            {
                listEl.appendChild(this._renderComment(commons, c));

                const recomments = Array.isArray(c.recomments) ? c.recomments : [];
                for (const r of recomments) listEl.appendChild(this._renderRecomment(commons, r));
            }
        }

        commons.renderPagination(pagerEl, currentPage, totalPages, (p) => this.loadPage(commons, p));

        const hashId = (location.hash || '').replace('#', '');
        if (hashId)
        {
            const el = document.getElementById(hashId);
            if (el) el.scrollIntoView({ block: 'start' });
        }
    },

    // 댓글 카드 렌더링
    _renderComment(commons, c)
    {
        const wrap = document.createElement('div');
        wrap.className = 'comment card pdXs';
        wrap.dataset.commentId = c.commentId;
        wrap.id = `comment-${c.commentId}`;

        // 삭제 배너
        if (c.deleted)
        {
            const banner = document.createElement('div');
            banner.className = 'deletedCard widthFull flex alignCenter';

            const p = document.createElement('p');
            p.className = 'text1 textCenter';

            const reason = c.deletedReasonDisplay ?? '본인 삭제';
            p.textContent = `삭제된 댓글입니다.(사유: ${reason})`;

            banner.appendChild(p);
            wrap.appendChild(banner);
        }

        const a = document.createElement('a');
        a.className = 'text1 bold';
        a.textContent = c.authorName ?? '익명';
        a.href = `/user/${encodeURIComponent(c.authorUserId)}`;

        const dt = document.createElement('p');
        dt.className = 'text2 lightText';
        dt.textContent = commons.formatDateTime ? commons.formatDateTime(c.createdAt) : (c.createdAt ?? '');

        const menu = document.createElement('div');
        menu.className = 'commentMenu flex justifyEnd gapXs';

        // 삭제된 댓글이면 버튼은 숨김
        if (!c.deleted)
        {
            // 로그인만: 답글 버튼 + 본인 삭제
            if (this.state.isAuth)
            {
                const replyBtn = document.createElement('button');
                replyBtn.className = 'btn teal';
                replyBtn.textContent = '답글';
                replyBtn.onclick = () => this.toggleRecommentForm(commons, replyBtn);
                menu.appendChild(replyBtn);

                if (c.mine)
                {
                    const delBtn = document.createElement('button');
                    delBtn.className = 'btn teal';
                    delBtn.textContent = '삭제';
                    delBtn.onclick = () =>
                    {
                        this._setDeleteTarget('comment', c.commentId, 'last');
                        this.openCommentDeleteModal(commons);
                    };
                    menu.appendChild(delBtn);
                }
            }

            // 관리자 삭제
            if (this.state.isAdmin)
            {
                const admBtn = document.createElement('button');
                admBtn.className = 'btn teal';
                admBtn.textContent = '삭제(관리자)';
                admBtn.onclick = () =>
                {
                    this._setDeleteTarget('comment', c.commentId, 'last');
                    this.openCommentAdminDeleteModal(commons);
                };
                menu.appendChild(admBtn);
            }
        }

        const body = document.createElement('p');
        body.className = 'text1';
        body.textContent = c.content ?? '';

        wrap.appendChild(a);
        wrap.appendChild(dt);
        wrap.appendChild(menu);
        wrap.appendChild(body);

        // 로그인만: 대댓글 폼
        if (this.state.isAuth && !c.deleted) wrap.appendChild(this._buildRecommentForm());

        return wrap;
    },

    // 대댓글 카드 렌더링
    _renderRecomment(commons, r)
    {
        const outer = document.createElement('div');
        outer.className = 'recomment flex gapXs';

        const line = document.createElement('div');
        line.className = 'recommentLine';

        const wrap = document.createElement('div');
        wrap.className = 'comment widthFull card pdXs';
        wrap.dataset.recommentId = r.recommentId;
        wrap.dataset.commentId = r.commentId;
        wrap.id = `recomment-${r.recommentId}`;

        // 삭제 배너(삭제된 대댓글이면 항상 표시)
        if (r.deleted)
        {
            const banner = document.createElement('div');
            banner.className = 'deletedCard widthFull flex alignCenter';

            const p = document.createElement('p');
            p.className = 'text1 textCenter';

            const reason = r.deletedReasonDisplay ?? '본인 삭제';
            p.textContent = `삭제된 댓글입니다.(사유: ${reason})`;

            banner.appendChild(p);
            wrap.appendChild(banner);
        }

        const a = document.createElement('a');
        a.className = 'text1 bold';
        a.textContent = r.authorName ?? '익명';
        a.href = `/user/${encodeURIComponent(r.authorUserId)}`;

        const dt = document.createElement('p');
        dt.className = 'text2 lightText';
        dt.textContent = commons.formatDateTime ? commons.formatDateTime(r.createdAt) : (r.createdAt ?? '');

        const menu = document.createElement('div');
        menu.className = 'commentMenu flex justifyEnd gapXs';

        // 삭제된 대댓글이면 버튼은 숨김
        if (!r.deleted)
        {
            // 로그인만: 언급 버튼 + 본인 삭제
            if (this.state.isAuth)
            {
                const mentionBtn = document.createElement('button');
                mentionBtn.className = 'btn teal';
                mentionBtn.textContent = '언급';
                mentionBtn.onclick = () => this.toggleRecommentForm(commons, mentionBtn);
                menu.appendChild(mentionBtn);

                if (r.mine)
                {
                    const delBtn = document.createElement('button');
                    delBtn.className = 'btn teal';
                    delBtn.textContent = '삭제';
                    delBtn.onclick = () =>
                    {
                        this._setDeleteTarget('recomment', r.recommentId, 'current');
                        this.openCommentDeleteModal(commons);
                    };
                    menu.appendChild(delBtn);
                }
            }

            // 관리자 삭제
            if (this.state.isAdmin)
            {
                const admBtn = document.createElement('button');
                admBtn.className = 'btn teal';
                admBtn.textContent = '삭제(관리자)';
                admBtn.onclick = () =>
                {
                    this._setDeleteTarget('recomment', r.recommentId, 'current');
                    this.openCommentAdminDeleteModal(commons);
                };
                menu.appendChild(admBtn);
            }
        }

        const body = document.createElement('p');
        body.className = 'text1';

        if (r.mentionName)
        {
            const mentionSpan = document.createElement('span');
            mentionSpan.className = 'mainColorText';
            mentionSpan.textContent = `@${r.mentionName} `;
            body.appendChild(mentionSpan);
        }

        body.appendChild(document.createTextNode(r.content ?? ''));

        wrap.appendChild(a);
        wrap.appendChild(dt);
        wrap.appendChild(menu);
        wrap.appendChild(body);

        if (this.state.isAuth && !r.deleted) wrap.appendChild(this._buildRecommentForm());

        outer.appendChild(line);
        outer.appendChild(wrap);
        return outer;
    },

    // 대댓글 작성 폼 생성
    _buildRecommentForm()
    {
        const form = document.createElement('form');
        form.name = 'recommentForm';
        form.className = 'commentForm flexColumn gapXs';
        form.style.display = 'none';

        const header = document.createElement('div');
        header.className = 'sectionHeader flex justifyBetween';

        const title = document.createElement('span');
        title.className = 'title2Text bold';
        title.textContent = '대댓글 작성하기';

        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn teal';
        btn.textContent = '등록';
        btn.setAttribute('onclick', 'subRecomment(this)'); // 전역 함수 유지

        header.appendChild(title);
        header.appendChild(btn);

        const ta = document.createElement('textarea');
        ta.name = 'content';
        ta.placeholder = '500자 이내, 등록 후 수정이 불가합니다.';

        form.appendChild(header);
        form.appendChild(ta);

        return form;
    },

    // 댓글 등록 API 호출 + UI 갱신(마지막 페이지)
    async subComment(commons)
    {
        const f = document.forms?.commentForm;
        if (!f) { commons.showToast('댓글 폼을 찾을 수 없습니다.'); return; }

        const contentEl = f.content;
        if (!contentEl) { commons.showToast('댓글 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(contentEl, '내용', null, '', 1, 500)) return;

        const content = commons.getValueEl(contentEl);

        const postId = this.state.postId || this._getPostId();
        if (!postId) { commons.showToast('게시글 정보를 찾을 수 없습니다.'); return; }

        const data = await commons.postJson(
            `/api/post/${encodeURIComponent(postId)}/comment`,
            { content },
            {
                defaultErrorMessage: "댓글 등록에 실패했습니다.",
                toastOnSuccess: "댓글이 등록됐습니다.",
                parseJson: true
            }
        );

        if (!data) return;

        contentEl.value = '';
        const newCommentId = data?.commentId || data?.id;
        if (newCommentId) location.hash = `comment-${newCommentId}`;
        await this._loadLastPage(commons);
    },

    // 대댓글 폼 토글(답글/언급 버튼)
    toggleRecommentForm(commons, btnEl)
    {
        const commentEl = btnEl?.closest?.('.comment');
        if (!commentEl) { commons.showToast('댓글 영역을 찾을 수 없습니다.'); return; }

        const formEl = commentEl.querySelector('form[name="recommentForm"]');
        if (!formEl) { commons.showToast('대댓글 폼을 찾을 수 없습니다.'); return; }

        const isOpen = (getComputedStyle(formEl).display !== 'none');

        formEl.style.display = isOpen ? 'none' : 'flex';
        btnEl.textContent = isOpen ? '답글' : '닫기';

        if (!isOpen) formEl.querySelector('textarea[name="content"]')?.focus();
    },

    // 대댓글 등록 API 호출 + UI 갱신(현재 페이지)
    async subRecomment(commons, btnEl)
    {
        const formEl = btnEl?.closest?.('form.commentForm');
        if (!formEl) { commons.showToast('대댓글 폼을 찾을 수 없습니다.'); return; }

        const contentEl = formEl.querySelector('textarea[name="content"]');
        if (!contentEl) { commons.showToast('대댓글 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(contentEl, '대댓글', null, '', 1, 500)) return;

        const content = commons.getValueEl(contentEl);

        // commentId 찾기
        let commentId = formEl.closest('.comment')?.dataset?.commentId;
        if (!commentId)
        {
            const parent = formEl.closest('.recomment')?.previousElementSibling?.closest?.('.comment');
            commentId = parent?.dataset?.commentId || null;
        }

        if (!commentId || !/^\d+$/.test(commentId))
        { commons.showToast('부모 댓글 정보를 찾을 수 없습니다.'); return; }

        const data = await commons.postJson(
            `/api/post/comment/${encodeURIComponent(commentId)}/recomment`,
            { content },
            {
                defaultErrorMessage: "대댓글 등록에 실패했습니다.",
                toastOnSuccess: "대댓글이 등록됐습니다.",
                parseJson: true
            }
        );

        if (!data) return;

        contentEl.value = '';
        formEl.style.display = 'none';

        const newRecommentId = data?.recommentId || data?.id;
        if (newRecommentId) location.hash = `recomment-${newRecommentId}`;

        await this.loadPage(commons, this.state.page);
    },

    // 삭제 모달 confirm 기존 핸들러 제거
    _unbindDeleteModals()
    {
        const delBtn = document.getElementById('deletedModal')?.querySelector('button.btn.teal');
        if (delBtn) delBtn.onclick = null;

        const admBtn = document.getElementById('adminDeletedModal')?.querySelector('button.btn.teal');
        if (admBtn) admBtn.onclick = null;
    },

    // 삭제 API URL 생성(일반/관리자 reason 포함)
    _buildDeleteUrl(type, id, reason = null)
    {
        const base = (type === 'comment')
            ? `/api/post/comment/${encodeURIComponent(id)}/delete`
            : `/api/post/recomment/${encodeURIComponent(id)}/delete`;

        if (reason == null) return base;
        return `${base}?reason=${encodeURIComponent(reason)}`;
    },

    // 삭제 후 UI 재호출(댓글=last, 대댓글=current)
    async _afterDeleteReload(commons)
    {
        if (this.state.lastReloadStrategy === 'last') await this._loadLastPage(commons);
        else await this.loadPage(commons, this.state.page);

        this.state.deleteTarget = null;
    },

    // 본인 삭제 모달 오픈 + confirm 바인딩
    openCommentDeleteModal(commons)
    {
        const t = this.state.deleteTarget;
        if (!t) { commons.showToast('삭제 대상이 없습니다.'); return; }

        const modal = document.getElementById('deletedModal');
        const confirmBtn = modal?.querySelector('button.btn.teal');
        if (!modal || !confirmBtn) { commons.showToast('삭제 모달을 찾을 수 없습니다.'); return; }

        confirmBtn.onclick = async () =>
        {
            const tt = this.state.deleteTarget;
            if (!tt) { commons.showToast('삭제 대상이 없습니다.'); return; }

            const ok = await commons.fetchJson(
                this._buildDeleteUrl(tt.type, tt.id),
                { method: "POST" },
                { defaultErrorMessage: "삭제에 실패했습니다.", parseJson: false }
            );

            if (ok === null) return;

            closeModal('deletedModal');
            await this._afterDeleteReload(commons);
        };

        openModal('deletedModal');
    },

    // 관리자 삭제 모달 오픈 + confirm 바인딩
    openCommentAdminDeleteModal(commons)
    {
        const t = this.state.deleteTarget;
        if (!t) { commons.showToast('삭제 대상이 없습니다.'); return; }

        const modal = document.getElementById('adminDeletedModal');
        const confirmBtn = modal?.querySelector('button.btn.teal');
        const reasonInput = modal?.querySelector('input[name="reason"]');

        if (!modal || !confirmBtn || !reasonInput)
        { commons.showToast('관리자 삭제 모달을 찾을 수 없습니다.'); return; }

        reasonInput.value = '';
        reasonInput.focus();

        confirmBtn.onclick = async () =>
        {
            const tt = this.state.deleteTarget;
            if (!tt) { commons.showToast('삭제 대상이 없습니다.'); return; }

            const reason = commons.getValueEl(reasonInput);
            if (!commons.validate(reasonInput, '삭제 사유')) return;

            const ok = await commons.fetchJson(
                this._buildDeleteUrl(tt.type, tt.id, reason),
                { method: "POST" },
                { defaultErrorMessage: "삭제에 실패했습니다.", parseJson: false }
            );

            if (ok === null) return;

            closeModal('adminDeletedModal');
            await this._afterDeleteReload(commons);
        };

        openModal('adminDeletedModal');
    },

    // 삭제 대상/리로드 전략 저장
    _setDeleteTarget(type, id, reloadStrategy = 'current')
    {
        this.state.deleteTarget = { type, id };
        this.state.lastReloadStrategy = reloadStrategy;
    },
};
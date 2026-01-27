export const board =
{
    // 게시판 내 검색
    searchBoard(commons)
    {
        const f = document.forms?.search;
        if (!f) { commons.showToast('검색 폼을 찾을 수 없습니다.'); return; }

        const keywordEl = f.keyword;
        const selectEl = f.querySelector('select');

        if (!keywordEl) { commons.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(keywordEl, '검색어')) return;

        const keyword = commons.getValueEl(keywordEl);
        const typeText = selectEl?.value ?? '제목';

        // 매핑
        const typeMap = { '제목': 'title', '제목+내용': 'titleContent', '내용': 'content' };
        const type = typeMap[typeText] ?? 'title';

        // TODO: 실제 검색 동작
        commons.showToast(`"${keyword}" (${typeText}) 검색`);
    },

    // 게시글 등록
    writePost(commons)
    {
        const f = document.forms?.writeForm;
        if (!f) { commons.showToast('작성 폼을 찾을 수 없습니다.'); return; }

        const tabEl = f.tab;
        const titleEl = f.title;
        const contentEl = f.content;

        if (!tabEl || !titleEl || !contentEl)
        { commons.showToast('작성 폼 입력 요소를 찾을 수 없습니다.'); return; }

        if (!commons.validate(titleEl, '제목', null, '', 0, 100)) return;
        if (!commons.validate(contentEl, '본문', null, '', 0, 0)) return;

        const title = commons.getValueEl(titleEl);
        const content = commons.getValueEl(contentEl);

        // TODO: 실제 등록

        commons.showToast('게시글이 등록됐습니다.');
    },

    // 댓글 등록
    subComment(commons)
    {
        const f = document.forms?.commentForm;
        if (!f) { commons.showToast('댓글 폼을 찾을 수 없습니다.'); return; }

        const contentEl = f.content;
        if (!contentEl) { commons.showToast('댓글 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(contentEl, '내용', null, '', 1, 500)) return;

        const content = commons.getValueEl(contentEl);

        // TODO: 실제 등록 처리
        commons.showToast('댓글이 등록됐습니다.');
    },
    
    // 대댓글 폼 토글
    toggleRecommentForm(commons, btnEl)
    {
        const commentEl = btnEl?.closest?.('.comment');
        if (!commentEl) { commons.showToast('댓글 영역을 찾을 수 없습니다.'); return; }

        const formEl = commentEl.querySelector('form[name="recommentForm"]');
        if (!formEl) { commons.showToast('대댓글 폼을 찾을 수 없습니다.'); return; }

        const isOpen = (getComputedStyle(formEl).display !== 'none');

        formEl.style.display = isOpen ? 'none' : 'flex';
        btnEl.textContent = isOpen ? '답글' : '닫기';

        if (!isOpen)
        { const textarea = formEl.querySelector('textarea[name="content"]'); textarea?.focus(); }
    },

    // 대댓글 등록
    subRecomment(commons, btnEl)
    {
        const formEl = btnEl?.closest?.('form.commentForm');
        if (!formEl) { commons.showToast('대댓글 폼을 찾을 수 없습니다.'); return; }

        const contentEl = formEl.querySelector('textarea[name="content"]');
        if (!contentEl) { commons.showToast('대댓글 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(contentEl, '대댓글', null, '', 1, 500)) return;

        const content = commons.getValueEl(contentEl);

        // TODO: 실제 등록 처리(fetch/ajax 등)
        commons.showToast('대댓글이 등록됐습니다.');

        contentEl.value = '';
        formEl.style.display = 'none';
        const commentEl = formEl.closest('.comment');
        const toggleBtn = commentEl?.querySelector('.commentMenu button[onclick^="toggleForm"]');
        if (toggleBtn) toggleBtn.textContent = '답글';
    }
};
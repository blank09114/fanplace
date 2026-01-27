// /js/admin.js
export const admin =
{
    // 주차 변경
    changeWeek(commons, direction)
    {
        const weekEl = document.getElementById('week');
        if (!weekEl) return;

        const format = (date) =>
        {
            const yy = date.getFullYear();
            const mm = String(date.getMonth() + 1).padStart(2, '0');
            const dd = String(date.getDate()).padStart(2, '0');
            return `${yy}.${mm}.${dd}.`;
        };

        let baseDate;
        const text = (weekEl.textContent ?? '').trim();
        const match = text.match(/(\d{4})\.(\d{2})\.(\d{2})\./);

        if (match)
        {
            const y = Number(match[1]);
            const m = Number(match[2]);
            const d = Number(match[3]);
            baseDate = new Date(y, m - 1, d);
        }
        else { baseDate = new Date(); }
        if (direction === 'prev') baseDate.setDate(baseDate.getDate() - 7);
        else if (direction === 'next') baseDate.setDate(baseDate.getDate() + 7);

        const day = baseDate.getDay();
        const diffToMonday = (day === 0) ? -6 : (1 - day);

        const monday = new Date(baseDate);
        monday.setDate(baseDate.getDate() + diffToMonday);

        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);

        weekEl.textContent = `${format(monday)} ~ ${format(sunday)}`;

        // TODO: monday~sunday 범위로 통계 재조회
    },

    // 삭제된 글 검색
    searchDeletedPost(commons)
    {
        const f = document.forms?.deletedPostSearch;
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
    
    // 삭제된 댓글 검색
    searchDeletedComment(commons)
    {
        const f = document.forms?.deletedCommentSearch;
        if (!f) { commons.showToast('검색 폼을 찾을 수 없습니다.'); return; }

        const keywordEl = f.keyword;

        if (!keywordEl) { commons.showToast('검색 입력창을 찾을 수 없습니다.'); return; }
        if (!commons.validate(keywordEl, '검색어')) return;

        const keyword = commons.getValueEl(keywordEl);

        // TODO: 실제 검색 동작
        commons.showToast(`"${keyword}" 검색`);
    }
};
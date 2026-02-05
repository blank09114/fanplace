// 삭제된 댓글 검색
function searchDeletedComment(commons)
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

// 함수 등록
export function bindDeletedComment(commons)
{
    // 이 페이지가 아니면 아무 것도 안 함
    window.searchDeletedComment = () => searchDeletedComment(commons);
}
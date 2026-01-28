import { commons } from '/js/commons.js';
import { bindAuth } from '/js/account/auth.js';
import { bindUser } from '/js/account/user.js';
import { board } from '/js/board/board.js';
import { post, richEditor } from '/js/board/post.js';
import { comment } from '/js/board/comment.js';
import { admin } from '/js/admin/admin.js';

// 전역 함수 등록: commons
window.toggleUi = commons.toggleUi;
window.toggleDrawer = commons.toggleDrawer;
window.scrollCtr = commons.scrollCtr;
window.showToast = commons.showToast.bind(commons);
window.closeToast = commons.closeToast.bind(commons);
window.openModal = commons.openModal.bind(commons);
window.closeModal = commons.closeModal.bind(commons);
window.getValueEl = commons.getValueEl.bind(commons);
window.searchUniv = commons.searchUniv.bind(commons);

// 전역 함수 등록: board
window.searchBoard = () => board.searchBoard(commons);
window.writePost = () => post.writePost(commons);
window.like = (btnEl) => post.like(commons, btnEl);
window.subComment = () => comment.subComment(commons);
window.toggleRecommentForm = (btnEl) => comment.toggleRecommentForm(commons, btnEl);
window.subRecomment = (btnEl) => comment.subRecomment(commons, btnEl);

// 전역 함수 등록: admin
window.changeWeek = (direction) => admin.changeWeek(commons, direction);
window.searchDeletedSearch = () => admin.searchDeletedPost(commons);
window.searchDeletedComment = () => admin.searchDeletedComment(commons);

// 이벤트 바인딩
document.addEventListener('DOMContentLoaded', () =>
{
    commons.applyTheme();
    bindAuth(commons);
    bindUser(commons);

    board.initBoardPage?.(commons);

    if (document.querySelector('textarea[data-tinymce="post"]')) { richEditor.initPostEditor(); }
    post.initLike(commons);
    const hasPostDelete = document.getElementById('postDeleteForm')
    || document.getElementById('postAdminDeleteForm')
    || document.getElementById('postDeletedReasonForm');
    if (hasPostDelete)
    {
        post.initPostDelete(commons);
        post.initDeletedReasonChange(commons);
    }
    const f = document.forms?.univSearch;
    if (f)
    { f.addEventListener('submit', (e) => { e.preventDefault(); commons.searchUniv(); }); }

    if (document.querySelector('[data-admin-page="1"]')) { admin.changeWeek(commons, 'current'); }

    // 인증/리다이렉트 토스트 처리
    const params = new URLSearchParams(window.location.search);
    const joined = params.get("joined");

    if (joined === "1")
    {
        commons.showToast("회원가입이 완료됐습니다.");
        history.replaceState({}, "", window.location.pathname);
    }
    if (joined === "expired")
    {
        commons.showToast("링크가 만료됐거나 유효하지 않습니다. 다시 시도해주세요.");
        history.replaceState({}, "", window.location.pathname);
    }

    // 로그인/로그아웃 토스트 처리
    const login = params.get("login");
    const logout = params.get("logout");

    if (login === "1")
    {
        commons.fetchJson("/api/auth/me", { method: "GET" }, { parseJson: true })
            .then(data =>
            {
                if (data && data.userName) commons.showToast(`${data.userName}님, 환영합니다.`);
                else commons.showToast("로그인 되었습니다.");
            })
            .finally(() =>
            { history.replaceState({}, "", window.location.pathname); });
    }
    if (logout === "1")
    {
        commons.showToast("로그아웃 됐습니다.");
        history.replaceState({}, "", window.location.pathname);
    }

    // 계정 복구 토스트 처리
    const reset = params.get("reset");

    if (reset === "done")
    {
        commons.showToast("임시 비밀번호가 적용됐습니다.");
        history.replaceState({}, "", window.location.pathname);
    }
    if (reset === "expired")
    {
        commons.showToast("링크가 만료됐거나 유효하지 않습니다.");
        history.replaceState({}, "", window.location.pathname);
    }

    // 비밀번호 변경 토스트 처리
    const pw = params.get("pw");

    if (pw === "changed")
    {
        commons.showToast("비밀번호가 변경됐습니다. 다시 로그인해주세요.");
        history.replaceState({}, "", window.location.pathname);
    }

    // 회원 탈퇴 토스트 처리
    const withdraw = params.get("withdraw");

    if (withdraw === "done")
    {
        commons.showToast("회원 탈퇴가 완료되었습니다.");
        history.replaceState({}, "", window.location.pathname);
    }
    if (withdraw === "expired")
    {
        commons.showToast("링크가 만료됐거나 유효하지 않습니다.");
        history.replaceState({}, "", window.location.pathname);
    }
});
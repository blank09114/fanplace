import { commons } from '/js/commons.js';
import { bindAuth } from '/js/auth.js';
import { bindUser } from '/js/user.js';
import { board } from '/js/board.js';
import { admin } from '/js/admin.js';

// 전역 함수 등록
window.toggleUi = commons.toggleUi;
window.toggleDrawer = commons.toggleDrawer;
window.scrollCtr = commons.scrollCtr;
window.showToast = commons.showToast.bind(commons);
window.closeToast = commons.closeToast.bind(commons);
window.openModal = commons.openModal.bind(commons);
window.closeModal = commons.closeModal.bind(commons);
window.getValueEl = commons.getValueEl.bind(commons);
window.searchUniv = commons.searchUniv.bind(commons);
window.searchBoard = () => board.searchBoard(commons);
window.writePost = () => board.writePost(commons);
window.subComment = () => board.subComment(commons);
window.toggleRecommentForm = (btnEl) => board.toggleRecommentForm(commons, btnEl);
window.subRecomment = (btnEl) => board.subRecomment(commons, btnEl);
window.changeWeek = (direction) => admin.changeWeek(commons, direction);
window.searchDeletedSearch = () => admin.searchDeletedPost(commons);
window.searchDeletedComment = () => admin.searchDeletedComment(commons);

// 이벤트 바인딩
document.addEventListener('DOMContentLoaded', () =>
{
    commons.applyTheme();
    bindAuth(commons);
    bindUser(commons);
    admin.changeWeek(commons, 'current');

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
});
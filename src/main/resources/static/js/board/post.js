export const post =
{
    // 게시글 등록/수정
    writePost(commons)
    {
        const f = document.forms?.writeForm;
        if (!f) { commons.showToast('폼을 찾을 수 없습니다.'); return; }

        const categoryEl = f.categoryId;
        const titleEl = f.title;
        const contentEl = f.content;

        if (!categoryEl || !titleEl || !contentEl)
        { commons.showToast('폼 입력 요소가 누락되었습니다.'); return; }

        if (window.tinymce) { window.tinymce.triggerSave(); }
        if (!commons.validate(titleEl, '제목', null, '', 1, 100)) return;
        if (!commons.validate(contentEl, '본문', null, '', 1, 0)) return;

        f.submit();
    },

    // 게시글 삭제
    initPostDelete(commons)
    {
        const delModal = document.getElementById('deletedModal');
        const adminModal = document.getElementById('adminDeletedModal');

        if (!delModal || !adminModal)
        { commons.showToast?.('삭제 모달을 찾을 수 없습니다.'); }
    },

    // 삭제 사유 변경
    initDeletedReasonChange(commons)
    {
        const modal = document.getElementById('deletedReasonModal');
        const form = document.getElementById('postDeletedReasonForm');
        const confirmBtn = modal?.querySelector('button.btn.teal');
        const reasonInput = modal?.querySelector('input[name="reason"]');
        const hiddenReason = form?.querySelector('input[name="reason"]');

        if (!modal || !form || !confirmBtn || !reasonInput || !hiddenReason) return;

        confirmBtn.onclick = () =>
        {
            const reason = commons.getValueEl(reasonInput);
            if (!commons.validate(reasonInput, '삭제 사유')) return;

            hiddenReason.value = reason;
            form.submit();
        };
    },

    // 좋아요 초기 세팅
    async initLike(commons)
    {
        const btnEl = document.querySelector('button.btn.like[data-post-id]');
        if (!btnEl) return;

        const postId = btnEl.dataset.postId;
        if (!postId) return;

        const data = await commons.fetchJson(`/api/post/${postId}/like`, { method: "GET" },
        {
            defaultErrorMessage: "좋아요 상태를 불러오지 못했습니다.",
            parseJson: true
        });

        if (!data) return;

        btnEl.textContent = data.liked ? '♥' : '♡';
    },

    // 좋아요 토글
    async like(commons, btnEl)
    {
        if (!btnEl) return;

        const postId = btnEl.dataset.postId;
        if (!postId) { commons.showToast('게시글 정보가 없습니다.'); return; }

        const isLiked = btnEl.textContent.trim() === '♥';
        const method = isLiked ? "DELETE" : "POST";

        const data = await commons.fetchJson(`/api/post/${postId}/like`, { method },
        {
            defaultErrorMessage: "좋아요 처리에 실패했습니다.",
            parseJson: true
        });

        if (!data) return;

        btnEl.textContent = data.liked ? '♥' : '♡';
        commons.showToast(data.liked ? "좋아요!" : "좋아요를 취소했습니다.");
    },

    // 게시글 삭제 모달 열기
    openPostDeleteModal(commons)
    {
        const delForm = document.getElementById('postDeleteForm');
        if (!delForm) { commons.showToast('삭제 폼을 찾을 수 없습니다.'); return; }

        // 공용 모달 confirm 교체 바인딩
        commons.bindModalConfirm('deletedModal', () => { delForm.submit(); });

        openModal('deletedModal');
    },

    // 게시글 관리자 삭제 모달 열기
    openPostAdminDeleteModal(commons)
    {
        const modal = document.getElementById('adminDeletedModal');
        const reasonInput = modal?.querySelector('input[name="reason"]');

        const form = document.getElementById('postAdminDeleteForm');
        const hiddenReason = form?.querySelector('input[name="reason"]');

        if (!modal || !reasonInput || !form || !hiddenReason)
        { commons.showToast('관리자 삭제 모달/폼을 찾을 수 없습니다.'); return; }

        commons.resetAdminDeleteModal();
        reasonInput.focus();

        // 공용 모달 confirm 교체 바인딩
        commons.bindModalConfirm('adminDeletedModal', () =>
        {
            const reason = commons.getValueEl(reasonInput);
            if (!commons.validate(reasonInput, '삭제 사유')) return;

            hiddenReason.value = reason;
            form.submit();
        });

        openModal('adminDeletedModal');
    },
};

// 에디터 초기화
export const richEditor =
{
    initPostEditor()
    {
        const textarea = document.querySelector('textarea[data-tinymce="post"]');
        if (!textarea || !window.tinymce) return;

        const id = textarea.id || (textarea.id = `editor_${Math.random().toString(36).slice(2, 10)}`);
        const height = parseInt(textarea.dataset.editorHeight || "600", 10);

        window.tinymce.init(this._options(`#${id}`, height));
    },

    _options(selector, height)
    {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';

        return {
            selector,
            height,

            menubar: false,
            branding: false,
            promotion: false,
            license_key: "gpl",

            base_url: "/vendor/tinymce",
            suffix: ".min",

            language: "ko_KR",

            skin: isDark ? "oxide-dark" : "oxide",
            content_css: isDark ? "dark" : "default",

            plugins: "lists link image table code codesample",
            toolbar: [
                "fontsize | bold italic underline strikethrough | superscript subscript | forecolor backcolor | alignleft aligncenter alignright alignjustify",
                "bullist numlist | hr | table | link image | removeformat | code"
            ].join(" | "),

            font_size_formats: "24px 20px 18px 16px 14px 12px",
            fontsize_default: "16px",
            content_style: "body { font-size: 16px; }",

            images_upload_handler: this._uploadImage,

            convert_urls: false,
            relative_urls: false,
            remove_script_host: false,
        };
    },

    _uploadImage(blobInfo, progress)
    {
        return new Promise((resolve, reject) =>
        {
            const xhr = new XMLHttpRequest();
            xhr.open("POST", "/api/upload");
            xhr.responseType = "json";
            xhr.withCredentials = true;

            xhr.upload.onprogress = (e) => { if (e.lengthComputable) progress((e.loaded / e.total) * 100); };

            xhr.onload = () =>
            {
                const res = xhr.response;
                if (xhr.status !== 200) return reject(res?.message || "이미지 업로드에 실패했습니다.");
                if (!res?.url) return reject("업로드 응답에 url이 없습니다.");
                resolve(res.url);
            };

            xhr.onerror = () => reject("네트워크 오류가 발생했습니다.");

            const fd = new FormData();
            fd.append("file", blobInfo.blob(), blobInfo.filename());
            xhr.send(fd);
        });
    }
};

// 바인딩
export function bindPost(commons)
{
    window.writePost = () => post.writePost(commons);
    window.like = (btnEl) => post.like(commons, btnEl);

    // 에디터가 있는 페이지에서만 초기화
    if (document.querySelector('textarea[data-tinymce="post"]')) { richEditor.initPostEditor(); }

    // 좋아요 버튼이 있는 페이지에서만 초기화(내부 가드 있음)
    post.initLike(commons);

    // 삭제 관련 요소가 있는 페이지에서만 초기화 + 전역 함수 등록
    const hasPostDelete = document.getElementById('postDeleteForm')
    || document.getElementById('postAdminDeleteForm')
    || document.getElementById('postDeletedReasonForm')
    || document.getElementById('deletedModal')
    || document.getElementById('adminDeletedModal')
    || document.getElementById('deletedReasonModal');

    if (hasPostDelete)
    {
        post.initPostDelete(commons);
        post.initDeletedReasonChange(commons);

        window.openPostDeleteModal = () => post.openPostDeleteModal(commons);
        window.openPostAdminDeleteModal = () => post.openPostAdminDeleteModal(commons);
    }
}
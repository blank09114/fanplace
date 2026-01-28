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

        // TODO: 실제 등록 처리
        commons.showToast('대댓글이 등록됐습니다.');

        contentEl.value = '';
        formEl.style.display = 'none';
        const commentEl = formEl.closest('.comment');
        const toggleBtn = commentEl?.querySelector('.commentMenu button[onclick^="toggleForm"]');
        if (toggleBtn) toggleBtn.textContent = '답글';
    }
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
            const form = document.forms?.writeForm;

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
export const dashboard =
{
    // 주차 변경
    async changeWeek(commons, direction)
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

        const formatISO = (date) =>
        {
            const yy = date.getFullYear();
            const mm = String(date.getMonth() + 1).padStart(2, '0');
            const dd = String(date.getDate()).padStart(2, '0');
            return `${yy}-${mm}-${dd}`;
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

        const weekStart = formatISO(monday);

        this.clearUI();
        await this.reloadAll(commons, weekStart);
    },

    async reloadAll(commons, weekStart)
    {
        const qs = `?weekStart=${encodeURIComponent(weekStart)}`;

        const [activity, boardStatus, quality, anomaly] = await Promise.all([
            commons.fetchJson(`/api/admin/dashboard/activity${qs}`, { method: "GET" }, { parseJson: true }),
            commons.fetchJson(`/api/admin/dashboard/board-status${qs}`, { method: "GET" }, { parseJson: true }),
            commons.fetchJson(`/api/admin/dashboard/conversation-quality${qs}`, { method: "GET" }, { parseJson: true }),
            commons.fetchJson(`/api/admin/dashboard/user-anomaly${qs}`, { method: "GET" }, { parseJson: true }),
        ]);

        if (!activity || !boardStatus || !quality || !anomaly)
        {
            commons.showToast("대시보드 데이터를 불러오지 못했습니다.");
            return;
        }

        this.bindActivity(activity);
        this.bindBoardStatus(boardStatus);
        this.bindConversationQuality(quality);
        this.bindUserAnomaly(anomaly);
    },

    // 초기화: 헤더 row 제외 싹 제거
    clearUI()
    {
        document.querySelectorAll('.pointColorText').forEach(el => { el.textContent = ''; });

        this.clearTable('activityTable');
        this.clearTable('boardStatusTable');
        this.clearTable('qualityTable');
    },

    clearTable(tableId)
    {
        const table = document.getElementById(tableId);
        if (!table) return;

        // 첫 번째 .flex(헤더)만 남기고 나머지 제거
        const rows = table.querySelectorAll(':scope > .flex');
        for (let i = rows.length - 1; i >= 1; i--) rows[i].remove();
    },

    // 활동 추이
    bindActivity(data)
    {
        const weekBox = document.querySelector('.grayCard');
        if (weekBox)
        {
            const nums = weekBox.querySelectorAll('.pointColorText');

            if (nums.length >= 3 && data.sum)
            {
                nums[0].textContent = this.num(data.sum.post);
                nums[1].textContent = this.num(data.sum.comment);
                nums[2].textContent = this.num(data.sum.user);
            }

            if (nums.length >= 6 && data.diff)
            {
                nums[3].textContent = this.pct(data.diff.postPct);
                nums[4].textContent = this.pct(data.diff.commentPct);
                nums[5].textContent = this.pct(data.diff.userPct);
            }
        }

        const table = document.getElementById('activityTable');
        if (!table) return;

        const days = Array.isArray(data.days) ? data.days : [];
        for (const d of days)
        {
            const row = this.makeRow([
                this.dateDot(d.date),
                this.num(d.post),
                this.num(d.comment),
                this.num(d.user),
            ]);
            table.appendChild(row);
        }
    },

    // 게시판 상태
    bindBoardStatus(data)
    {
        const table = document.getElementById('boardStatusTable');
        if (!table) return;

        const items = Array.isArray(data?.items) ? data.items : [];
        for (const it of items)
        {
            const row = this.makeRow([
                it.name ?? '',
                this.num(it.post),
                this.num(it.comment),
                this.fixed(it.cpr, 2),
            ]);
            table.appendChild(row);
        }
    },

    // 대화 품질
    bindConversationQuality(data)
    {
        const table = document.getElementById('qualityTable');
        if (!table) return;

        const items = Array.isArray(data?.items) ? data.items : [];
        for (const it of items)
        {
            const row = this.makeRow([
                it.name ?? '',
                this.num(it.noComment),
                this.num(it.authorOnly),
                this.fixed(it.avgUser, 2),
            ]);
            table.appendChild(row);
        }
    },

    // 유저 행동 이상
    bindUserAnomaly(data)
    {
        const section = this.findSectionByTitle('유저 행동 이상');
        if (!section) return;

        const card = section.querySelector('.card');
        if (!card) return;

        const nums = card.querySelectorAll('.pointColorText');
        if (nums.length < 3) return;

        nums[0].textContent = `${this.num(data.postOver10)}명`;
        nums[1].textContent = `${this.num(data.commentOver50)}명`;
        nums[2].textContent = this.pct(data.top1Pct);
    },

    // DOM 생성 유틸
    makeRow(values)
    {
        const row = document.createElement('div');
        row.className = 'flex gapXs';

        for (const v of values)
        {
            const sp = document.createElement('span');
            sp.className = 'text1 textCenter';
            sp.textContent = (v ?? '');
            row.appendChild(sp);
        }
        return row;
    },

    // 유틸
    findSectionByTitle(title)
    {
        const els = document.querySelectorAll('p.title2Text.bold');
        for (const p of els)
        {
            if ((p.textContent ?? '').trim() === title) return p.parentElement;
        }
        return null;
    },

    num(v)
    {
        const n = Number(v ?? 0);
        if (!Number.isFinite(n)) return '0';
        return String(Math.trunc(n));
    },

    fixed(v, digits)
    {
        const n = Number(v ?? 0);
        if (!Number.isFinite(n)) return '0';
        return n.toFixed(digits);
    },

    pct(v)
    {
        const n = Number(v ?? 0);
        if (!Number.isFinite(n)) return '0%';
        const sign = (n > 0) ? '+' : '';
        return `${sign}${Math.round(n)}%`;
    },

    dateDot(isoDate)
    {
        if (!isoDate) return '-';
        const s = String(isoDate);
        const m = s.match(/^(\d{4})-(\d{2})-(\d{2})/);
        if (!m) return s;
        return `${m[1]}.${m[2]}.${m[3]}.`;
    }
};

// 바인딩
export function bindDashboard(commons)
{
    if (!document.querySelector('[data-admin-dashboard="1"]')) return;

    window.changeWeek = (direction) => dashboard.changeWeek(commons, direction);

    dashboard.clearUI();
    dashboard.changeWeek(commons, 'current');
}
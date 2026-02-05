export const dashboard =
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
    }
};

// 바인딩
export function bindDashboard(commons)
{
    // 대시보드에서만 전역 등록
    if (!document.querySelector('[data-admin-dashboard="1"]')) return;

    window.changeWeek = (direction) => dashboard.changeWeek(commons, direction);

    // 초기 표시(현재 주차)
    dashboard.changeWeek(commons, 'current');
}
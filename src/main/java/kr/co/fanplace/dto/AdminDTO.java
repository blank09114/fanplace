package kr.co.fanplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

public class AdminDTO
{
    // 활동 추이
    @Getter
    @AllArgsConstructor
    public static class DashboardActivityRes
    {
        private final WeekSum sum;
        private final WeekDiff diff;
        private final List<DayRow> days;
    }

    @Getter
    @AllArgsConstructor
    public static class WeekSum
    {
        private final long post;
        private final long comment;
        private final long user;
    }

    @Getter
    @AllArgsConstructor
    public static class WeekDiff
    {
        private final double postPct;
        private final double commentPct;
        private final double userPct;
    }

    @Getter
    @AllArgsConstructor
    public static class DayRow
    {
        private final LocalDate date;
        private final long post;
        private final long comment;
        private final long user;
    }

    // 게시판 상태
    @Getter
    @AllArgsConstructor
    public static class DashboardBoardStatusRes
    { private final List<BoardRow> items; }

    @Getter
    @AllArgsConstructor
    public static class BoardRow
    {
        private final Long boardId;
        private final String name;
        private final long post;
        private final long comment;
        private final double cpr;
    }

    // 대화 품질
    @Getter
    @AllArgsConstructor
    public static class DashboardConversationQualityRes
    { private final List<QualityRow> items; }

    @Getter
    @AllArgsConstructor
    public static class QualityRow
    {
        private final Long boardId;
        private final String name;

        private final long noComment;
        private final long authorOnly;
        private final double avgUser;
    }

    // 유저 행동 이상
    @Getter
    @AllArgsConstructor
    public static class DashboardUserAnomalyRes
    {
        private final long postOver10;
        private final long commentOver50;
        private final double top1Pct;
        private final long top1User;
    }
}
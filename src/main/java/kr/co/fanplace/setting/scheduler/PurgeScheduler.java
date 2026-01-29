package kr.co.fanplace.setting.scheduler;

import kr.co.fanplace.service.board.PurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PurgeScheduler
{
    private final PurgeService purgeService;

    // 매일 새벽 4시
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeDaily()
    {
        // 글 purge
        while (true)
        {
            int purged = purgeService.purgeOnce(500);
            if (purged == 0) break;
        }

        // 댓글 purge
        while (true)
        {
            int purged = purgeService.purgeCommentsOnce(500);
            if (purged == 0) break;
        }

        // 대댓글 purge
        while (true)
        {
            int purged = purgeService.purgeRecommentsOnce(500);
            if (purged == 0) break;
        }
    }
}
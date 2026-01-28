package kr.co.fanplace.setting.scheduler;

import kr.co.fanplace.service.board.PostPurgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostPurgeScheduler
{
    private final PostPurgeService postPurgeService;

    // 매일 새벽 4시
    @Scheduled(cron = "0 0 4 * * *")
    public void purgeDaily()
    {
        // 한 번에 너무 많이 지우지 말고 batch로 여러 번
        int total = 0;
        while (true)
        {
            int purged = postPurgeService.purgeOnce(500);
            total += purged;
            if (purged == 0) break;
        }
    }
}
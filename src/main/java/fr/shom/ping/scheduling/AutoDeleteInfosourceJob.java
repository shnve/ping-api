package fr.shom.ping.scheduling;

import fr.shom.ping.service.InfosourceAdminService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class AutoDeleteInfosourceJob extends QuartzJobBean {
    @Autowired
    private InfosourceAdminService infosourceAdminService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        final UUID infosourceId = UUID.fromString(context.getJobDetail().getJobDataMap().getString("infosourceId"));
        log.info("AutoDeleteInfosourceJob: {}", infosourceId);
        infosourceAdminService.autoDelete(infosourceId);
    }
}

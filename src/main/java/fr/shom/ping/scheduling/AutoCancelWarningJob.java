package fr.shom.ping.scheduling;

import fr.shom.ping.exception.ServiceException;
import fr.shom.ping.service.WarningAdminService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class AutoCancelWarningJob extends QuartzJobBean {

    @Autowired
    private WarningAdminService warningAdminService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        final UUID warningId = UUID.fromString(context.getJobDetail().getJobDataMap().getString("warningId"));
        log.info("AutoCancelWarningJob: {}", warningId);
        try {
            this.warningAdminService.autoCancel(warningId);
        } catch (ServiceException e) {
            log.error("Failed to generate NAVTEX cancellation file", e);
        }
    }
}

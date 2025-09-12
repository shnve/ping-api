package fr.shom.ping.scheduling;

import fr.shom.ping.service.GeoregulationAdminService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class AutoUpdateGeoregulationExpiredStatusJob extends QuartzJobBean {

    public static final String GEOREGULATION_KEY_ID = "georegulationId";

    @Autowired
    private GeoregulationAdminService georegulationAdminService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        final UUID georegulationId = UUID.fromString(context.getJobDetail().getJobDataMap().getString(GEOREGULATION_KEY_ID));
        log.info("AutoUpdateGeoregulationExpiredStatusJob: {}", georegulationId);
        georegulationAdminService.autoUpdateGeoregulationExpiredStatus(georegulationId);
    }
}

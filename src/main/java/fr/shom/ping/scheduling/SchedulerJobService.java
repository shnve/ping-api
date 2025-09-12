package fr.shom.ping.scheduling;

import fr.shom.ping.model.FixedDateRange;
import fr.shom.ping.model.Georegulation;
import fr.shom.ping.model.GeoregulationPart;
import fr.shom.ping.model.Infosource;
import fr.shom.ping.model.Survey;
import fr.shom.ping.model.Warning;
import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.repository.GeoregulationRepository;
import fr.shom.ping.utils.SurveyUtil;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;

import static fr.shom.ping.scheduling.AutoDeleteSurveyJob.SURVEY_KEY_ID;
import static fr.shom.ping.scheduling.AutoDeleteSurveyJob.SURVEY_KEY_STATE;
import static fr.shom.ping.utils.GeoregulationUtil.getTargetDateForNewStatus;
import static fr.shom.ping.utils.InfosourceUtil.getDeletionDate;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerJobService {

    public static final String GEOREGULATION_GROUP = "georegulation-group";
    private final SchedulerFactoryBean schedulerFactoryBean;

    private final GeoregulationRepository georegulationRepository;

    public void scheduleAutoCancelWarningJob(Warning warning) {
        String jobName = "auto-cancel-" + warning.getId() + "-" + warning.getWarningNumber() + "-" + warning.getYear();
        String jobGroup = "warning-group";
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();

            JobDetail job = newJob(AutoCancelWarningJob.class)
                .withIdentity(jobName, jobGroup)
                .requestRecovery(true)
                .usingJobData("warningId", warning.getId().toString())
                .build();

            if (!scheduler.checkExists(job.getKey())) {

                Trigger trigger = newTrigger()
                    .withIdentity(jobName, jobGroup)
                    .startAt(Date.from(warning.getCancellationDate()))
                    .forJob(job.getKey())
                    .build();

                scheduler.scheduleJob(job, trigger);

                log.info(">>>>> jobName = [" + jobName + "]" + " scheduled.");
            } else {
                log.warn("scheduleAutoCancelWarningJob.jobAlreadyExist");
            }
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    public void scheduleAutoDeleteDraftInfosourceJob(Infosource infosource) {
        final String jobName = "auto-delete-" + infosource.getId();
        final String jobGroup = "infosource-group";
        try {
            final Scheduler scheduler = schedulerFactoryBean.getScheduler();

            final JobDetail job = newJob(AutoDeleteInfosourceJob.class)
                .withIdentity(jobName, jobGroup)
                .requestRecovery(true)
                .usingJobData("infosourceId", infosource.getId().toString())
                .build();

            removeScheduledAutoDeleteDraftInfosource(infosource);

            final Trigger trigger = newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(getDeletionDate(infosource)))
                .forJob(job.getKey())
                .build();

            scheduler.scheduleJob(job, trigger);

            log.info(">>>>> jobName = [" + jobName + "]" + " scheduled.");
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    public void removeScheduledAutoDeleteDraftInfosource(Infosource infosource) {
        final Scheduler scheduler = schedulerFactoryBean.getScheduler();
        final String jobName = "auto-delete-" + infosource.getId();
        final String jobGroup = "infosource-group";
        final JobDetail job = newJob(AutoDeleteInfosourceJob.class)
            .withIdentity(jobName, jobGroup)
            .requestRecovery(true)
            .usingJobData("infosourceId", infosource.getId().toString())
            .build();
        final JobKey jobKey = job.getKey();
        try {
            unscheduleJobIfExists(scheduler, jobKey, jobName, jobGroup);
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    public void scheduleAutoUpdateGeoregulationNewStatusJob(Georegulation georegulation) {
        final String jobName = "auto-update-isNew-" + georegulation.getId();
        final String jobGroup = GEOREGULATION_GROUP;
        try {
            final Scheduler scheduler = schedulerFactoryBean.getScheduler();

            final JobDetail job = newJob(AutoUpdateGeoregulationNewStatusJob.class)
                .withIdentity(jobName, jobGroup)
                .requestRecovery(true)
                .usingJobData(AutoUpdateGeoregulationNewStatusJob.GEOREGULATION_KEY_ID, georegulation.getId().toString())
                .build();

            unscheduleJobIfExists(scheduler, job.getKey(), jobName, jobGroup);

            final Date endOfNewStatusDate = Date.from(getTargetDateForNewStatus(georegulation));
            if (endOfNewStatusDate.before(new Date())) {
                georegulationRepository.updateNewStatusToFalse(georegulation.getId());
            } else {
                final Trigger trigger = newTrigger()
                    .withIdentity(jobName, jobGroup)
                    .startAt(endOfNewStatusDate)
                    .forJob(job.getKey())
                    .build();

                scheduler.scheduleJob(job, trigger);
                log.info(">>>>> jobName = [" + jobName + "]" + " scheduled.");
            }
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    public void scheduleAutoUpdateGeoregulationExpiredStatusJob(Georegulation georegulation) {
        if (!georegulation.isTemporary()) {
            return;
        }

        final String jobName = "auto-update-isExpired-" + georegulation.getId();
        final String jobGroup = GEOREGULATION_GROUP;
        try {
            final Scheduler scheduler = schedulerFactoryBean.getScheduler();

            final JobDetail job = newJob(AutoUpdateGeoregulationExpiredStatusJob.class)
                .withIdentity(jobName, jobGroup)
                .requestRecovery(true)
                .usingJobData(AutoUpdateGeoregulationExpiredStatusJob.GEOREGULATION_KEY_ID, georegulation.getId().toString())
                .build();

            unscheduleJobIfExists(scheduler, job.getKey(), jobName, jobGroup);

            final LocalDate expirationDate = getTargetDateForExpiredStatus(georegulation);
            if (expirationDate == null) {
                log.error("No target date for georegulation " + georegulation.getId());
            } else if (expirationDate.isBefore(LocalDate.now())) {
                georegulationRepository.updateExpiredStatus(georegulation.getId(), true);
            } else {
                georegulationRepository.updateExpiredStatus(georegulation.getId(), false);
                final Trigger trigger = newTrigger()
                    .withIdentity(jobName, jobGroup)
                    .startAt(Date.from(expirationDate.atStartOfDay().toInstant(ZoneOffset.UTC)))
                    .forJob(job.getKey())
                    .build();

                scheduler.scheduleJob(job, trigger);

                log.info(">>>>> jobName = [" + jobName + "]" + " scheduled.");
            }
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    /**
     * Remove the scheduled job for the georegulation if it exists or update the expired status to false and if the georegulation was before temporary and is now permanent (and still published)
     *
     * @param georegulation Georegulation
     */
    public void removeScheduleAutoUpdateGeoregulationExpiredStatusJob(Georegulation georegulation) {
        if (georegulation.isTemporary()) {
            return;
        }

        final String jobName = "auto-update-isExpired-" + georegulation.getId();
        final String jobGroup = GEOREGULATION_GROUP;
        try {
            final Scheduler scheduler = schedulerFactoryBean.getScheduler();

            final JobDetail job = newJob(AutoUpdateGeoregulationExpiredStatusJob.class)
                .withIdentity(jobName, jobGroup)
                .requestRecovery(true)
                .usingJobData(AutoUpdateGeoregulationExpiredStatusJob.GEOREGULATION_KEY_ID, georegulation.getId().toString())
                .build();

            unscheduleJobIfExists(scheduler, job.getKey(), jobName, jobGroup);
            georegulationRepository.updateExpiredStatus(georegulation.getId(), false);
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    private static void unscheduleJobIfExists(Scheduler scheduler, JobKey job, String jobName, String jobGroup) throws SchedulerException {
        if (!scheduler.checkExists(job)) { return; }
        scheduler.unscheduleJob(new TriggerKey(jobName, jobGroup));
        log.info(">>>>> jobName = [{}] deleted.", jobName);
    }

    public LocalDate getTargetDateForExpiredStatus(Georegulation georegulation) {
        Set<FixedDateRange> periods = new HashSet<>(georegulation.getCalendarPeriods());
        for (GeoregulationPart part : georegulation.getParts()) {
            periods.addAll(part.getCalendarPeriods());
        }
        LocalDate latestEndDate = null;
        if (!periods.isEmpty()) {
            latestEndDate = periods.stream().findFirst().get().getDateEnd();
            for (FixedDateRange period : periods) {
                if (period.getDateEnd() == null) {
                    latestEndDate = null;
                    break;
                } else if (period.getDateEnd().isAfter(latestEndDate)) {
                    latestEndDate = period.getDateEnd();
                }
            }
        }
        return latestEndDate;
    }

    public void scheduleAutoDeleteSurveyJob(Survey survey) {
        final String jobName = "auto-delete-" + survey.getId();
        final String jobGroup = "survey-group";
        try {
            final Scheduler scheduler = schedulerFactoryBean.getScheduler();

            final JobDetail job = newJob(AutoDeleteSurveyJob.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData(SURVEY_KEY_ID, survey.getId().toString())
                .usingJobData(SURVEY_KEY_STATE, survey.getState().toString())
                .build();

            removeScheduledAutoDeleteSurvey(survey);

            final Trigger trigger = newTrigger()
                .withIdentity(jobName, jobGroup)
                .startAt(Date.from(SurveyUtil.getDataDeletionDate(survey.getLastUpdate(), survey.getState() == SurveyState.TRANSMITTED)))
                .forJob(job.getKey())
                .build();

            scheduler.scheduleJob(job, trigger);

            log.info(">>>>> jobName = [" + jobName + "]" + " scheduled.");
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }

    public void removeScheduledAutoDeleteSurvey(Survey survey) {
        final Scheduler scheduler = schedulerFactoryBean.getScheduler();
        final String jobName = "auto-delete-" + survey.getId();
        final String jobGroup = "survey-group";
        final JobDetail job = newJob(AutoDeleteSurveyJob.class)
            .withIdentity(jobName, jobGroup)
            .usingJobData(SURVEY_KEY_ID, survey.getId().toString())
            .usingJobData(SURVEY_KEY_STATE, survey.getState().toString())
            .build();

        final JobKey jobKey = job.getKey();

        try {
            unscheduleJobIfExists(scheduler, jobKey, jobName, jobGroup);
        } catch (SchedulerException exception) {
            log.error(exception.getMessage(), exception);
        }
    }
}

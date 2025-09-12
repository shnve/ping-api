package fr.shom.ping.scheduling;

import fr.shom.ping.model.enums.SurveyState;
import fr.shom.ping.service.AbstractSurveyService;
import java.util.UUID;
import fr.shom.ping.service.SurveyService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class AutoDeleteSurveyJob extends QuartzJobBean {
    public static final String SURVEY_KEY_ID = "surveyId";

    public static final String SURVEY_KEY_STATE = "surveyState";

    @Autowired
    private SurveyService service;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        final UUID surveyId = UUID.fromString(context.getJobDetail().getJobDataMap().getString(SURVEY_KEY_ID));
        final SurveyState surveyState = SurveyState.valueOf(context.getJobDetail().getJobDataMap().getString(SURVEY_KEY_STATE));
        log.info("AutoDeleteSurveyJob: {}", surveyId);
        service.autoDelete(surveyId, surveyState != SurveyState.DRAFT);
    }
}

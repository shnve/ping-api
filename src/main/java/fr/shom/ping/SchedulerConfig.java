package fr.shom.ping;

import java.util.Properties;

import javax.sql.DataSource;

import fr.shom.ping.scheduling.SchedulerJobFactory;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.quartz.QuartzProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@Configuration
public class SchedulerConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QuartzProperties quartzProperties;

    @Bean
    @DependsOnDatabaseInitialization
    public SchedulerFactoryBean schedulerFactoryBean() {

        SchedulerJobFactory jobFactory = new SchedulerJobFactory();
        jobFactory.setApplicationContext(applicationContext);

        Properties properties = new Properties();
        properties.putAll(quartzProperties.getProperties());

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setOverwriteExistingJobs(true);
        factory.setAutoStartup(true);
        factory.setDataSource(dataSource);
        factory.setQuartzProperties(properties);
        factory.setJobFactory(jobFactory);
        return factory;
    }

    @Bean
    public LockProvider lockProvider() {
        return new JdbcTemplateLockProvider(dataSource);
    }
}

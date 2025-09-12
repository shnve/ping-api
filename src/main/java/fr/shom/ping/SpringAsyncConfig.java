package fr.shom.ping;

import fr.shom.ping.exception.AsyncExceptionHandler;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class SpringAsyncConfig extends AsyncConfigurerSupport {
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }
}

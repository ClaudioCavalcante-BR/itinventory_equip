package br.com.infnet.itinventory.search.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class SearchAsyncConfig {

    @Bean(name = "searchExecutor")
    public Executor searchExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("search-index-");
        ex.initialize();
        return ex;
    }
}
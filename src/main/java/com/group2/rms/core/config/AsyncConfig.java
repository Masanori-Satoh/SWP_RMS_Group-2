package com.group2.rms.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Luồng gửi email tuyển dụng chạy ngầm.
 * Tài khoản đăng nhập được chuyển sang luồng nền để audit log ghi đúng người thao tác.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String RECRUITMENT_MAIL_EXECUTOR = "recruitmentMailExecutor";

    @Bean
    public ThreadPoolTaskExecutor recruitmentMailThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("recruitment-mail-");
        // Hàng đợi đầy thì luồng gọi tự gửi: chậm hơn nhưng không mất thư và không lỗi sau khi lịch đã lưu
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean(RECRUITMENT_MAIL_EXECUTOR)
    public AsyncTaskExecutor recruitmentMailExecutor(ThreadPoolTaskExecutor recruitmentMailThreadPool) {
        return new DelegatingSecurityContextAsyncTaskExecutor(recruitmentMailThreadPool);
    }
}

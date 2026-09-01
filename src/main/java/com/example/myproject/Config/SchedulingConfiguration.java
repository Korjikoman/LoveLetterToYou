package com.example.myproject.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfiguration {
    
}
// 1. Запускается MyprojectApplication.main()
// 2. Spring создаёт OutboxBatchProcessor как @Service
// 3. Spring находит метод с @Scheduled
// 4. Регистрирует его во внутреннем планировщике
// 5. Планировщик автоматически вызывает processBatch()
// 6. После завершения ждёт OUTBOX_POLL_DELAY_MS мс
// 7. Вызывает снова
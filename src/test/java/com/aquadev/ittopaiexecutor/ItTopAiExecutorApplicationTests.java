package com.aquadev.ittopaiexecutor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"homework-execution", "homework-result"})
class ItTopAiExecutorApplicationTests {

    @Test
    void contextLoads() {
    }

}

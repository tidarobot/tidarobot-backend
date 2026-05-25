package org.uj.project.tidarobot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:boottest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.secret=SGVsbG9Xb3JsZEZvclRlc3RpbmdQdXJwb3Nlc09ubHkxMjM0",
        "encryption.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class TidaroBotApplicationTests {

    // LettuceConnectionFactory implements both RedisConnectionFactory and ReactiveRedisConnectionFactory
    @MockitoBean
    LettuceConnectionFactory lettuceConnectionFactory;

    @Test
    void contextLoads() {
    }
}

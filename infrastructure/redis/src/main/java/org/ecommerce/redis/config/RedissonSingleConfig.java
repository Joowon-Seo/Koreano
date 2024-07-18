package org.ecommerce.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RedissonSingleConfig {
	private final RedisSingleInfo redisSingleInfo;

	@Bean
	public RedissonClient redissonClient() {
		RedissonClient redisson = null;
		Config config = new Config();
		config.useSingleServer().setAddress(redisSingleInfo.getREDISSON_PREFIX() + redisSingleInfo.getHost() +
			":" + redisSingleInfo.getPort());
		redisson = Redisson.create(config);
		return redisson;
	}
}
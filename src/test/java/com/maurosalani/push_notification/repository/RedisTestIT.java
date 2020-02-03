package com.maurosalani.push_notification.repository;

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;
import org.junit.Test;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 *
 * Remember to download Redis and instantiate a container 
 * docker run --name redis -p 6379:6379 redis:5.0.6
 *
 * @author matteo
 */
public class RedisTestIT {

    private final String PORT = "6379";
    private final String URL = "redis://127.0.0.1:";

    @Test
    public void persist() {
        // 1. Create config object
        Config config = new Config();
        config.useSingleServer().setAddress(URL + PORT);
        // 2. Create Redisson instance
        RedissonClient redisson = Redisson.create(config);
        // 3. Get Redis based Map
        RMap<Long, String> myMap = redisson.getMap("myMap");
        myMap.put(1L, "hello");
        String mapValue = myMap.get(1L);
        assertThat(mapValue, equalTo("hello"));
    }
}

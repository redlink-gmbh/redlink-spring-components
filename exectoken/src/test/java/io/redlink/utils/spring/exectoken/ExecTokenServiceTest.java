/*
 * Copyright 2019 Redlink GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.redlink.utils.spring.exectoken;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import io.redlink.utils.spring.exectoken.repo.ExecTokenRepository;
import io.redlink.utils.test.testcontainers.MongoContainer;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration
@ContextConfiguration(classes = { ExecutionTokenService.class})
@EnableMongoRepositories(basePackageClasses = {ExecTokenRepository.class})
public class ExecTokenServiceTest {
    
    private final static Logger log = LoggerFactory.getLogger(ExecTokenServiceTest.class);
    
    @ClassRule
    public static final MongoContainer mongoContainer = new MongoContainer()
            .withDatabaseName("exectoken-test");

    @ClassRule
    public static final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @BeforeClass
    public static void setUpClass() {
        log.info("mongo URI: {}", mongoContainer.getConnectionUrl());
        environmentVariables.set("SPRING_DATA_MONGODB_URI", mongoContainer.getConnectionUrl());
    }
    
    @Autowired
    private ExecutionTokenService etService;

    @Test(expected=IllegalArgumentException.class)
    public void testTokenNameNull() throws Exception {
        etService.obtain(null, Duration.ofSeconds(1));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testTokenNameEmpty() throws Exception {
        etService.obtain("", Duration.ofSeconds(1));
    }

    @Test
    public void testSimpleExecToken() throws Exception{
        try (ExecutionToken et = etService.obtain("test")){
            assertExecutionToken(et, "test");
        }
        Assert.assertFalse(etService.isLocked("test"));
    }
    
    @Test
    public void testLockout() throws Exception{
        try (ExecutionToken et = etService.obtain("test", Duration.ofSeconds(1))){
            assertExecutionToken(et, "test");
        } //release on close
        Assert.assertFalse(etService.isLocked("test"));
        
        try (ExecutionToken et = etService.obtain("test", Duration.ofSeconds(1))){
            et.setReleaseOnClose(false);
            assertExecutionToken(et, "test");
        }
        //finally test that the lock expires after the 1sec
        TimeUnit.MILLISECONDS.sleep(1010);
        //NOTE the TTL Monitor runs every 60sec ... so we need to wait up to 1min to be sure
        log.info(" ... need wait up to 60sec for the Mongo TTL Index Monitor to run");
        long end = System.currentTimeMillis() + 60 * 1000;
        while(etService.isLocked("test") && System.currentTimeMillis() < end){
            TimeUnit.MILLISECONDS.sleep(500);
        }
        //now assert
        Assert.assertFalse(etService.isLocked("test"));
    }
    
    @Test(expected=ExecutionLockedException.class)
    public void testLocked() throws Exception {
        try (ExecutionToken et = etService.obtain("test", Duration.ofSeconds(1))){
            et.setReleaseOnClose(false);
            assertExecutionToken(et, "test");
        }
        etService.obtain("test", Duration.ofSeconds(1));
    }
    
    @Test
    public void testRenew() throws Exception{
        try (ExecutionToken et = etService.obtain("test", Duration.ofSeconds(1))){
            assertExecutionToken(et, "test");
            for(int i=0;i<5;i++) {
                TimeUnit.MILLISECONDS.sleep(800);
                Assert.assertTrue(etService.isLocked("test"));
                et.renew();
            }
        }
        //release on close is enabled
        Assert.assertFalse(etService.isLocked("test"));
    }

    private void assertExecutionToken(ExecutionToken et, String name) {
        Assert.assertNotNull(et);
        Assert.assertNotNull(et.getId());
        Assert.assertEquals(name, et.getName());
        Assert.assertNotNull(et.getDate());
        Assert.assertNotNull(et.getExpires());
        Assert.assertNotNull(et.getObtained());
    }
    
}

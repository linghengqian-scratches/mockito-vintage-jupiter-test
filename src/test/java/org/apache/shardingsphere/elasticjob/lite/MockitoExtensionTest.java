package org.apache.shardingsphere.elasticjob.lite;

import com.lingh.util.ReflectionUtils;
import org.apache.shardingsphere.elasticjob.api.JobConfiguration;
import org.apache.shardingsphere.elasticjob.infra.handler.sharding.JobInstance;
import org.apache.shardingsphere.elasticjob.lite.internal.config.ConfigurationService;
import org.apache.shardingsphere.elasticjob.lite.internal.election.LeaderService;
import org.apache.shardingsphere.elasticjob.lite.internal.failover.FailoverService;
import org.apache.shardingsphere.elasticjob.lite.internal.instance.InstanceService;
import org.apache.shardingsphere.elasticjob.lite.internal.schedule.JobRegistry;
import org.apache.shardingsphere.elasticjob.lite.internal.schedule.JobScheduleController;
import org.apache.shardingsphere.elasticjob.lite.internal.server.ServerService;
import org.apache.shardingsphere.elasticjob.lite.internal.sharding.ExecutionService;
import org.apache.shardingsphere.elasticjob.lite.internal.sharding.ShardingService;
import org.apache.shardingsphere.elasticjob.lite.internal.storage.JobNodeStorage;
import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockitoExtensionTest {
    @Test
    void assertGetAllFailoveringItems() {
        final FailoverService failoverService = new FailoverService(null, "test_job");
        JobNodeStorage jobNodeStorage = mock(JobNodeStorage.class);
        ShardingService shardingService = mock(ShardingService.class);
        ConfigurationService configService = mock(ConfigurationService.class);
        ReflectionUtils.setFieldValue(failoverService, "jobNodeStorage", jobNodeStorage);
        ReflectionUtils.setFieldValue(failoverService, "shardingService", shardingService);
        ReflectionUtils.setFieldValue(failoverService, "jobName", "test_job");
        ReflectionUtils.setFieldValue(failoverService, "configService", configService);
        JobRegistry.getInstance().addJobInstance("test_job", new JobInstance("127.0.0.1@-@0"));
        when(configService.load(true)).thenReturn(JobConfiguration.newBuilder("test_job", 3).build());
        String jobInstanceId = "127.0.0.1@-@1";
        when(jobNodeStorage.getJobNodeData("sharding/0/failovering")).thenReturn(jobInstanceId);
        lenient().when(jobNodeStorage.getJobNodeData("sharding/2/failovering")).thenReturn(jobInstanceId);
        Map<Integer, JobInstance> actual = failoverService.getAllFailoveringItems();
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0), is(new JobInstance(jobInstanceId)));
        assertThat(actual.get(2), is(new JobInstance(jobInstanceId)));
    }

    @Test
    void assertGetAllRunningItems() {
        final ExecutionService executionService = new ExecutionService(null, "test_job");
        JobNodeStorage jobNodeStorage = mock(JobNodeStorage.class);
        ConfigurationService configService = mock(ConfigurationService.class);
        ReflectionUtils.setFieldValue(executionService, "jobNodeStorage", jobNodeStorage);
        ReflectionUtils.setFieldValue(executionService, "configService", configService);
        when(configService.load(true)).thenReturn(JobConfiguration.newBuilder("test_job", 3).build());
        String jobInstanceId = "127.0.0.1@-@1";
        when(jobNodeStorage.getJobNodeData("sharding/0/running")).thenReturn(jobInstanceId);
        lenient().when(jobNodeStorage.getJobNodeData("sharding/2/running")).thenReturn(jobInstanceId);
        Map<Integer, JobInstance> actual = executionService.getAllRunningItems();
        assertThat(actual.size(), is(2));
        assertThat(actual.get(0), is(new JobInstance(jobInstanceId)));
        assertThat(actual.get(2), is(new JobInstance(jobInstanceId)));
        JobRegistry.getInstance().shutdown("test_job");
    }

    @Test
    void assertGetCrashedShardingItemsWithEnabledServer() {
        final ShardingService shardingService = new ShardingService(null, "test_job");
        CoordinatorRegistryCenter regCenter = mock(CoordinatorRegistryCenter.class);
        JobScheduleController jobScheduleController = mock(JobScheduleController.class);
        JobNodeStorage jobNodeStorage = mock(JobNodeStorage.class);
        LeaderService leaderService = mock(LeaderService.class);
        ConfigurationService configService = mock(ConfigurationService.class);
        ExecutionService executionService = mock(ExecutionService.class);
        ServerService serverService = mock(ServerService.class);
        InstanceService instanceService = mock(InstanceService.class);
        ReflectionUtils.setFieldValue(shardingService, "jobNodeStorage", jobNodeStorage);
        ReflectionUtils.setFieldValue(shardingService, "leaderService", leaderService);
        ReflectionUtils.setFieldValue(shardingService, "configService", configService);
        ReflectionUtils.setFieldValue(shardingService, "executionService", executionService);
        ReflectionUtils.setFieldValue(shardingService, "instanceService", instanceService);
        ReflectionUtils.setFieldValue(shardingService, "serverService", serverService);
        JobRegistry.getInstance().addJobInstance("test_job", new JobInstance("127.0.0.1@-@0", null, "127.0.0.1"));
        JobRegistry.getInstance().registerRegistryCenter("test_job", regCenter);
        JobRegistry.getInstance().registerJob("test_job", jobScheduleController);
        when(serverService.isEnableServer("127.0.0.1")).thenReturn(true);
        when(configService.load(true)).thenReturn(JobConfiguration.newBuilder("test_job", 3).cron("0/1 * * * * ?").build());
        when(jobNodeStorage.getJobNodeData("sharding/0/instance")).thenReturn("127.0.0.1@-@0");
        when(jobNodeStorage.isJobNodeExisted("sharding/0/running")).thenReturn(true);
        when(jobNodeStorage.getJobNodeData("sharding/2/instance")).thenReturn("127.0.0.1@-@0");
        lenient().when(jobNodeStorage.isJobNodeExisted("sharding/2/running")).thenReturn(true);
        assertThat(shardingService.getCrashedShardingItems("127.0.0.1@-@0"), is(Arrays.asList(0, 2)));
        JobRegistry.getInstance().shutdown("test_job");
    }
}

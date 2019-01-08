/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
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
 * </p>
 */

package io.shardingsphere.transaction.xa.handler;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.constant.PoolType;
import io.shardingsphere.transaction.api.TransactionType;
import io.shardingsphere.transaction.core.manager.ShardingTransactionManager;
import io.shardingsphere.transaction.spi.xa.XATransactionManager;
import io.shardingsphere.transaction.xa.fixture.DataSourceUtils;
import io.shardingsphere.transaction.xa.jta.datasource.ShardingXADataSource;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class XAShardingTransactionHandlerTest {
    
    private XAShardingTransactionHandler xaShardingTransactionHandler = new XAShardingTransactionHandler();
    
    @Mock
    private XATransactionManager xaTransactionManager;
    
    @Before
    public void setUp() {
        setMockXATransactionManager(xaShardingTransactionHandler, xaTransactionManager);
    }
    
    @SneakyThrows
    private void setMockXATransactionManager(final XAShardingTransactionHandler xaShardingTransactionHandler, final XATransactionManager xaTransactionManager) {
        Field field = xaShardingTransactionHandler.getClass().getDeclaredField("xaTransactionManager");
        field.setAccessible(true);
        field.set(xaShardingTransactionHandler, xaTransactionManager);
    }
    
    @Test
    public void assertGetTransactionManager() {
        ShardingTransactionManager shardingTransactionManager = xaShardingTransactionHandler.getShardingTransactionManager();
        assertThat(shardingTransactionManager, instanceOf(XATransactionManager.class));
    }
    
    @Test
    public void assertGetTransactionType() {
        assertThat(xaShardingTransactionHandler.getTransactionType(), is(TransactionType.XA));
    }
    
    @Test
    public void assertRegisterXATransactionalDataSource() {
        Map<String, DataSource> dataSourceMap = createDataSourceMap(PoolType.DRUID_XA, DatabaseType.MySQL);
        xaShardingTransactionHandler.registerTransactionalResource(DatabaseType.MySQL, dataSourceMap);
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            verify(xaTransactionManager).registerRecoveryResource(entry.getKey(), (XADataSource) entry.getValue());
        }
    }
    
    @Test
    public void assertRegisterAtomikosDataSourceBean() {
        Map<String, DataSource> dataSourceMap = createAtomikosDataSourceBeanMap();
        xaShardingTransactionHandler.registerTransactionalResource(DatabaseType.MySQL, dataSourceMap);
        verify(xaTransactionManager, times(0)).registerRecoveryResource(anyString(), any(XADataSource.class));
    }
    
    @Test
    public void assertRegisterNoneXATransactionalDAtaSource() {
        Map<String, DataSource> dataSourceMap = createDataSourceMap(PoolType.HIKARI, DatabaseType.MySQL);
        xaShardingTransactionHandler.registerTransactionalResource(DatabaseType.MySQL, dataSourceMap);
        Map<String, ShardingXADataSource> cachedXADatasourceMap = getCachedShardingXADataSourceMap();
        assertThat(cachedXADatasourceMap.size(), is(2));
    }
    
    @Test
    public void assertCreateNoneTransactionalConnection() {
    }
    
    @Test
    public void assertCreateXATransactionalConnection() {
    
    }
    
    @Test
    public void assertCreateNoneXATransactionalConnection() {
    
    }
    
    @SneakyThrows
    @SuppressWarnings("unchecked")
    private Map<String, ShardingXADataSource> getCachedShardingXADataSourceMap() {
        Field field = xaShardingTransactionHandler.getClass().getDeclaredField("cachedShardingXADataSourceMap");
        field.setAccessible(true);
        return (Map<String, ShardingXADataSource>) field.get(xaShardingTransactionHandler);
    }
    
    private Map<String, DataSource> createDataSourceMap(final PoolType poolType, final DatabaseType databaseType) {
        Map<String, DataSource> result = new HashMap<>();
        result.put("ds1", DataSourceUtils.build(poolType, databaseType, "demo_ds_1"));
        result.put("ds2", DataSourceUtils.build(poolType, databaseType, "demo_ds_2"));
        return result;
    }
    
    private Map<String, DataSource> createAtomikosDataSourceBeanMap() {
        Map<String, DataSource> result = new HashMap<>();
        result.put("ds1", new AtomikosDataSourceBean());
        result.put("ds2", new AtomikosDataSourceBean());
        return result;
    }
}

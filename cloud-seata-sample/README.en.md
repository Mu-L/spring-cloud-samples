## Seata Distributed Transaction Sample

### Project Overview

This module demonstrates how to integrate distributed transactions using Spring Cloud Alibaba Seata, consisting of the following four microservices:

| Service          | Port  | Description                                                                 |
|------------------|-------|-----------------------------------------------------------------------------|
| business-service | 18081 | Business service (entry point), calls downstream services via RestTemplate / FeignClient |
| storage-service  | 18082 | Storage service, responsible for deducting product inventory                |
| order-service    | 18083 | Order service, responsible for creating orders and calling the account service |
| account-service  | 18084 | Account service, responsible for deducting user balance                     |

### Prerequisites

#### 1. Initialize Database

Create a database named `seata` in MySQL, then execute the `all.sql` script to create the following tables:

- **Seata Base Tables**: `undo_log`, `global_table`, `branch_table`, `lock_table`, `distributed_lock`
- **Business Tables**: `storage_tbl`, `order_tbl`, `account_tbl`

To modify database connection information, edit the following configuration in each service's `resources/application.yml`:

```yaml
base:
  config:
    mdb:
      hostname: 127.0.0.1
      dbname: seata
      port: 3306
      username: 'root'
      password: 'root'
```

#### 2. Start Nacos

Ensure Nacos is running at `127.0.0.1:8848`.

#### 3. Configure Nacos (Seata Configuration Center)

Create a configuration in Nacos:

- **Data ID**: `seata.properties`
- **Group**: `SEATA_GROUP`
- **Configuration Format**: Properties

Configuration content:

```properties
service.vgroupMapping.default_tx_group=default
service.vgroupMapping.order-service-tx-group=default
service.vgroupMapping.account-service-tx-group=default
service.vgroupMapping.business-service-tx-group=default
service.vgroupMapping.storage-service-tx-group=default
```

#### 4. Start Seata Server
The `nacos-client` dependency bundled in the downloaded binary is version 1.4.6, which is too outdated, while the local version is 3.2.1. Therefore, building from source is required to start the server.
```text
git clone https://github.com/apache/incubator-seata
Modify the application.yml in the server module as shown in the YAML configuration below
Build the entire project, then start the ServerApplication in the server module
```

```yaml
seata:
  config:
    type: nacos 
    nacos:
      server-addr: 127.0.0.1:8848
      username: 'nacos'
      password: '7fDJZBbiLzO2'
      group: SEATA_GROUP
      namespace: public
      data-id: seata.properties
  registry:
    type: nacos
    nacos:
      application: seata-server
      group: SEATA_GROUP
      namespace: public
      cluster: default
      server-addr: 127.0.0.1:8848
      username: 'nacos'
      password: '7fDJZBbiLzO2'
```

### Running the Sample

Start the main classes of the following four applications in order:

1. `StorageApplication` (storage-service)
2. `AccountApplication` (account-service)
3. `OrderApplication` (order-service)
4. `BusinessApplication` (business-service)

After all services are started, access the following endpoints via GET requests:

Via RestTemplate
```shell
curl http://127.0.0.1:18081/seata/rest
```
Via FeignClient
```shell
curl http://127.0.0.1:18081/seata/feign
```

Response description:
- **SUCCESS**: Call succeeded
- **500 Exception**: Random mock exception in business-service (used to verify transaction rollback)

### Verifying Distributed Transactions

#### Xid Propagation

Check the console logs of each service to confirm that all services output the same Xid for a single request:

```
Storage Service Begin ... xid: 192.168.x.x:8091:xxxx
Order Service Begin ... xid: 192.168.x.x:8091:xxxx
Account Service Begin ... xid: 192.168.x.x:8091:xxxx
```

#### Data Consistency

`OrderService` and `AccountService` randomly throw exceptions via `Random.nextBoolean()` to simulate failure scenarios. If distributed transactions work correctly, the following equations should always hold:

- **User Balance**: `10000 = current balance + 2(unit price) × order count × 2(quantity per order)`
- **Inventory**: `100 = current stock + order count × 2(quantity per order)`

```sql
SELECT * FROM account_tbl;
SELECT * FROM storage_tbl;
SELECT * FROM order_tbl;
```

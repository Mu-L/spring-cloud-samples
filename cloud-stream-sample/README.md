### 演示
下载RocketMQ官方包<br>
启动 name server
```shell
bin/mqnamesrv
```
启动 broker
```shell
bin/mqbroker -n localhost:9876
```
发送消息
```shell
curl 'http://localhost:8769/send?message=hello'
```

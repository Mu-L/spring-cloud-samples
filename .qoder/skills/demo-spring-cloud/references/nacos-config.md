# ⚙️ Nacos Config 动态配置

启动 `cloud-nacos-config-sample`（端口 8761），通过模块提供的接口管理配置（避免直接调用 Nacos API 的鉴权问题）：
```shell
# 发布配置
curl 'http://localhost:8761/nacos/publishConfig?dataId=my.city&content=wuhan'
# 获取配置
curl 'http://localhost:8761/nacos/getConfig?dataId=my.city'
```

模块还演示了 `@NacosConfig`、`@ConfigurationProperties`、`@Value` + `@RefreshScope` 三种配置绑定方式，详细演示步骤参考 [SKILL.md](../SKILL.md) 中的 Nacos Config 章节。

### 分布式锁组件
 
 支持Redis、Zookeeper 实现
 
- Redis锁
 
    操作一定时间内没有释放锁，自动释放。

- zookeeper锁

    分布式，没有实现自动释放功能。

### Maven坐标

目前没有发布到中央仓库，deploy 到自己的仓库或者install。



```
    <dependency>
        <groupId>com.wuweibi</groupId>
        <artifactId>common-lock</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
```


### 调用方式

见单元测试类

### 集成Spring Boot

```
/**
 *  分布式锁组件
 */
@Bean
public LockHandler beanLockHandler() {
    LockHandler bean = new ZookeeperLockHandler();
    return bean;
}
```


### 使用方式

使用了锁一定要返回前释放

```
String key = "test";
boolean status = handler.tryLock(key, 10, TimeUnit.SECONDS);
if (!status) { // 锁没有被释放会等10秒，10秒内获取不到status=false
    System.out.println("请稍后再说！");
}

try {
    System.out.println("业务逻辑处理");
    
} catch (Exception e) {
    log.error("", e);
} finally {
    handler.unLock(key);
}
```
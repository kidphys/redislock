redislock
=========

This simple project allow you to execute a transaction in a synchronized manner by point all servers to a center Redis server which acts as a lock provider. Only 1 transaction who acquire a lock can proceed.

Although Redis implemention is provided, it's not necessary to use Redis.

This project depends on Jedis to communicate with Redis.

Basic example:

```java
JedisPool pool = new JedisPool(hostAddr, port);
RedisLockServer server = new RedisLockServer(pool);
// all lock with same ID can only be acquired once at a time
DistributedLock lock = new DistributedLock(server, "lockID");
SynchronizedTransaction trans = new SynchronizedTransaction(server);
trans.execute(new TransactionExecutable(){
  // your synchronized code
});
```



package redis.concurrency;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DistributedLock implements TransactionLock {

	private static final long MAX_RETRY_ATTEMPT = 5;
	private LockServer lockServer;
	private String key;
	private String id;
	private Boolean lockIsActive = false; // provide a cache layer

	public DistributedLock(LockServer lockServer, String key) {
		this.lockServer = lockServer;
		this.key = key;
		this.id = UUID.randomUUID().toString();
	}

	public Boolean tryLock(long duration, TimeUnit unit)
			throws InterruptedException {
		
		if(!lockServer.isAlive()){
			return true;
		}
		
		long sleepInterval = duration / MAX_RETRY_ATTEMPT;
		for (int i = 0; i < MAX_RETRY_ATTEMPT; i++) {
			if (!tryLock()) {
				unit.sleep(sleepInterval);
			} else {
				return true;
			}
		}

		// last attempt, no regret
		return tryLock();
	}

	public Boolean tryLock() {
		Boolean result;
		
		if(lockIsActive){
			return false;
		}
		
		try{
			result = lockServer.acquireLock(key, id);
			if (result){
				lockIsActive = true;
			}
		}
		catch(RuntimeException e){
			return true;
		}
		return result;
	}

	public void unlock() {
		lockServer.releaseLock(key, id);
		lockIsActive = false;
	}

}

package redis.concurrency;

/**
 * Provide interface to acquire a lock on a central server
 * @author chau.hoang
 *
 */
public interface LockServer {
	
	/**
	 * @param password : to protect the key from unauthorized release
	 * @return
	 */
	Boolean acquireLock(String key, String password);
	
	/**
	 * @param password : if different from current password, should not release
	 * @return
	 */
	Boolean releaseLock(String key, String password);
	
	/**
	 * Allow tracking if server is working properly
	 * @return
	 */
	Boolean isAlive();
}

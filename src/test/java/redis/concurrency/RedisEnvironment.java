package redis.concurrency;

/**
 * @author chau.hoang
 * Let temporarily store redis configuration here
 */
public class RedisEnvironment {

    public String getHost(){
    	return "localhost";
    }

    public int getPort(){
    	return 6379;
    }

}

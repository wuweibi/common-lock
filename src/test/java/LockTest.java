import com.wuweibi.common.lock.impl.ZookeeperLockHandler;
import org.I0Itec.zkclient.ZkClient;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author marker
 *         Created by Administrator on 2018/8/27.
 */
public class LockTest {

    @Test
    public void test() {
        ZkClient client = new ZkClient("192.168.89.64:2181");

        ZookeeperLockHandler handler = new ZookeeperLockHandler(client);
        String key = "mobi";
        boolean a = handler.tryLock(key, 10, TimeUnit.SECONDS);
        if (!a) {
            System.out.println("请稍后再说！");
        }

        try {

            System.out.println("业务逻辑处理");

        } catch (Exception e) {

        } finally {
            handler.unLock(key);
        }


    }
}

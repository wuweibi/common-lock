import com.wuweibi.common.lock.impl.ZookeeperLockHandler;
import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.CreateMode;

/**
 * @author marker
 *         Created by Administrator on 2018/8/27.
 */
public class MainTest {


    public static void main(String[] args) {

        ZkClient client = new ZkClient("192.168.89.64:2181");

        ZookeeperLockHandler handler = new ZookeeperLockHandler(client);


                String status = client.create("/lock/b","1", CreateMode.PERSISTENT);

        System.out.println(status);


    }
}

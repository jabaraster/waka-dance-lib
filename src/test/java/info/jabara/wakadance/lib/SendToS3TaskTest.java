/**
 *
 */
package info.jabara.wakadance.lib;

import java.nio.file.Paths;

import org.junit.Test;

/**
 * @author jabaraster
 */
public class SendToS3TaskTest {

    /**
     *
     */
    @SuppressWarnings({ "static-method", "nls" })
    @Test
    public void _run() {
        final String accessKey = System.getProperty("awsAccessKey");
        final String secretKey = System.getProperty("awsSecretKey");
        new SendToS3Task(accessKey, secretKey, Paths.get("/Users/jabaraster/Movies/Kawano's iPad-20140428151019.mov")) //
                .run();
    }

}

/**
 *
 */
package info.jabara.wakadance.app;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;

import jabara.general.EnvironmentUtil;
import jabara.general.ExceptionUtil;
import jabara.general.IoUtil;

/**
 * @author jabaraster
 */
final class AppUtil {

    private AppUtil() {
        // nop
    }

    static AmazonS3 createS3Client(final Map<String, String> pProperties) {
        final AmazonS3 ret = new AmazonS3Client(new BasicAWSCredentials(pProperties.get("awsAccessKey"), pProperties.get("awsSecretKey"))); //$NON-NLS-1$ //$NON-NLS-2$
        ret.setEndpoint("https://s3-ap-northeast-1.amazonaws.com"); //$NON-NLS-1$
        return ret;
    }

    static TransferManager createTransferManager(final AmazonS3 pS3Client, final int pPartSizeMegaBites) {
        final TransferManager manager = new TransferManager(pS3Client);
        final TransferManagerConfiguration c = new TransferManagerConfiguration();
        c.setMinimumUploadPartSize(pPartSizeMegaBites * 1024 * 1024);
        manager.setConfiguration(c);
        return manager;
    }

    static Map<String, String> loadProperties() {
        final Properties p = new Properties();
        try (final InputStream fileIn = new FileInputStream(EnvironmentUtil.getStringUnsafe("propertiesFilePath")); // //$NON-NLS-1$
                final BufferedInputStream bufIn = IoUtil.toBuffered(fileIn)) {
            p.load(bufIn);

            final Map<String, String> ret = new HashMap<>();
            for (final Map.Entry<Object, Object> entry : p.entrySet()) {
                ret.put((String) entry.getKey(), (String) entry.getValue());
            }
            return ret;

        } catch (final IOException e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

}

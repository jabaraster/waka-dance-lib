/**
 *
 */
package info.jabara.wakadance.app;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.Upload;

import jabara.general.EnvironmentUtil;
import jabara.general.ExceptionUtil;
import jabara.general.IoUtil;

/**
 * @author jabaraster
 */
public class S3Sender {

    private static final Map<String, String> props = load();

    /**
     * @param pArgs -
     * @throws Exception -
     */
    @SuppressWarnings("nls")
    public static void main(final String[] pArgs) throws Exception {
        Class.forName(props.get("javax.persistence.driver")); //$NON-NLS-1$
        final String url = props.get("javax.persistence.jdbc.url");
        final String user = props.get("javax.persistence.jdbc.user");
        final String password = props.get("javax.persistence.jdbc.password");
        try (final Connection conn = DriverManager.getConnection(url, user, password)) {
            exec(conn);
            System.out.println("終了.");
        }
    }

    private static AmazonS3 createS3Client() {
        final AmazonS3 s3Client = new AmazonS3Client(new BasicAWSCredentials(props.get("awsAccessKey"), props.get("awsSecretKey"))); //$NON-NLS-1$ //$NON-NLS-2$
        s3Client.setEndpoint("https://s3-ap-northeast-1.amazonaws.com"); //$NON-NLS-1$
        return s3Client;
    }

    private static void exec(final Connection conn) throws SQLException {
        final String sql = "select * from euploadfile where sendstate in ('UNUPLOAD','FAIL')"; //$NON-NLS-1$
        try (final PreparedStatement stmt = conn.prepareStatement(sql); //
                final ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                final String localFilePath = rs.getString("localfilepath"); //$NON-NLS-1$
                final long id = rs.getLong("id"); //$NON-NLS-1$
                final long contentLength = rs.getLong("size"); //$NON-NLS-1$
                send(conn, id, localFilePath, contentLength);
            }
        }
    }

    private static Map<String, String> load() {
        final Properties p = new Properties();
        try (final InputStream fileIn = new FileInputStream(EnvironmentUtil.getStringUnsafe("jpaPropertiesFilePath")); // //$NON-NLS-1$
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

    @SuppressWarnings("nls")
    private static void send(final Connection conn, final long pDbKey, final String pLocalFilePath, final long pContentLength) throws SQLException {
        System.out.println(pLocalFilePath + " のS3への送信を開始."); //$NON-NLS-1$
        final String bucketName = props.get("bucketName"); //$NON-NLS-1$
        System.out.println("bucket名 -> " + bucketName);

        final AmazonS3 s3Client = createS3Client();
        final TransferManager manager = new TransferManager(s3Client);
        final TransferManagerConfiguration c = new TransferManagerConfiguration();
        c.setMinimumUploadPartSize(5/* MB */ * 1024 * 1024);
        manager.setConfiguration(c);

        final Path path = Paths.get(pLocalFilePath);
        try (final InputStream fileIn = Files.newInputStream(path); //
                final BufferedInputStream bufIn = IoUtil.toBuffered(fileIn)) {
            final ObjectMetadata putMetaData = new ObjectMetadata();
            putMetaData.setContentLength(pContentLength);
            final Upload upload = manager.upload( //
                    bucketName //
                    , path.getFileName().toString() //
                    , fileIn //
                    , putMetaData);
            upload.addProgressListener(new ProgressListener() {
                @Override
                public void progressChanged(final ProgressEvent pProgressEvent) {
                    System.out.print(".");
                }
            });
            upload.waitForCompletion();
            switch (upload.getState()) {
            case Completed:
                System.out.println("正常終了."); //$NON-NLS-1$
                updateStatus(conn, pDbKey, "SUCCESS"); //$NON-NLS-1$
                break;
            case Failed:
                System.out.println("異常終了."); //$NON-NLS-1$
                updateStatus(conn, pDbKey, "FAIL"); //$NON-NLS-1$
                break;
            // $CASES-OMITTED$
            default:
                throw new IllegalStateException();
            }
        } catch (final InterruptedException e) {
            System.out.println("異常終了."); //$NON-NLS-1$
            throw ExceptionUtil.rethrow(e);
        } catch (final IOException e) {
            System.out.println("異常終了."); //$NON-NLS-1$
            updateStatus(conn, pDbKey, "FAIL"); //$NON-NLS-1$
            e.printStackTrace();
        } finally {
            manager.shutdownNow();
        }
    }

    private static void updateStatus(final Connection conn, final long pDbKey, final String pStatus) throws SQLException {
        final String sql = "update euploadfile set sendstate = ? where id = ?"; //$NON-NLS-1$
        try (final PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pStatus);
            stmt.setLong(2, pDbKey);
            stmt.executeUpdate();
        }
    }
}

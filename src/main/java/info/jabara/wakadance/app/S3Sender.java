/**
 *
 */
package info.jabara.wakadance.app;

import java.io.BufferedInputStream;
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
import java.util.Map;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import jabara.general.ExceptionUtil;
import jabara.general.IoUtil;

/**
 * @author jabaraster
 */
public class S3Sender {

    private static final Map<String, String> props = AppUtil.loadProperties();

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

    @SuppressWarnings("nls")
    private static void send(final Connection conn, final long pDbKey, final String pLocalFilePath, final long pContentLength) throws SQLException {
        System.out.println(pLocalFilePath + " のS3への送信を開始."); //$NON-NLS-1$
        final String bucketName = props.get("bucketName"); //$NON-NLS-1$
        System.out.println("bucket名 -> " + bucketName);

        final AmazonS3 s3Client = AppUtil.createS3Client(props);
        final TransferManager manager = AppUtil.createTransferManager(s3Client, 5/* MB */);

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

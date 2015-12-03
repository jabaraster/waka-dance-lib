/**
 *
 */
package info.jabara.wakadance.lib;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.amazonaws.services.s3.transfer.Upload;

import jabara.general.ExceptionUtil;
import jabara.general.IoUtil;

/**
 * @author jabaraster
 */
public class SendToS3Task implements Runnable {

    private final AmazonS3 s3Client;
    private final Path     uploadedFilePath;

    private int    uploadPartSizeMegaBytes = 5;
    private String bucketName              = "waka-dance-test"; //$NON-NLS-1$

    private int uploadTimeoutSeconds = 30;

    /**
     * @param pAccessKey -
     * @param pSecredKey -
     * @param pUploadedFilePath -
     */
    public SendToS3Task(final String pAccessKey, final String pSecredKey, final Path pUploadedFilePath) {

        final ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setConnectionTimeout((int) TimeUnit.SECONDS.toMillis(this.uploadTimeoutSeconds));
        this.s3Client = new AmazonS3Client(new BasicAWSCredentials(pAccessKey, pSecredKey));
        this.s3Client.setEndpoint("https://s3-ap-northeast-1.amazonaws.com"); //$NON-NLS-1$

        this.uploadedFilePath = pUploadedFilePath;
    }

    /**
     * @return the bucketName
     */
    public String getBucketName() {
        return this.bucketName;
    }

    /**
     * @return the s3UploadPartSizeMegaBytes
     */
    public int getUploadPartSizeMegaBytes() {
        return this.uploadPartSizeMegaBytes;
    }

    /**
     * @return the uploadTimeoutSeconds
     */
    public int getUploadTimeoutSeconds() {
        return this.uploadTimeoutSeconds;
    }

    @Override
    public void run() {
        // TransferManagerを利用
        final TransferManager manager = new TransferManager(this.s3Client);

        // 分割サイズを設定
        final TransferManagerConfiguration c = new TransferManagerConfiguration();
        c.setMinimumUploadPartSize(this.uploadPartSizeMegaBytes * 1024 * 1024);
        manager.setConfiguration(c);

        // メタデータに分割したデータのサイズを指定
        try (final InputStream fileIn = Files.newInputStream(this.uploadedFilePath);                                        //
                final BufferedInputStream bufIn = IoUtil.toBuffered(fileIn)) {
            final ObjectMetadata putMetaData = new ObjectMetadata();
            putMetaData.setContentLength(this.uploadedFilePath.toFile().length());
            final Upload upload = manager.upload( //
                    this.bucketName //
                    , this.uploadedFilePath.getFileName().toString() //
                    , fileIn //
                    , putMetaData);
            upload.waitForCompletion();
            switch (upload.getState()) {
            case Completed:
                return;
            case Failed:
                return;
            default:
                throw new IllegalStateException();
            }
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final InterruptedException e) {
            throw ExceptionUtil.rethrow(e);
        }
    }

    /**
     * @param pBucketName the bucketName to set
     */
    public void setBucketName(final String pBucketName) {
        this.bucketName = pBucketName;
    }

    /**
     * @param pS3UploadPartSizeMegaBytes the s3UploadPartSizeMegaBytes to set
     */
    public void setUploadPartSizeMegaBytes(final int pS3UploadPartSizeMegaBytes) {
        this.uploadPartSizeMegaBytes = pS3UploadPartSizeMegaBytes;
    }

    /**
     * @param pUploadTimeoutSeconds the uploadTimeoutSeconds to set
     */
    public void setUploadTimeoutSeconds(final int pUploadTimeoutSeconds) {
        this.uploadTimeoutSeconds = pUploadTimeoutSeconds;
    }

}

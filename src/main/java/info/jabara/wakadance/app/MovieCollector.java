/**
 *
 */
package info.jabara.wakadance.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.amazonaws.services.s3.transfer.TransferManager;

import info.jabara.wakadance.entity.EUploadFile;
import jabara.general.ExceptionUtil;
import jabara.jpa.entity.EntityBase_;

/**
 * @author jabaraster
 */
public class MovieCollector implements AutoCloseable {

    private final EntityManagerFactory emf;
    private final EntityManager        em;
    private final TransferManager      transferManager;

    /**
     *
     */
    public MovieCollector() {
        final Map<String, String> properties = AppUtil.loadProperties();
        this.emf = Persistence.createEntityManagerFactory("WakaDance", properties); //$NON-NLS-1$
        this.em = this.emf.createEntityManager();
        this.transferManager = AppUtil.createTransferManager(AppUtil.createS3Client(properties), 5);
    }

    /**
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() {
        try {
            this.em.close();
        } catch (final Exception e) {
            e.printStackTrace(System.out);
        }
        try {
            this.emf.close();
        } catch (final Exception e) {
            e.printStackTrace(System.out);
        }

        this.transferManager.shutdownNow();
    }

    private void exec() {
        for (final Entry<String, List<EUploadFile>> entry : this.getAll().entrySet()) {
            MovieCollector.download(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("serial")
    private Map<String, List<EUploadFile>> getAll() {
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery<EUploadFile> query = builder.createQuery(EUploadFile.class);
        final Root<EUploadFile> root = query.from(EUploadFile.class);
        query.orderBy(builder.asc(root.get(EntityBase_.created)));
        final Map<String, List<EUploadFile>> ret = new HashMap<String, List<EUploadFile>>() {
            @Override
            public List<EUploadFile> get(final Object pKey) {
                @SuppressWarnings("hiding")
                List<EUploadFile> ret = super.get(pKey);
                if (ret == null) {
                    ret = new ArrayList<>();
                    this.put((String) pKey, ret);
                }
                return ret;
            }
        };

        for (final EUploadFile f : this.em.createQuery(query).getResultList()) {
            ret.get(f.getPersonName()).add(f);
        }
        return ret;
    }

    /**
     * @param pArgs -
     */
    public static void main(final String[] pArgs) {
        try (final MovieCollector app = new MovieCollector()) {
            app.exec();
        }
    }

    private static void download(final String pPersonName, final List<EUploadFile> pFiles) {
        final Path dir = Paths.get("target/movies", pPersonName); //$NON-NLS-1$
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                throw ExceptionUtil.rethrow(e);
            }
        }
    }
}

/**
 *
 */
package info.jabara.wakadance.lib;

/**
 * @author jabaraster
 */
public final class WakaUtil {
    private WakaUtil() {
        // nop
    }

    /**
     * @param pFileName -
     * @return -
     */
    public static String getSuffix(final String pFileName) {
        final int lastDotPosition = pFileName.lastIndexOf("."); //$NON-NLS-1$
        if (lastDotPosition != -1) {
            return pFileName.substring(lastDotPosition + 1);
        }
        return null;
    }

}

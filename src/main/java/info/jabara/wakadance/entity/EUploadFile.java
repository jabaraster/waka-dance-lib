/**
 *
 */
package info.jabara.wakadance.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import jabara.jpa.entity.GlobalEntityBase;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author jabaraster
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class EUploadFile extends GlobalEntityBase<EUploadFile> {
    private static final long serialVersionUID = 2631625076729897717L;

    @Column(length = 300, nullable = false)
    String                    personName       = "";                  //$NON-NLS-1$

    @Column(length = 1000, nullable = false)
    String                    uploadFileName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    SendState                 sendState;

    @Column(length = 3000, nullable = false)
    String                    localFilePath;

    @Column(length = 100, nullable = false)
    String                    contentType;

    @Column(nullable = false)
    long                      size             = -1;
}

package org.texttechnologylab.models.authentication;


import lombok.Getter;
import lombok.Setter;
import org.texttechnologylab.uce.common.models.ModelBase;
import org.texttechnologylab.uce.common.models.corpus.Document;

import javax.persistence.*;

@Entity
@Table(
        name = "documentpermissions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"document_id", "name", "type"})
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class DocumentPermission extends ModelBase {
    /**
     * This class holds the permissions a user has on a single document.
     * We utilize Keycloak to perform authentication, on login we receive the user info and his group memberships.
     * UCE tracks the permissions internally using this table.
     * The table combined group and user permissions. We precompute the effective permissions for each user on login.
     * In SQL the queries thus only have to check for the user and the minimum permission level.
     * TODO later we should consider using Keycloak's API to asynchronously update permissions when group memberships change.
     */

    // NOTE this should not be reordered, as it would break existing databases!
    public enum DOCUMENT_PERMISSION_TYPE {
        GROUP,
        USER,
        EFFECTIVE
    }

    // Permission level, you can test the needed permission with "greater than"
    // NOTE this should not be reordered, as it would break existing databases!
    public enum DOCUMENT_PERMISSION_LEVEL {
        NONE,
        READ,
        WRITE,
        OWNER
    }

    @Setter
    @Getter
    @Column(nullable = false)
    private DOCUMENT_PERMISSION_TYPE type;

    @Setter
    @Getter
    @Column(nullable = false)
    private DOCUMENT_PERMISSION_LEVEL level;

    // User/group name, this is the name we receive from Keycloak
    @Setter
    @Getter
    @Column(nullable = false)
    private String name;

    // NOTE permissions should not be set using this field directly, use the Document's addPermission/removePermission methods to keep both sides of the relation in sync
    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
}

package edu.stanford.bmir.protege.web.shared.form;

import com.google.gwt.user.client.rpc.IsSerializable;
import edu.stanford.bmir.protege.web.shared.util.UUIDUtil;

import javax.annotation.Nonnull;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.shared.util.UUIDUtil.isWellFormed;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 12 Jul 2017
 *
 * Represents an id for a WebProtege collection.  A collection id is UUID collection.  Collections contain data about
 * domain objects that get transformed into OWL axioms.
 */
public class CollectionId implements IsSerializable {

    private String id;

    private CollectionId() {
    }

    private CollectionId(@Nonnull String id) {
        this.id = checkNotNull(id);
    }

    /**
     * Gets a collection id for the specified UUID string.
     * @param uuid The UUID string.  This must be formatted according to the UUID pattern specified
     *             in {@link edu.stanford.bmir.protege.web.shared.util.UUIDUtil#UUID_PATTERN}.  A runtime
     *             exception will be thrown if this is not the case.
     * @return The {@link CollectionId} for the specified UUID string.
     */
    public static CollectionId get(@Nonnull String uuid) {
        if(!isWellFormed(uuid)) {
            throw new RuntimeException("Invalid UUID format: " + uuid);
        }
        return new CollectionId(uuid);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CollectionId)) {
            return false;
        }
        CollectionId other = (CollectionId) obj;
        return this.id.equals(other.id);
    }


    @Override
    public String toString() {
        return toStringHelper("CollectionId")
                .addValue(id)
                .toString();
    }
}
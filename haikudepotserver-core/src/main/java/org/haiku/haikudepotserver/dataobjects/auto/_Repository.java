package org.haiku.haikudepotserver.dataobjects.auto;

import java.sql.Timestamp;
import java.util.List;

import org.apache.cayenne.exp.Property;
import org.haiku.haikudepotserver.dataobjects.RepositorySource;
import org.haiku.haikudepotserver.dataobjects.support.AbstractDataObject;

/**
 * Class _Repository was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Repository extends AbstractDataObject {

    private static final long serialVersionUID = 1L; 

    public static final String ID_PK_COLUMN = "id";

    public static final Property<Boolean> ACTIVE = Property.create("active", Boolean.class);
    public static final Property<String> CODE = Property.create("code", String.class);
    public static final Property<Timestamp> CREATE_TIMESTAMP = Property.create("createTimestamp", Timestamp.class);
    public static final Property<String> DESCRIPTION = Property.create("description", String.class);
    public static final Property<String> INFORMATION_URL = Property.create("informationUrl", String.class);
    public static final Property<Timestamp> MODIFY_TIMESTAMP = Property.create("modifyTimestamp", Timestamp.class);
    public static final Property<String> NAME = Property.create("name", String.class);
    public static final Property<String> PASSWORD_HASH = Property.create("passwordHash", String.class);
    public static final Property<String> PASSWORD_SALT = Property.create("passwordSalt", String.class);
    public static final Property<List<RepositorySource>> REPOSITORY_SOURCES = Property.create("repositorySources", List.class);

    public void setActive(Boolean active) {
        writeProperty("active", active);
    }
    public Boolean getActive() {
        return (Boolean)readProperty("active");
    }

    public void setCode(String code) {
        writeProperty("code", code);
    }
    public String getCode() {
        return (String)readProperty("code");
    }

    public void setCreateTimestamp(Timestamp createTimestamp) {
        writeProperty("createTimestamp", createTimestamp);
    }
    public Timestamp getCreateTimestamp() {
        return (Timestamp)readProperty("createTimestamp");
    }

    public void setDescription(String description) {
        writeProperty("description", description);
    }
    public String getDescription() {
        return (String)readProperty("description");
    }

    public void setInformationUrl(String informationUrl) {
        writeProperty("informationUrl", informationUrl);
    }
    public String getInformationUrl() {
        return (String)readProperty("informationUrl");
    }

    public void setModifyTimestamp(Timestamp modifyTimestamp) {
        writeProperty("modifyTimestamp", modifyTimestamp);
    }
    public Timestamp getModifyTimestamp() {
        return (Timestamp)readProperty("modifyTimestamp");
    }

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setPasswordHash(String passwordHash) {
        writeProperty("passwordHash", passwordHash);
    }
    public String getPasswordHash() {
        return (String)readProperty("passwordHash");
    }

    public void setPasswordSalt(String passwordSalt) {
        writeProperty("passwordSalt", passwordSalt);
    }
    public String getPasswordSalt() {
        return (String)readProperty("passwordSalt");
    }

    public void addToRepositorySources(RepositorySource obj) {
        addToManyTarget("repositorySources", obj, true);
    }
    public void removeFromRepositorySources(RepositorySource obj) {
        removeToManyTarget("repositorySources", obj, true);
    }
    @SuppressWarnings("unchecked")
    public List<RepositorySource> getRepositorySources() {
        return (List<RepositorySource>)readProperty("repositorySources");
    }

}

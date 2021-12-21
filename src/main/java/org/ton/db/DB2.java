package org.ton.db;

import lombok.extern.slf4j.Slf4j;
import org.ton.settings.MyLocalTonSettings;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.File;

@Slf4j
public class DB2 {
    private EntityManagerFactory emf;
    private final String dbFileName;

    public DB2(String dbName) {
        dbFileName = "myLocalTon_" + dbName + ".odb";
        emf = Persistence.createEntityManagerFactory("objectdb:" + MyLocalTonSettings.DB_DIR + File.separator + dbFileName);
        log.debug("DB {} initialized.", dbName);
    }

    public String getDbFileName() {
        return dbFileName;
    }

    public EntityManagerFactory getEmf() {
        return emf;
    }

}

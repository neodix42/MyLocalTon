package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.db.DB2;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

@Slf4j
public class PreinstalledCountCallable implements Callable<String> {
    String dbName;

    public PreinstalledCountCallable(String val) {
        dbName = val;
    }

    public String call() {
        long result = 0;
        DB2 db = new DB2(dbName);
        EntityManager em = db.getEmf().createEntityManager();
        try {
            result = em.createQuery("SELECT count(t) FROM WalletEntity t where t.preinstalled = true", Long.class).getSingleResult();
            return String.valueOf(result);
        } catch (Exception e) {
            log.error("Error {}", e.getMessage());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return String.valueOf(result);
    }
}
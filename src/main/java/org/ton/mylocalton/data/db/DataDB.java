package org.ton.mylocalton.data.db;

import static java.util.Objects.nonNull;

import java.io.File;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.settings.MyLocalTonSettings;

@Slf4j
public class DataDB {

  public static final EntityManagerFactory emf;

  static {
    String dbFileName = "myLocalTon_data.odb";
    emf =
        Persistence.createEntityManagerFactory(
            "objectdb:" + MyLocalTonSettings.DB_DIR + File.separator + dbFileName);
    log.info("DataDB {} initialized.", "myLocalTon_data");
  }

  private DataDB() {}

  public static boolean insertDataWallet(DataWalletEntity wallet) {
    EntityManager em = emf.createEntityManager();
    try {
      //      log.info("inserting into db wallet {}", wallet);
      em.getTransaction().begin();
      em.persist(wallet);
      em.getTransaction().commit();
      //      log.info("wallet inserted into db {}", wallet);
      return true;
    } catch (Exception e) {
      log.error("cannot insert wallet {}", wallet);
      return false;
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static void deleteDataWallet(DataWalletPk dataWalletPk) {
    EntityManager em = emf.createEntityManager();
    try {
      DataWalletEntity dataWalletEntity = em.find(DataWalletEntity.class, dataWalletPk);
      if (nonNull(dataWalletEntity)) {
        em.getTransaction().begin();
        em.remove(dataWalletEntity);
        em.getTransaction().commit();
        log.info("wallet deleted {}", dataWalletPk);
      }
    } catch (Exception e) {
      log.error("cannot delete wallet {}", dataWalletPk);
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static void updateDataWalletStatus(DataWalletPk dataWalletPk, String status) {
    EntityManager em = emf.createEntityManager();
    try {
      DataWalletEntity dataWalletEntity = em.find(DataWalletEntity.class, dataWalletPk);
      if (nonNull(dataWalletEntity)) {
        em.getTransaction().begin();
        dataWalletEntity.setStatus(status);
        em.getTransaction().commit();
        //        log.info("wallet updated {}", walletEntity);
      }
    } catch (Exception e) {
      log.error("cannot update wallet status {}", dataWalletPk);
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static List<DataWalletEntity> getDataWalletsToSend() {
    EntityManager em = emf.createEntityManager();
    try {
      List<DataWalletEntity> results =
          em.createQuery(
                  "SELECT b FROM DataWalletEntity b where status is null ORDER BY b.createdAt ASC",
                  DataWalletEntity.class)
              .getResultList();
      return results;
    } catch (Exception e) {
      log.error("cannot get wallets to send {}", e.getMessage());
      return new ArrayList<>();
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static List<DataWalletEntity> getDataWalletsSent() {
    EntityManager em = emf.createEntityManager();
    try {
      List<DataWalletEntity> results =
          em.createQuery(
                  "SELECT b FROM DataWalletEntity b where status = 'sent' ORDER BY b.createdAt ASC",
                  DataWalletEntity.class)
              .getResultList();
      return results;
    } catch (Exception e) {
      log.error("cannot get wallets sent {}", e.getMessage());
      return new ArrayList<>();
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static long getTotalDataWallets() {
    EntityManager em = emf.createEntityManager();
    try {
      return em.createQuery("SELECT count(b) FROM DataWalletEntity b", Long.class)
          .getSingleResult();
    } catch (Exception e) {
      log.error("cannot get total wallets {}", e.getMessage());
      return -1;
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static long deleteExpiredDataWallets(long expirationPeriod) {
    EntityManager em = emf.createEntityManager();
    try {
      em.getTransaction().begin();
      Query query =
          em.createQuery(
              "DELETE FROM DataWalletEntity b where b.createdAt + :expirationperiod < :datetimenow",
              DataWalletEntity.class);
      int deletedCount =
          query
              .setParameter("expirationperiod", expirationPeriod)
              .setParameter("datetimenow", Instant.now().getEpochSecond())
              .executeUpdate();
      em.getTransaction().commit();
      log.info("deleted from queue {}", deletedCount);

      return deletedCount;
    } catch (Exception e) {
      log.error("cannot delete expired wallets {}", e.getMessage());
      return -1;
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }

  public static DataWalletEntity findDataWallet(DataWalletPk dataWalletPk) {
    EntityManager em = emf.createEntityManager();
    try {
      return em.find(DataWalletEntity.class, dataWalletPk);
    } catch (Exception e) {
      log.error("cannot find wallet {}", dataWalletPk);
      return null;
    } finally {
      if (em.isOpen()) em.close();
    }
  }

  public static void addDataRequest(String walletAddr, BigInteger balance) {

    if (nonNull(
        findDataWallet(
            DataWalletPk.builder()
                .walletAddress(walletAddr)
                .build()))) { // this wallet already requested
      log.error("cant add request - wallet {} found", walletAddr);
      return;
    }

    log.info(
        "REQUEST ADDED {} {} from {}",
        balance,
        walletAddr,
        Thread.currentThread().getStackTrace()[2].getClassName());
    insertDataWallet(
        DataWalletEntity.builder()
            .walletAddress(walletAddr)
            .createdAt(Instant.now().getEpochSecond())
            .balance(balance)
            .build());
  }
}

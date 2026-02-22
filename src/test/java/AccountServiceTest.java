package co.edu.uniandes.dse.TallerPruebas.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.PocketEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.services.AccountService;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

/**
 * Pruebas regla 1
 */
@DataJpaTest
@Transactional
@Import(AccountService.class)
public class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AccountEntity> accountList = new ArrayList<>();
    private List<PocketEntity> pocketList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from PocketEntity").executeUpdate();
        entityManager.getEntityManager().createQuery("delete from AccountEntity").executeUpdate();
    }

    private void insertData() {
        AccountEntity accountEntity = factory.manufacturePojo(AccountEntity.class);
        accountEntity.setEstado("ACTIVA");
        accountEntity.setSaldo(1000.0);
        entityManager.persist(accountEntity);
        accountList.add(accountEntity);

        PocketEntity pocketEntity = factory.manufacturePojo(PocketEntity.class);
        pocketEntity.setSaldo(0.0);
        pocketEntity.setAccount(accountEntity);
        entityManager.persist(pocketEntity);
        pocketList.add(pocketEntity);

        accountEntity.setPockets(pocketList);
    }

    /**
     * 1. Éxito: mover dinero de la cuenta al bolsillo con saldo suficiente.
     */
    @Test
    void testMoverDineroBolsilloSuficiente() throws EntityNotFoundException, BusinessLogicException {
        AccountEntity account = accountList.get(0);
        PocketEntity pocket = pocketList.get(0);

        accountService.transferToPocket(account.getId(), pocket.getId(), 500.0);

        AccountEntity updatedAccount = entityManager.find(AccountEntity.class, account.getId());
        PocketEntity updatedPocket = entityManager.find(PocketEntity.class, pocket.getId());

        assertEquals(500.0, updatedAccount.getSaldo());
        assertEquals(500.0, updatedPocket.getSaldo());
    }
    /**
    * 2. Éxito: mover exactamente un monto igual al saldo de la cuenta.
    */
    @Test
    void ttestRegla1ExactAmountSuccess() throws EntityNotFoundException, BusinessLogicException {
        AccountEntity account = accountList.get(0);
        PocketEntity pocket = pocketList.get(0);

        double monto = account.getSaldo(); // usar todo el saldo disponible
        accountService.transferToPocket(account.getId(), pocket.getId(), monto);

        AccountEntity updatedAccount = entityManager.find(AccountEntity.class, account.getId());
        PocketEntity updatedPocket = entityManager.find(PocketEntity.class, pocket.getId());

        assertEquals(0.0, updatedAccount.getSaldo());
        assertEquals(monto, updatedPocket.getSaldo());
}


    /**
     * 3. Fallo: mover dinero con insuficiente saldo en la cuenta.
     */
    @Test
    void testMoverDineroConSaldoInsuficiente() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity account = accountList.get(0);
            PocketEntity pocket = pocketList.get(0);

            accountService.transferToPocket(account.getId(), pocket.getId(), 2000.0);
        });
    }

    /**
     * 4. Fallo: mover dinero desde bolsillo que no existe.
     */
    @Test
    void testMoverDineroBolsilloNoExiste() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity account = accountList.get(0);
            accountService.transferToPocket(account.getId(), 0L, 100.0);
        });
    }

    /**
     *5.  Fallo: mover dinero desde cuenta que no existe.
     */
    @Test
    void testMoverDineroCuentaNoExiste() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity account = accountList.get(0);
            accountService.transferToPocket(account.getId(), 0L, 100.0);
        });
    }
}

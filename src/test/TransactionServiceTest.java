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
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

/**
 * Pruebas regla 2
 */
@DataJpaTest
@Transactional
@Import(TransactionService.class)
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TestEntityManager entityManager;

    private PodamFactory factory = new PodamFactoryImpl();

    private List<AccountEntity> accountList = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearData();
        insertData();
    }

    private void clearData() {
        entityManager.getEntityManager().createQuery("delete from AccountEntity").executeUpdate();
    }

    private void insertData() {
        for (int i = 0; i < 2; i++) {
            AccountEntity accountEntity = factory.manufacturePojo(AccountEntity.class);
            accountEntity.setEstado("ACTIVA");
            accountEntity.setSaldo(1000.0);
            entityManager.persist(accountEntity);
            accountList.add(accountEntity);
        }
    }

    /**
     * 1. Éxito: transferir dinero entre dos cuentas distintas con suficiente saldo.
     */
    @Test
    void testTransferBetweenAccountsSuccess() throws EntityNotFoundException, BusinessLogicException {
        AccountEntity origen = accountList.get(0);
        AccountEntity destino = accountList.get(1);

        transactionService.transferBetweenAccounts(origen.getId(), destino.getId(), 500.0);

        AccountEntity updatedOrigen = entityManager.find(AccountEntity.class, origen.getId());
        AccountEntity updatedDestino = entityManager.find(AccountEntity.class, destino.getId());

        assertEquals(500.0, updatedOrigen.getSaldo());
        assertEquals(1500.0, updatedDestino.getSaldo());
    }

    /**
     * 2. Fallo: Transferir dinero cuando cuenta origen no existe.
     */
    @Test
    void testTransferInvalidOriginAccount() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity destino = accountList.get(1);
            transactionService.transferBetweenAccounts(0L, destino.getId(), 100.0);
        });
    }

    /**
     * 3. Fallo: Transferir dinero cuando cuenta destino no existe.
     */
    @Test
    void testTransferInvalidDestinationAccount() {
        assertThrows(EntityNotFoundException.class, () -> {
            AccountEntity origen = accountList.get(0);
            transactionService.transferBetweenAccounts(origen.getId(), 0L, 100.0);
        });
    }

    /**
     * 4. Fallo: Transferir dinero cuando con saldo insuficiente en la cuenta origen.
     */
    @Test
    void testTransferInsufficientFunds() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity origen = accountList.get(0);
            AccountEntity destino = accountList.get(1);
            transactionService.transferBetweenAccounts(origen.getId(), destino.getId(), 2000.0);
        });
    }

    /**
     * 5. Fallo: Transferir dinero cuando cuenta origen y destino son la misma, transferiri dinero desde origen a origen.
     */
    @Test
    void testTransferSameAccount() {
        assertThrows(BusinessLogicException.class, () -> {
            AccountEntity origen = accountList.get(0);
            transactionService.transferBetweenAccounts(origen.getId(), origen.getId(), 100.0);
        });
    }
}

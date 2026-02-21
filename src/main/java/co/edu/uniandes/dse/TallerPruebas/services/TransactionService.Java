package co.edu.uniandes.dse.TallerPruebas.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.TransactionEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.repositories.AccountRepository;
import co.edu.uniandes.dse.TallerPruebas.repositories.TransactionRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase que implementa la lógica de la regla 2
 */
@Slf4j
@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Transfiere dinero entre dos cuentas.
     * 
     * @param origenId id de la cuenta origen
     * @param destinoId id de la cuenta destino
     * @param monto cantidad a transferir
     * @return entidad de la transacción creada
     * @throws EntityNotFoundException si alguna de las cuentas no existe
     * @throws BusinessLogicException si las cuentas son iguales, están bloqueadas o no hay saldo suficiente
     */
    @Transactional
    public TransactionEntity transferBetweenAccounts(Long origenId, Long destinoId, Double monto) throws EntityNotFoundException, BusinessLogicException {
        log.info("Inicia proceso de transferencia de dinero de la cuenta {} a la cuenta {}", origenId, destinoId);

        // 1. Verificar que la cuenta origen existe
        Optional<AccountEntity> cuentaOrigen = accountRepository.findById(origenId);
        if (cuentaOrigen.isEmpty()) {
            throw new EntityNotFoundException("La cuenta origen no existe, no se puede hacer la transacción");
        }

        // 2. Verificar que la cuenta destino existe
        Optional<AccountEntity> cuentaDestino = accountRepository.findById(destinoId);
        if (cuentaDestino.isEmpty()) {
            throw new EntityNotFoundException("La cuenta destino no existe, no se puede hacer la transacción");
        }

        // 3. Verificar que las cuentas sean diferentes, que no sean la misma cuenta en la que se movera el dinero (mover dinero de origen a origen)
        if (origenId.equals(destinoId)) {
            throw new BusinessLogicException("La cuenta origen y destino deben ser diferentes, no pueden ser las mismas");
        }

        // 4. Verificar que la cuenta origen esté activa
        if (!"ACTIVA".equals(cuentaOrigen.get().getEstado())) {
            throw new BusinessLogicException("La cuenta origen debe estar en estado ACTIVA");
        }

        // 5. Verificar que la cuenta destino esté activa
        if (!"ACTIVA".equals(cuentaDestino.get().getEstado())) {
            throw new BusinessLogicException("La cuenta destino debe estar en estado ACTIVA");
        }

        // 6. Verificar que la cuenta origen tenga saldo suficiente
        if (cuentaOrigen.get().getSaldo() < monto) {
            throw new BusinessLogicException("Saldo insuficiente en la cuenta origen, no se puede hacer la transacción");
        }

        // 7. Actualizar saldos después de mover el dinero, si es que se logro mover el dinero
        cuentaOrigen.get().setSaldo(cuentaOrigen.get().getSaldo() - monto);
        cuentaDestino.get().setSaldo(cuentaDestino.get().getSaldo() + monto);
        log.info("Termina proceso de transferencia de dinero de la cuenta {} a la cuenta {}", origenId, destinoId);
        return transactionRepository.save(transaccion);
    }
}

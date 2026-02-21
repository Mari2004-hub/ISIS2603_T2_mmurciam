package co.edu.uniandes.dse.TallerPruebas.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniandes.dse.TallerPruebas.entities.AccountEntity;
import co.edu.uniandes.dse.TallerPruebas.entities.PocketEntity;
import co.edu.uniandes.dse.TallerPruebas.exceptions.BusinessLogicException;
import co.edu.uniandes.dse.TallerPruebas.exceptions.EntityNotFoundException;
import co.edu.uniandes.dse.TallerPruebas.repositories.AccountRepository;
import co.edu.uniandes.dse.TallerPruebas.repositories.PocketRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Clase que implementa la lógica de la regla 1
 */
@Slf4j
@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PocketRepository pocketRepository;

    /**
     * Transfiere dinero de una cuenta a un bolsillo.
     * 
     * @param accountId id de la cuenta origen
     * @param pocketId id del bolsillo destino
     * @param monto cantidad a transferir
     * @return entidad del bolsillo actualizado
     * @throws EntityNotFoundException si la cuenta o el bolsillo no existen
     * @throws BusinessLogicException si la cuenta está bloqueada, el bolsillo está bloqueado o no hay saldo suficiente
     */
    @Transactional
    public PocketEntity transferToPocket(Long accountId, Long pocketId, Double monto) throws EntityNotFoundException, BusinessLogicException {
        log.info("Inicia proceso de transferencia de dinero de la cuenta {} al bolsillo {}", accountId, pocketId);

        // 1. Verificar que la cuenta existe
        Optional<AccountEntity> accountEntity = accountRepository.findById(accountId);
        if (accountEntity.isEmpty()) {
            throw new EntityNotFoundException("La cuenta no existe");
        }

        // 2. Verificar que el bolsillo existe
        Optional<PocketEntity> pocketEntity = pocketRepository.findById(pocketId);
        if (pocketEntity.isEmpty()) {
            throw new EntityNotFoundException("El bolsillo no existe");
        }

        // 3. Verificar que la cuenta esté activa
        if (!"ACTIVA".equals(accountEntity.get().getEstado())) {
            throw new BusinessLogicException("La cuenta debe estar en estado ACTIVA para transferir dinero");
        }

        // 4. Verificar que el bolsillo esté activo
        if (!"ACTIVA".equals(pocketEntity.get().getSaldo())) {
            throw new BusinessLogicException("El bolsillo debe estar en estado ACTIVA para recibir dinero");
        }

        // 5. Verificar que la cuenta tenga saldo suficiente
        if (accountEntity.get().getSaldo() < monto) {
            throw new BusinessLogicException("La cuenta no tiene saldo suficiente para la transferencia");
        }

        // 6. Actualizar saldos, después de mover el dinero, si es que se logro mover el dinero
        accountEntity.get().setSaldo(accountEntity.get().getSaldo() - monto);
        pocketEntity.get().setSaldo(pocketEntity.get().getSaldo() + monto);
        log.info("Termina proceso de transferencia de dinero de la cuenta {} al bolsillo {}", accountId, pocketId);
        return pocketRepository.save(pocketEntity.get());
    }
}

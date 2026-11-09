package ma.youssef.bank.query;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationRepository extends JpaRepository<AccountOperation, String> {
    List<AccountOperation> findByAccountIdOrderByOccurredAtAsc(String accountId);
}

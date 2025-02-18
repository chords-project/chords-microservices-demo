package choral.sagas;

import java.io.Serializable;
import java.util.HashMap;

public interface Transaction {
    /**
     * Perform transaction action
     * Must be idempotent and it can abort
     *
     * @param name a unique name for the transaction to commit
     * @param data additional data needed to perform the transaction
     */
    void commit(String name, HashMap<String, Serializable> data) throws Exception;

    /**
     * Perform a compensation of the transaction action after it has been (partially or fully) committed
     * Must be idempotent, commutative and it can not abort
     *
     * @param name a unique name for the transaction to compensate
     * @param data additional data needed to compensate the transaction
     */
    void compensate(String name, HashMap<String, Serializable> data);
}

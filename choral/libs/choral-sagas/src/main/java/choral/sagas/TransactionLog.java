package choral.sagas;

import java.io.Serializable;
import java.util.HashMap;

public interface TransactionLog {

    int startSaga();
    void completeSaga(int sagaID);
    void abortSaga(int sagaID);

    /**
     * Write a "start transaction" message to the log
     * @param sagaID the id of the saga this transaction is associated with
     * @return a unique transaction id
     */
    int startTransaction(int sagaID, String name, HashMap<String, Serializable> data);

    /**
     * Write an "end transaction" message to the log
     * @param sagaID the id of the saga this transaction is associated with
     * @param txID the id of the transaction that has ended
     */
    void endTransaction(int sagaID, int txID);
}

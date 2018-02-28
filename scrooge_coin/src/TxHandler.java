import java.util.HashSet;
import java.util.Set;

/**
 *  Coursera Introduction to Crypto Currency Course
 *  Assignment1: Scrooge Coin
 *  @author mross Marty Ross
 */
public class TxHandler {

    /** truth source: transaction ledger */
    private final UTXOPool mUtxoPool;
    
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(final UTXOPool utxoPool) {
        mUtxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(final Transaction tx) {       

        final Set<UTXO> utxoSet = new HashSet<>(tx.numInputs());
        double sumInputValues = 0d;
        for (int i = 0; i < tx.numInputs(); i++) {
            
            final Transaction.Input txi = tx.getInput(i);
            final UTXO txiUtxo = new UTXO(txi.prevTxHash, txi.outputIndex);
            final Transaction.Output txio = mUtxoPool.getTxOutput(txiUtxo);

            // all outputs claimed by tx are in the current UTXO pool
            if (txio == null) {
                log("tx not found in UTXO pool");
                return false;
            }

            // the signatures on each input of tx are valid
            final byte[] rawDataToSign = tx.getRawDataToSign(i);
            if (rawDataToSign == null) {
                log("getRawDataToSign returns null");
                return false;
            }
            
            if (!Crypto.verifySignature(txio.address, rawDataToSign, txi.signature)) {
                log("signature doesn't match");
                return false;
            }
            
            // no UTXO is claimed multiple times by tx
            if (!utxoSet.add(txiUtxo)) {
                System.out.println("double txiUtxo");
                return false;
            }
            
            sumInputValues += txio.value;
            
        }
        System.out.println("sum of input values = " + sumInputValues);

        double sumOutputValues = 0d;
        for (int i = 0; i < tx.numOutputs(); i++) {
            
            // all of txs output values are non-negative
            final Transaction.Output txo = tx.getOutput(i);
            if (txo.value < 0d) {
                log("double-counted txiUtxo");
                return false;
            }
            
            sumOutputValues += txo.value;
            
        }
        System.out.println("sum of output values = " + sumOutputValues);
        
        // sum of txs input values is greater than or equal to the sum of its output values
        if (sumInputValues < sumOutputValues) {
            log("sum of input less than sum of output values");
            return false;
        }
        
        log("tx is valid");
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(final Transaction[] possibleTxs) {
        
        final Set<Transaction> validTxs = new HashSet<>(possibleTxs.length);
        for (final Transaction ptx : possibleTxs) {
            if (!isValidTx(ptx) || validTxs.contains(ptx)) {
                // ignore invalid or duplicate transactions
                continue;
            }
            // remove "consumed" ledger entries
            for (final Transaction.Input txi : ptx.getInputs()) {
                mUtxoPool.removeUTXO(new UTXO(txi.prevTxHash, txi.outputIndex));
            }
            // add the new (validated) transaction to the ledger
            for (int i = 0; i < ptx.numOutputs(); i++) {
                final Transaction.Output txo = ptx.getOutput(i);
                mUtxoPool.addUTXO(new UTXO(ptx.getHash(), i), txo);
            }
            validTxs.add(ptx);
        }
        
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }
    
    
    //
    //  Private class methods
    //
    
    /**
     *  @param message message to log (if logging enabled)
     */
    private static void log(String message) {
        System.out.println(message);
    }

}

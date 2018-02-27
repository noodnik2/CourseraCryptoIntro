import java.util.ArrayList;

import javax.xml.bind.DatatypeConverter;

public class CryptoFormatter {

    public static String toString(UTXOPool utxoPool) {
        final StringBuilder sb = new StringBuilder();
        for (final UTXO utxo : utxoPool.getAllUTXO()) {
            final Transaction.Output output = utxoPool.getTxOutput(utxo);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(
                String.format(
                    "(I:%s,TxH:%s,V:%s,K:%x)", 
                    utxo.getIndex(), 
                    toString(utxo.getTxHash()), 
                    output.value, 
                    output.address.hashCode()
                )
            );
        }
        return sb.toString();
    }

    public static String itoString(ArrayList<Transaction.Input> inputs) {
        final StringBuilder sb = new StringBuilder();
        for (final Transaction.Input ti : inputs) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(String.format("I(pTxH:%s, S:%s)", toString(ti.prevTxHash), toString(ti.signature)));
        }
        return sb.toString();
    }

    public static String otoString(ArrayList<Transaction.Output> outputs) {
        final StringBuilder sb = new StringBuilder();
        for (final Transaction.Output to : outputs) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(String.format("O(V:%s, A:%x)", to.value, to.address.hashCode()));
        }
        return sb.toString();
    }

    public static String toString(Transaction tx) {
        return String.format("Tx(TxH:%s,I:{%s},O:{%s})", toString(tx.getHash()), itoString(tx.getInputs()), otoString(tx.getOutputs()));
    }

    public static String toString(Transaction[] txs) {
        final StringBuilder sb = new StringBuilder();
        for (final Transaction tx : txs) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(toString(tx));
        }
        return sb.toString();
    }
    
    public static String toString(byte[] ba) {
        return Integer.toHexString(DatatypeConverter.printHexBinary(ba).hashCode());
    }

}

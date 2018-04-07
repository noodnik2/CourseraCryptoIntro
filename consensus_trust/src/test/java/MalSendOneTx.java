import java.util.Set;

/**
 *  Repeatedly sends only the first of the proposed transactions during each round
 *  @author Marty Ross
 */
public class MalSendOneTx implements Node {

    private Transaction pendingTx;

    public void setFollowees(boolean[] followees) {
        // does nothing
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        if (pendingTransactions == null || pendingTransactions.size() == 0) {
            throw new IllegalStateException();
        }
        pendingTx = pendingTransactions.iterator().next();
    }

    public Set<Transaction> sendToFollowers() {
        if (pendingTx == null) {
            throw new IllegalStateException();
        }
        return Set.of(pendingTx);
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // does nothing
    }

}

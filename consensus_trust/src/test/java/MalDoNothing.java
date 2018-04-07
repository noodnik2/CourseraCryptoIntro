import java.util.Collections;
import java.util.Set;

public class MalDoNothing implements Node {

    public void setFollowees(boolean[] followees) {
        // nothing to do
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // nothing to do
    }

    public Set<Transaction> sendToFollowers() {
        return Collections.emptySet();
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // nothing to do
    }
}

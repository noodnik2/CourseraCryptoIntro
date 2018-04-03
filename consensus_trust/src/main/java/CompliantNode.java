import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/*
 *  CompliantNode refers to a node that follows the rules (i.e., is not malicious)
 */
public class CompliantNode implements Node {

    private boolean[] followees = null;
    private Set<Transaction> pendingTransactions = null;

    private int nTxIgnoredFromFollowee = 0;
    private int receiveRoundNo = 0;
    private int sendRoundNo = 0;

    /**
     *  @param p_graph parameter for random graph: prob. that an edge will exist
     *  @param p_malicious prob. that a node will be set to be malicious
     *  @param p_txDistribution probability of assigning an initial transaction to each node
     *  @param numRounds number of simulation rounds your nodes will run for
     */
    public CompliantNode(
        final double p_graph,
        final double p_malicious,
        final double p_txDistribution,
        final int numRounds
    ) {
        log(String.format("%02X: CompliantNode()", hashCode()));
        printState();
    }

    @Override
    public void setFollowees(final boolean[] followees) {
        assertNotNullArgument("followees", followees);
        if (this.followees != null) {
            throw new UnsupportedOperationException("attempt to update followees");
        }
        this.followees = followees.clone();
        log(String.format("%02X: setFollowees(%s)", hashCode(), count(this.followees)));
        printState();
    }

    @Override
    public void setPendingTransaction(final //        log(
//            String.format(
//                "nSendToFollowers(%s), nReceiveFromFollowees(%s)",
//                nSendToFollowers,
//                nReceiveFromFollowees
//            )
//        );
Set<Transaction> pendingTransactions) {
        assertNotNullArgument("followees", followees);
        if (this.pendingTransactions != null) {
            throw new UnsupportedOperationException("attempt to update pending transactions");
        }
        this.pendingTransactions = new HashSet<>(pendingTransactions);
        log(String.format("%02X: setPendingTransaction(%s)", hashCode(), pendingTransactions.size()));
        printState();
    }

    @Override
    public Set<Transaction> sendToFollowers() {
        assertNotNullArgument("followees", followees);
        sendRoundNo++;
        log(String.format("%02X.%s: sendToFollowers()", hashCode(), sendRoundNo));
        printState();
        return Collections.unmodifiableSet(pendingTransactions);
    }

    @Override
    public void receiveFromFollowees(final Set<Candidate> candidates) {
        assertNotNullArgument("candidates", candidates);
        receiveRoundNo++;
        log(
            String.format(
                "%02X.%s: receiveFromFollowees(%s) vs pendingTransactions(%s),nTxIgnoredFromFollowee(%s)",
                hashCode(),
                receiveRoundNo,
                candidates.size(),
                pendingTransactions.size(),
                nTxIgnoredFromFollowee
            )
        );
        for (final Candidate c : candidates) {
            if (!followees[c.sender]) {
                log(String.format("ignored tx(%s) from(%s); not following", c.tx.id, c.sender));
                continue;
            }
            if (pendingTransactions.add(c.tx)) {
//                log(String.format("ignored tx(%s) from(%s); already have it", c.tx.id, c.sender));
                nTxIgnoredFromFollowee++;
            }
        }
        printState();
    }


    //
    //  Private instance methods
    //

    private void printState() {
        // does nothing now
    }


    //
    //  Private class methods
    //

    private static void assertNotNullArgument(final String inArgName, final Object argValue) {
        if (argValue == null) {
            throw new IllegalArgumentException(inArgName);
        }
    }

    private static void log(final String message) {
        System.out.println(message);
    }

    private static int count(final boolean[] values) {
        int c = 0;
        for (final boolean value : values) {
            if (value) {
                c++;
            }
        }
        return c;
    }

}

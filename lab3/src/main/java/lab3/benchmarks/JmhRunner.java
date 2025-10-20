package lab3.benchmarks;

public final class JmhRunner {

    private static final String DEFAULT_INCLUDE = "ReceiptStatisticsBenchmark";

    private JmhRunner() {
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty("jmh.separateClasspathInstance") == null) {
            System.setProperty("jmh.separateClasspathInstance", "false");
        }
        if (args == null || args.length == 0) {
            org.openjdk.jmh.Main.main(new String[] { DEFAULT_INCLUDE });
        } else {
            org.openjdk.jmh.Main.main(args);
        }
    }
}

package newsaggre;

import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Restricts what classes ObjectInputStream is willing to reconstruct when reading this app's own
 * local data files (~/.newsaggre/*.ser). Without this, deserializing ANY .ser file - even one an
 * attacker substituted at that path - can potentially be abused to run arbitrary code via a "gadget
 * chain" built from classes already on the classpath. This app ships zero third-party dependencies,
 * which already rules out the common public gadget chains, but this filter closes the door regardless:
 * only this app's own model classes (newsaggre.*) and the core java.lang/java.util classes they're
 * built from are allowed to be reconstructed. Everything else - including JDK-bundled packages known
 * to provide gadget-chain primitives, like javax.naming and com.sun.org.apache.xalan - is rejected
 * before any code from it can run.
 */
public final class SerializationSafety {

    private SerializationSafety() {}

    private static final String ALLOWLIST =
            "newsaggre.*;" +
            "java.lang.*;java.util.*;" +
            "!*"; // reject anything not explicitly allowed above (blocks javax.naming, java.rmi,
                  // com.sun.* internal classes, and any other known deserialization gadget sources)

    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(ALLOWLIST);

    /** Applies the allowlist filter to an ObjectInputStream. Call before any readObject(). */
    public static void harden(ObjectInputStream ois) {
        ois.setObjectInputFilter(FILTER);
    }
}

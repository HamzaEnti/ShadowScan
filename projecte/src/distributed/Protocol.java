package distributed;

/**
 * Protocol minimalista entre coordinator i workers.
 *
 * Format: una línia JSON per missatge.
 *
 * Coordinator → Worker:
 *   {"type":"scan", "ips":["10.0.0.1","..."], "mode":"PARCIAL", "udp":false}
 *   {"type":"ping"}
 *
 * Worker → Coordinator:
 *   {"type":"result", "host": &lt;ResultatHost JSON&gt;}
 *   {"type":"done"}
 *   {"type":"pong"}
 *   {"type":"error", "message":"..."}
 */
public final class Protocol {
    public static final int DEFAULT_PORT = 9876;
    private Protocol() {}
}

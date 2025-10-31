package ai.deeprunner.indexer.rabbit;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Spring Cloud Stream functional programming model
 * Compatible with Java 21 and Spring Cloud Stream 4.x
 */
@Component
public class CommandProducer {
    
    public static final String CREATE_OUTPUT = "documentCreate-out-0";
    public static final String UPDATE_OUTPUT = "documentUpdate-out-0";
    public static final String DELETE_OUTPUT = "documentDelete-out-0";
    
    private final StreamBridge streamBridge;
    
    public CommandProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }
    
    public void sendCreateMessage(Object message) {
        streamBridge.send(CREATE_OUTPUT, message);
    }
    
    public void sendUpdateMessage(Object message) {
        streamBridge.send(UPDATE_OUTPUT, message);
    }
    
    public void sendDeleteMessage(Object message) {
        streamBridge.send(DELETE_OUTPUT, message);
    }
}


package ai.deeprunner.indexer.controller;

import ai.deeprunner.core.ratelimit.Ratelimit;
import ai.deeprunner.core.service.ThreadLocalTenantResolver;
import ai.deeprunner.indexer.command.*;
import ai.deeprunner.indexer.command.handler.CommandInvoker;
import ai.deeprunner.indexer.command.handler.CommandResult;
import ai.deeprunner.indexer.service.DocumentIndexService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentCommandController {
    
    private final CommandInvoker commandInvoker;
    private final DocumentIndexService documentIndexService;

    @Ratelimit(name="ingestion", permitsPerSecond = 2, burstCapacity=20)
    @PostMapping("/{documentType}/{documentId}")
    public ResponseEntity<Map<String, Object>> createDocument(@PathVariable(name = "documentType") String documentType,@PathVariable(name = "documentId") String documentId, @RequestBody Map<String, Object> document) {
        CreateDocumentCommand command = new CreateDocumentCommand();
        command.setDocumentId(documentId);
        command.setTenantId(ThreadLocalTenantResolver.getCurrentTenant());
        command.setDocument(document);
        command.setDocumentType(documentType);
        CommandResult result = commandInvoker.executeCommand(command);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("documentId", command.getDocumentId());
        response.put("tenantId", command.getTenantId());
        response.put("message", result.getMessage());
        
        return ResponseEntity.ok(response);
    }

    @Ratelimit(name="ingestion", permitsPerSecond = 2, burstCapacity=20)
    @PutMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> updateDocument(
            @PathVariable String documentId,
            @RequestParam String tenantId,
            @RequestBody UpdateDocumentCommand command) {

        command.setTenantId(ThreadLocalTenantResolver.getCurrentTenant());
        command.setDocumentId(documentId);

        CommandResult result = commandInvoker.executeCommand(command);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("documentId", documentId);
        response.put("message", result.getMessage());
        
        return ResponseEntity.ok(response);
    }

    @SneakyThrows
    @Ratelimit(name="deletion", permitsPerSecond = 1, burstCapacity=5)
    @DeleteMapping("/{documentType}/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable(name = "documentType") String documentType,
            @PathVariable(name = "documentId") String documentId
            ) {
        String tenantId = ThreadLocalTenantResolver.getCurrentTenant();
        documentIndexService.deleteDocument(tenantId, documentType, documentId);


        Map<String, Object> response = new HashMap<>();
        response.put("status",  "SUCCESS");
        response.put("documentId", documentId);

        return ResponseEntity.ok(response);
    }
    
}



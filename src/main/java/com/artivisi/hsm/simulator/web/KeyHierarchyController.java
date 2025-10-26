package com.artivisi.hsm.simulator.web;

import com.artivisi.hsm.simulator.entity.MasterKey;
import com.artivisi.hsm.simulator.repository.MasterKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/keys/hierarchy")
public class KeyHierarchyController {

    private final MasterKeyRepository masterKeyRepository;

    @GetMapping
    public String showHierarchy(Model model) {
        List<MasterKey> allKeys = masterKeyRepository.findAll();

        // Generate Mermaid diagram syntax
        String mermaidDiagram = generateMermaidDiagram(allKeys);

        model.addAttribute("mermaidDiagram", mermaidDiagram);
        model.addAttribute("totalKeys", allKeys.size());

        return "keys/hierarchy";
    }

    private String generateMermaidDiagram(List<MasterKey> keys) {
        StringBuilder diagram = new StringBuilder();
        diagram.append("graph TD\n");

        for (MasterKey key : keys) {
            String nodeId = sanitizeNodeId(key.getId().toString());
            String label = formatKeyLabel(key);
            String nodeClass = getNodeClass(key);

            // Define node with styling
            diagram.append("    ").append(nodeId)
                   .append("[\"").append(label).append("\"]:::")
                   .append(nodeClass).append("\n");

            // Add parent relationship if exists
            if (key.getParentKeyId() != null) {
                String parentNodeId = sanitizeNodeId(key.getParentKeyId().toString());
                diagram.append("    ").append(parentNodeId)
                       .append(" --> ").append(nodeId).append("\n");
            }

            // Add rotation relationship if exists
            if (key.getRotatedFromKeyId() != null) {
                String rotatedFromNodeId = sanitizeNodeId(key.getRotatedFromKeyId().toString());
                diagram.append("    ").append(rotatedFromNodeId)
                       .append(" -.rotated to.-> ").append(nodeId).append("\n");
            }
        }

        // Add style classes
        diagram.append("\n    classDef activeKey fill:#10b981,stroke:#059669,stroke-width:2px,color:#fff\n");
        diagram.append("    classDef revokedKey fill:#ef4444,stroke:#dc2626,stroke-width:2px,color:#fff\n");
        diagram.append("    classDef rotatedKey fill:#f59e0b,stroke:#d97706,stroke-width:2px,color:#fff\n");
        diagram.append("    classDef expiredKey fill:#6b7280,stroke:#4b5563,stroke-width:2px,color:#fff\n");

        return diagram.toString();
    }

    private String formatKeyLabel(MasterKey key) {
        String keyId = key.getMasterKeyId();
        if (keyId.length() > 30) {
            keyId = keyId.substring(0, 30) + "...";
        }
        return String.format("<b>%s</b><br/>%s<br/><i>%s</i>",
            key.getKeyType(),
            keyId,
            key.getStatus().name()
        );
    }

    private String getNodeClass(MasterKey key) {
        return switch (key.getStatus()) {
            case ACTIVE -> "activeKey";
            case REVOKED -> "revokedKey";
            case ROTATED -> "rotatedKey";
            case EXPIRED -> "expiredKey";
        };
    }

    private String sanitizeNodeId(String id) {
        return "node_" + id.replace("-", "_");
    }
}

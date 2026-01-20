package com.yantai.superinventory.controller;

import com.yantai.superinventory.model.Contract;
import com.yantai.superinventory.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contracts")
@CrossOrigin(origins = "*")
public class ContractController {
    
    @Autowired
    private ContractService contractService;
    
    /**
     * 生成合同
     */
    @PostMapping("/generate/{orderId}")
    public ResponseEntity<Contract> generateContract(@PathVariable String orderId) {
        try {
            Contract contract = contractService.generateContract(orderId);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取订单合同
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Contract> getContractByOrderId(@PathVariable String orderId) {
        try {
            Contract contract = contractService.getContractByOrderId(orderId);
            if (contract == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 预览合同（返回合同数据）
     */
    @GetMapping("/preview/{orderId}")
    public ResponseEntity<Contract> previewContract(@PathVariable String orderId) {
        try {
            Contract contract = contractService.getContractByOrderId(orderId);
            if (contract == null) {
                // 如果没有合同，自动生成
                contract = contractService.generateContract(orderId);
            }
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 下载Word合同
     */
    @GetMapping("/download/word/{orderId}")
    public ResponseEntity<byte[]> downloadWordContract(@PathVariable String orderId) {
        try {
            byte[] document = contractService.generateWordContract(orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "contract_" + orderId + ".docx");
            headers.setContentLength(document.length);
            
            return new ResponseEntity<>(document, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 下载PDF合同
     */
    @GetMapping("/download/pdf/{orderId}")
    public ResponseEntity<byte[]> downloadPdfContract(@PathVariable String orderId) {
        try {
            byte[] document = contractService.generatePdfContract(orderId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "contract_" + orderId + ".pdf");
            headers.setContentLength(document.length);
            
            return new ResponseEntity<>(document, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 确认合同
     */
    @PostMapping("/confirm/{orderId}")
    public ResponseEntity<Contract> confirmContract(@PathVariable String orderId) {
        try {
            Contract contract = contractService.confirmContract(orderId);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 更新合同状态
     */
    @PatchMapping("/{contractId}/status")
    public ResponseEntity<Contract> updateContractStatus(
            @PathVariable String contractId,
            @RequestParam String status) {
        try {
            Contract contract = contractService.updateContractStatus(contractId, status);
            return ResponseEntity.ok(contract);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

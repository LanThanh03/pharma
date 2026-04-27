package com.nckh.dia5.service;

import com.nckh.dia5.config.BlockchainConfig;
import com.nckh.dia5.util.BlockchainEncodingFixer;
import com.nckh.dia5.util.SafeFunctionEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service tương tác với PharmaLedgerOptimized Smart Contract
 * Đã được tối ưu hóa để giảm số lượng transaction và gas fee.
 * 
 * QUY TRÌNH CHUẨN:
 * 1. Tạo lô (Manufacturer): 1 Tx (createBatchWithItems)
 * 2. Chuyển hàng (Sender): 1 Tx (createAndDispatchShipment)
 * 3. Nhận hàng (Receiver): 1 Tx (receiveShipment)
 * 4. Bán lẻ (Pharmacy): 1 Tx (sellItem)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final BlockchainConfig blockchainConfig;
    private final BlockchainEncodingFixer encodingFixer;
    private final SafeFunctionEncoder safeFunctionEncoder;

    // ============================================================
    // 1. MANUFACTURER: TẠO LÔ THUỐC (1 Tx)
    // ============================================================
    
    public CompletableFuture<TransactionReceipt> createBatchWithItems(
            String drugName,
            String manufacturerName,
            BigInteger quantity,
            BigInteger manufactureDate,
            BigInteger expiryDate,
            String itemsMerkleRoot) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating batch on blockchain: drugName={}, quantity={}, merkleRoot={}", 
                         drugName, quantity, itemsMerkleRoot);

                // 1. Prepare DrugInfo struct
                List<Type> drugInfoParams = Arrays.asList(
                    new Utf8String(encodingFixer.cleanForBlockchain(drugName)),
                    new Utf8String(""), // activeIngredient
                    new Utf8String(""), // dosage
                    new Utf8String(encodingFixer.cleanForBlockchain(manufacturerName)),
                    new Utf8String("")  // registrationNumber
                );
                DynamicStruct drugInfo = new DynamicStruct(drugInfoParams);

                // 2. Prepare function params
                List<Type> inputParameters = Arrays.asList(
                    drugInfo,
                    new Uint256(quantity),
                    new Uint256(manufactureDate),
                    new Uint256(expiryDate),
                    new Bytes32(encodingFixer.safeHexToBytes(itemsMerkleRoot)),
                    new Utf8String(encodingFixer.cleanForBlockchain(manufacturerName))
                );

                Function function = new Function(
                    "createBatchWithItems",
                    inputParameters,
                    Arrays.asList(new TypeReference<Uint256>() {})
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to create batch on blockchain", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Alias for legacy code
    public CompletableFuture<TransactionReceipt> issueBatch(
            String drugName,
            String manufacturerName,
            String batchNumber,
            BigInteger quantity,
            BigInteger expiryDate,
            String storageConditions) {
        
        // Use current time as manufacture date
        BigInteger manufactureDate = BigInteger.valueOf(System.currentTimeMillis() / 1000);
        
        // Use a dummy merkle root (valid 32 bytes hex)
        // In production, this should be calculated from actual items
        String dummyRoot = "0x0000000000000000000000000000000000000000000000000000000000000001";
        
        return createBatchWithItems(
            drugName,
            manufacturerName,
            quantity,
            manufactureDate,
            expiryDate,
            dummyRoot
        );
    }

    // ============================================================
    // 2. SENDER: TẠO VÀ GỬI SHIPMENT (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> createAndDispatchShipment(
            BigInteger batchId,
            String toAddress,
            String fromLocation,
            String toLocation,
            BigInteger quantity,
            String trackingNumber,
            String notes) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Dispatching shipment: batchId={}, to={}, quantity={}", batchId, toAddress, quantity);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Address(toAddress),
                    new Utf8String(encodingFixer.cleanForBlockchain(fromLocation)),
                    new Utf8String(encodingFixer.cleanForBlockchain(toLocation)),
                    new Utf8String("UNKNOWN"), // toLocationType (deprecated but kept for signature)
                    new Uint256(quantity),
                    new Utf8String(encodingFixer.cleanForBlockchain(trackingNumber)),
                    new Utf8String(encodingFixer.cleanForBlockchain(notes))
                );

                Function function = new Function(
                    "createAndDispatchShipment",
                    inputParameters,
                    Arrays.asList(new TypeReference<Uint256>() {})
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to dispatch shipment", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Alias for createAndDispatchShipment to support legacy calls
    public CompletableFuture<TransactionReceipt> createShipment(
            BigInteger batchId,
            String toAddress,
            BigInteger quantity,
            String trackingNumber) {
        // Use default values for missing parameters
        return createAndDispatchShipment(
            batchId, 
            toAddress, 
            "Unknown Location", // fromLocation
            "Unknown Location", // toLocation
            quantity, 
            trackingNumber, 
            "" // notes
        );
    }

    // ============================================================
    // 3. RECEIVER: NHẬN HÀNG (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> receiveShipment(
            BigInteger shipmentId,
            String receiverLocationName) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Receiving shipment: shipmentId={}, location={}", shipmentId, receiverLocationName);

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(shipmentId),
                    new Utf8String(encodingFixer.cleanForBlockchain(receiverLocationName))
                );

                Function function = new Function(
                    "receiveShipment",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to receive shipment", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // Overload for legacy calls
    public CompletableFuture<TransactionReceipt> receiveShipment(BigInteger shipmentId) {
        return receiveShipment(shipmentId, "Unknown Location");
    }

    // ============================================================
    // 4. PHARMACY: BÁN LẺ (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> sellItem(
            BigInteger batchId,
            String itemCode,
            List<String> merkleProof) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Selling item: batchId={}, itemCode={}", batchId, itemCode);

                // Convert proof to Bytes32 list
                List<Bytes32> proofBytes = merkleProof.stream()
                        .map(p -> new Bytes32(encodingFixer.safeHexToBytes(p)))
                        .toList();

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Utf8String(itemCode),
                    new DynamicArray<>(Bytes32.class, proofBytes)
                );

                Function function = new Function(
                    "sellItem",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to sell item", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // ============================================================
    // 5. UPDATE ITEM STATUS (DAMAGED/RECALL) (1 Tx)
    // ============================================================

    public CompletableFuture<TransactionReceipt> updateItemStatus(
            BigInteger batchId,
            String itemCode,
            BigInteger newStatus,
            String reason,
            List<String> merkleProof) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Updating item status: batchId={}, itemCode={}, newStatus={}, reason={}", 
                         batchId, itemCode, newStatus, reason);

                // Convert proof to Bytes32 list
                List<Bytes32> proofBytes = merkleProof.stream()
                        .map(p -> new Bytes32(encodingFixer.safeHexToBytes(p)))
                        .toList();

                List<Type> inputParameters = Arrays.asList(
                    new Uint256(batchId),
                    new Utf8String(itemCode),
                    new org.web3j.abi.datatypes.generated.Uint8(newStatus), // ItemStatus is an enum (uint8)
                    new Utf8String(encodingFixer.cleanForBlockchain(reason)),
                    new DynamicArray<>(Bytes32.class, proofBytes)
                );

                Function function = new Function(
                    "updateItemStatus",
                    inputParameters,
                    Arrays.asList()
                );

                return executeTransaction(function);

            } catch (Exception e) {
                log.error("Failed to update item status", e);
                throw new RuntimeException("Blockchain transaction failed", e);
            }
        });
    }

    // ============================================================
    // READ-ONLY & HELPER METHODS
    // ============================================================

    public CompletableFuture<Boolean> verifyOwnership(BigInteger batchId, String address) {
        return CompletableFuture.completedFuture(true);
    }

    public List<Map<String, Object>> getShipmentHistory(BigInteger shipmentId) {
        return new ArrayList<>();
    }

    public Map<String, Object> getShipmentDetails(BigInteger shipmentId) {
        return new HashMap<>();
    }

    public CompletableFuture<TransactionReceipt> dispatchShipment(BigInteger shipmentId, String location, String notes) {
         // Placeholder: In real impl, this might call createAndDispatchShipment if arguments were available
         // or a specific dispatch function if the contract supported it.
         return CompletableFuture.completedFuture(new TransactionReceipt());
    }

    public CompletableFuture<TransactionReceipt> updateShipmentStatus(
            BigInteger shipmentId, 
            BigInteger status, 
            String location, 
            String notes) {
        return CompletableFuture.completedFuture(new TransactionReceipt());
    }

    public String getItemStatus(String itemCode) {
        return "UNKNOWN";
    }

    public String getItemStatus(BigInteger batchId, String itemCode) {
        return "UNKNOWN";
    }

    public String getContractAddress() {
        return blockchainConfig.getContractAddress();
    }

    public BigInteger getLatestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            log.error("Failed to get latest block number", e);
            return BigInteger.ZERO;
        }
    }

    public Optional<TransactionReceipt> getTransactionReceipt(String txHash) {
        try {
            return web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
        } catch (Exception e) {
            log.error("Failed to get transaction receipt", e);
            return Optional.empty();
        }
    }

    public boolean isTransactionSuccessful(String txHash) {
        return getTransactionReceipt(txHash)
                .map(r -> "0x1".equals(r.getStatus()))
                .orElse(false);
    }

    private TransactionReceipt executeTransaction(Function function) throws Exception {
        String contractAddress = blockchainConfig.getContractAddress();
        String encodedFunction = safeFunctionEncoder.safeEncode(function);

        TransactionManager txManager = new RawTransactionManager(web3j, credentials, blockchainConfig.getChainId());
        
        org.web3j.protocol.core.methods.response.EthSendTransaction response = 
            txManager.sendTransaction(
                gasProvider.getGasPrice(),
                gasProvider.getGasLimit(),
                contractAddress,
                encodedFunction,
                BigInteger.ZERO
            );

        if (response.hasError()) {
            throw new RuntimeException("Tx failed: " + response.getError().getMessage());
        }

        // Wait for receipt
        String txHash = response.getTransactionHash();
        for (int i = 0; i < 30; i++) {
            Optional<TransactionReceipt> receipt = web3j.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();
            if (receipt.isPresent()) return receipt.get();
            Thread.sleep(1000);
        }
        throw new RuntimeException("Tx receipt timeout");
    }

    private String callFunction(Function function) throws Exception {
        String contractAddress = blockchainConfig.getContractAddress();
        String encodedFunction = safeFunctionEncoder.safeEncode(function);
        
        org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
            org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(
                credentials.getAddress(), contractAddress, encodedFunction),
            DefaultBlockParameterName.LATEST
        ).send();
        
        return response.getValue();
    }
}
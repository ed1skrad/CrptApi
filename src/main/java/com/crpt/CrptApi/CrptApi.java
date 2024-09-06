package com.crpt.CrptApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.*;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CrptApi {

    private static final Logger logger = Logger.getLogger(CrptApi.class.getName());

    private final int requestLimit;
    private final AtomicInteger requestCount;
    private final Semaphore semaphore;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::resetRequestCount, timeUnit.toMillis(1), timeUnit.toMillis(1), TimeUnit.MILLISECONDS);
    }

    public void createDocument(Document document, String signature){
        if (requestCount.get() >= requestLimit) {
            logger.info("Request limit reached. Waiting for the limit to reset...");
            return;
        }

        try {
            semaphore.acquire();
            requestCount.incrementAndGet();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while waiting for semaphore", e);
            Thread.currentThread().interrupt();
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(document);
            RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url("https://ismp.crpt.ru/api/v3/lk/documents/create")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Signature", signature)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                assert response.body() != null;
                logger.info("Response: " + response.body().string());
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "An error occurred while creating the document", e);
            System.out.println("An error occurred while creating the document. Continuing...");
        } finally {
            semaphore.release();
        }
    }

    private void resetRequestCount() {
        requestCount.set(0);
        logger.info("Request count reset to 0");
    }

    @Data
    @NoArgsConstructor
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private Product[] products;
        private String regDate;
        private String regNumber;

        @Data
        @NoArgsConstructor
        public static class Description {
            private String participantInn;
        }

        @Data
        @NoArgsConstructor
        public static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Do you want to execute the request? (yes/no):");
            String response = scanner.nextLine();

            if ("yes".equalsIgnoreCase(response)) {
                Document document = new Document();
                Document.Description description = new Document.Description();
                description.setParticipantInn("1234567890");
                document.setDescription(description);
                document.setDocId("doc123");
                document.setDocStatus("active");
                document.setDocType("type1");
                document.setImportRequest(false);
                document.setOwnerInn("0987654321");
                document.setParticipantInn("1234567890");
                document.setProducerInn("1122334455");
                document.setProductionDate("2023-10-01");
                document.setProductionType("type");
                document.setRegDate("2023-10-01");
                document.setRegNumber("reg123");

                String signature = "signature123";

                crptApi.createDocument(document, signature);
            } else {
                System.out.println("Request not executed. Exiting the program.");
                break;
            }
        }

        scanner.close();
        crptApi.scheduler.shutdown();
    }
}

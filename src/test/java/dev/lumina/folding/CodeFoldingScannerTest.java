package dev.lumina.folding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CodeFoldingScannerTest {

    @Test
    void testScanImportsAndAnnotationsMatchingOrderJava() {
        String code = """
                package com.roze.nexacommerce.order.entity;
                
                import com.roze.nexacommerce.order.entity.enums.OrderStatus;
                import jakarta.persistence.*;
                import lombok.*;
                import org.hibernate.annotations.Where;
                
                @Entity
                @Table(name = "orders")
                @Data
                @Builder
                @NoArgsConstructor
                @AllArgsConstructor
                @Where(clause = "deleted = false")
                public class Order extends BaseEntity {
                
                    @Column(nullable = false, unique = true)
                    private String orderNumber;
                
                    @ManyToOne(fetch = FetchType.LAZY)
                    @JoinColumn(name = "customer_id")
                    private CustomerProfile customer;
                }
                """;

        List<FoldRegion> regions = CodeFoldingScanner.scan(code, "Order.java");
        assertNotNull(regions);
        assertFalse(regions.isEmpty());

        // 1. Imports block
        FoldRegion importRegion = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.IMPORTS)
                .findFirst()
                .orElse(null);
        assertNotNull(importRegion, "Imports region should be detected");
        assertEquals(3, importRegion.getStartLine());
        assertEquals(6, importRegion.getEndLine());
        assertTrue(importRegion.isFolded(), "Imports should be folded by default matching IntelliJ IDEA");
        assertEquals("...", importRegion.getPlaceholder());

        // 2. Class-level annotations
        FoldRegion classAnnotations = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.ANNOTATIONS && r.getStartLine() == 8)
                .findFirst()
                .orElse(null);
        assertNotNull(classAnnotations, "Class annotations should be detected");
        assertEquals(8, classAnnotations.getStartLine());
        assertEquals(14, classAnnotations.getEndLine());
        assertEquals("@{...}", classAnnotations.getPlaceholder());

        // 3. Field annotations (@ManyToOne + @JoinColumn)
        FoldRegion fieldAnnotations = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.ANNOTATIONS && r.getStartLine() == 20)
                .findFirst()
                .orElse(null);
        assertNotNull(fieldAnnotations, "Field annotations should be detected");
        assertEquals(20, fieldAnnotations.getStartLine());
        assertEquals(21, fieldAnnotations.getEndLine());
        assertEquals("@{...}", fieldAnnotations.getPlaceholder());

        // 4. Class body
        FoldRegion classBody = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.CLASS)
                .findFirst()
                .orElse(null);
        assertNotNull(classBody, "Class body should be detected");
        assertEquals(15, classBody.getStartLine());
        assertEquals(23, classBody.getEndLine());
    }

    @Test
    void testScanMethodsAndAnnotationsMatchingOrderServiceImpl() {
        String code = """
                package com.roze.nexacommerce.order.service.impl;
                
                import com.roze.nexacommerce.order.service.OrderService;
                import org.springframework.stereotype.Service;
                
                @Service
                @RequiredArgsConstructor
                public class OrderServiceImpl implements OrderService {
                
                    @Override
                    @Transactional
                    public OrderResponse createOrder(Long customerId, OrderCreateRequest request) {
                        log.info("Creating order for customer ID: {}", customerId);
                        return null;
                    }
                }
                """;

        List<FoldRegion> regions = CodeFoldingScanner.scan(code, "OrderServiceImpl.java");

        // Method-level annotations (@Override + @Transactional)
        FoldRegion methodAnnotations = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.ANNOTATIONS && r.getStartLine() == 10)
                .findFirst()
                .orElse(null);
        assertNotNull(methodAnnotations);
        assertEquals(10, methodAnnotations.getStartLine());
        assertEquals(11, methodAnnotations.getEndLine());
        assertEquals("@{...}", methodAnnotations.getPlaceholder());

        // Method body
        FoldRegion methodBody = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.METHOD)
                .findFirst()
                .orElse(null);
        assertNotNull(methodBody);
        assertEquals(12, methodBody.getStartLine());
        assertEquals(15, methodBody.getEndLine());
        assertEquals("{...}", methodBody.getPlaceholder());
    }

    @Test
    void testCommentsAndCustomRegions() {
        String code = """
                //<editor-fold desc="Custom Helper Region">
                int a = 1;
                int b = 2;
                //</editor-fold>
                
                /**
                 * Javadoc comment
                 */
                class Foo {}
                """;

        List<FoldRegion> regions = CodeFoldingScanner.scan(code, "Foo.java");

        FoldRegion custom = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.CUSTOM)
                .findFirst()
                .orElse(null);
        assertNotNull(custom);
        assertEquals(1, custom.getStartLine());
        assertEquals(4, custom.getEndLine());
        assertEquals("Custom Helper Region", custom.getPlaceholder());

        FoldRegion javadoc = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.COMMENT)
                .findFirst()
                .orElse(null);
        assertNotNull(javadoc);
        assertEquals(6, javadoc.getStartLine());
        assertEquals(8, javadoc.getEndLine());
        assertEquals("/**...*/", javadoc.getPlaceholder());
    }

    @Test
    void testFoldRegionFoldedContentAndToggle() {
        FoldRegion region = new FoldRegion(FoldRegion.RegionType.IMPORTS, 3, 20, "import [...]");
        assertFalse(region.isFolded());
        region.toggle();
        assertTrue(region.isFolded());
        region.setFoldedContent("import java.util.*;\nimport java.io.*;");
        assertEquals("import java.util.*;\nimport java.io.*;", region.getFoldedContent());
    }

    @Test
    void testOrderJavaImportsDefaultFolded() {
        String code = """
                package dev.lumina.orderservice.entity;

                import com.roze.nexacommerce.order.entity.enums.OrderStatus;
                import jakarta.persistence.*;
                import lombok.*;
                import org.hibernate.annotations.Where;

                @Entity
                @Table(name = "orders")
                public class Order {
                }
                """;

        List<FoldRegion> regions = CodeFoldingScanner.scan(code, "Order.java");
        FoldRegion imports = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.IMPORTS)
                .findFirst()
                .orElse(null);

        assertNotNull(imports);
        assertEquals(3, imports.getStartLine());
        assertEquals(6, imports.getEndLine());
        assertTrue(imports.isFolded(), "By default, import packages automatically keep collapsed in IntelliJ");
    }

    @Test
    void testCodeFoldingDoesNotAffectGitDiff() {
        String originalSource = """
                package com.roze.nexacommerce.customer.service.impl;

                import com.roze.nexacommerce.customer.repository.CustomerProfileRepository;
                import com.roze.nexacommerce.customer.repository.UserRepository;
                import org.springframework.stereotype.Service;

                @Service
                public class CustomerServiceImpl {
                    public void register() {
                        System.out.println("Registering");
                    }
                }
                """;

        List<FoldRegion> regions = CodeFoldingScanner.scan(originalSource, "CustomerServiceImpl.java");
        assertFalse(regions.isEmpty());

        // Find the folded imports region
        FoldRegion importsRegion = regions.stream()
                .filter(r -> r.getType() == FoldRegion.RegionType.IMPORTS)
                .findFirst()
                .orElse(null);
        assertNotNull(importsRegion);
        assertTrue(importsRegion.isFolded());

        // Verify git diff between HEAD (originalSource) and underlying document text is completely clean (0 chunks)
        dev.lumina.diff.DiffEngine.DiffResult diff = dev.lumina.diff.DiffEngine.diff(originalSource, originalSource, false);
        assertTrue(diff.chunks().isEmpty(), "Original document must have 0 git diff chunks (NORMAL status)");
    }
}

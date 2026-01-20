package com.yantai.superinventory.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.yantai.superinventory.model.Contract;
import com.yantai.superinventory.model.Order;
import com.yantai.superinventory.model.OrderItem;
import com.yantai.superinventory.model.Customer;
import com.yantai.superinventory.repository.ContractRepository;
import com.yantai.superinventory.repository.OrderRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ContractService {
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    
    /**
     * 生成合同
     */
    @Transactional
    public Contract generateContract(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        // 检查是否已有合同
        return contractRepository.findByOrder(order)
            .orElseGet(() -> {
                Contract contract = new Contract();
                contract.setOrder(order);
                
                // 生成合同编号：HT + yyyyMMdd + 订单ID后6位
                String contractNumber = "HT" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                    orderId.substring(Math.max(0, orderId.length() - 6));
                contract.setContractNumber(contractNumber);
                
                // 设置卖方信息（固定）
                contract.setSellerName("上海燕泰实业有限公司");
                contract.setSellerAddress("上海市");
                contract.setSellerContact("燕泰实业");
                contract.setSellerPhone("021-XXXXXXXX");
                contract.setSellerTaxNumber("91310000XXXXXXXXXX");
                
                // 从客户信息设置买方信息
                Customer customer = order.getCustomer();
                if (customer != null) {
                    contract.setBuyerName(customer.getCompanyName() != null ? 
                        customer.getCompanyName() : customer.getName());
                    contract.setBuyerAddress(customer.getCompanyAddress() != null ? 
                        customer.getCompanyAddress() : customer.getAddress());
                    contract.setBuyerContact(customer.getName());
                    contract.setBuyerPhone(customer.getPhone());
                    contract.setBuyerTaxNumber(customer.getTaxNumber());
                }
                
                // 设置合同条款
                contract.setPaymentTerms("货到付款，款到发货");
                contract.setDeliveryTerms("卖方负责送货上门");
                contract.setSpecialTerms("商品验收后7日内如有质量问题，可退换货");
                
                // 设置交货日期（默认7天后）
                contract.setDeliveryDate(LocalDateTime.now().plusDays(7));
                
                return contractRepository.save(contract);
            });
    }
    
    /**
     * 获取订单合同
     */
    public Contract getContractByOrderId(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        return contractRepository.findByOrder(order)
            .orElse(null);
    }
    
    /**
     * 生成Word合同文档
     */
    public byte[] generateWordContract(String orderId) throws IOException {
        Contract contract = getOrCreateContract(orderId);
        Order order = contract.getOrder();
        
        XWPFDocument document = new XWPFDocument();
        
        // 标题
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("销售合同");
        titleRun.setBold(true);
        titleRun.setFontSize(22);
        titleRun.setFontFamily("宋体");
        
        // 合同编号
        XWPFParagraph contractNo = document.createParagraph();
        contractNo.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun contractNoRun = contractNo.createRun();
        contractNoRun.setText("合同编号：" + contract.getContractNumber());
        contractNoRun.setFontSize(12);
        contractNoRun.setFontFamily("宋体");
        
        // 空行
        document.createParagraph();
        
        // 合同双方
        addParagraph(document, "甲方（卖方）：" + contract.getSellerName(), false);
        addParagraph(document, "地址：" + (contract.getSellerAddress() != null ? contract.getSellerAddress() : ""), false);
        addParagraph(document, "联系人：" + (contract.getSellerContact() != null ? contract.getSellerContact() : ""), false);
        addParagraph(document, "电话：" + (contract.getSellerPhone() != null ? contract.getSellerPhone() : ""), false);
        
        document.createParagraph();
        
        addParagraph(document, "乙方（买方）：" + (contract.getBuyerName() != null ? contract.getBuyerName() : ""), false);
        addParagraph(document, "地址：" + (contract.getBuyerAddress() != null ? contract.getBuyerAddress() : ""), false);
        addParagraph(document, "联系人：" + (contract.getBuyerContact() != null ? contract.getBuyerContact() : ""), false);
        addParagraph(document, "电话：" + (contract.getBuyerPhone() != null ? contract.getBuyerPhone() : ""), false);
        
        document.createParagraph();
        
        // 商品明细表
        addParagraph(document, "一、商品明细", true);
        
        XWPFTable table = document.createTable();
        table.setWidth("100%");
        
        // 表头
        XWPFTableRow headerRow = table.getRow(0);
        headerRow.getCell(0).setText("序号");
        headerRow.addNewTableCell().setText("商品名称");
        headerRow.addNewTableCell().setText("数量");
        headerRow.addNewTableCell().setText("单价（元）");
        headerRow.addNewTableCell().setText("金额（元）");
        
        // 商品行
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            XWPFTableRow row = table.createRow();
            row.getCell(0).setText(String.valueOf(i + 1));
            row.getCell(1).setText(item.getProduct().getName());
            row.getCell(2).setText(item.getQuantity().toString());
            row.getCell(3).setText(item.getUnitPrice().toString());
            row.getCell(4).setText(item.getSubtotal().toString());
        }
        
        // 合计行
        XWPFTableRow totalRow = table.createRow();
        totalRow.getCell(0).setText("");
        totalRow.getCell(1).setText("合计");
        totalRow.getCell(2).setText("");
        totalRow.getCell(3).setText("");
        
        BigDecimal total = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalRow.getCell(4).setText(total.toString());
        
        document.createParagraph();
        
        // 合同条款
        addParagraph(document, "二、付款方式", true);
        addParagraph(document, contract.getPaymentTerms(), false);
        
        document.createParagraph();
        
        addParagraph(document, "三、交货方式", true);
        addParagraph(document, contract.getDeliveryTerms(), false);
        addParagraph(document, "交货日期：" + contract.getDeliveryDate().format(DATE_FORMATTER), false);
        
        document.createParagraph();
        
        addParagraph(document, "四、特殊条款", true);
        addParagraph(document, contract.getSpecialTerms(), false);
        
        document.createParagraph();
        document.createParagraph();
        
        // 签字栏
        addParagraph(document, "甲方（盖章）：__________________    日期：__________________", false);
        document.createParagraph();
        addParagraph(document, "乙方（盖章）：__________________    日期：__________________", false);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();
        
        return out.toByteArray();
    }
    
    /**
     * 生成PDF合同文档
     */
    public byte[] generatePdfContract(String orderId) throws DocumentException, IOException {
        Contract contract = getOrCreateContract(orderId);
        Order order = contract.getOrder();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        com.itextpdf.text.Document document = new com.itextpdf.text.Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        
        // 设置中文字体
        BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfChinese, 22, Font.BOLD);
        Font headerFont = new Font(bfChinese, 14, Font.BOLD);
        Font normalFont = new Font(bfChinese, 12, Font.NORMAL);
        Font smallFont = new Font(bfChinese, 10, Font.NORMAL);
        
        document.open();
        
        // 标题
        Paragraph title = new Paragraph("销售合同", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        document.add(title);
        
        // 合同编号
        Paragraph contractNo = new Paragraph("合同编号：" + contract.getContractNumber(), normalFont);
        contractNo.setAlignment(Element.ALIGN_RIGHT);
        contractNo.setSpacingAfter(20);
        document.add(contractNo);
        
        // 甲方信息
        document.add(new Paragraph("甲方（卖方）：" + contract.getSellerName(), normalFont));
        document.add(new Paragraph("地址：" + (contract.getSellerAddress() != null ? contract.getSellerAddress() : ""), normalFont));
        document.add(new Paragraph("联系人：" + (contract.getSellerContact() != null ? contract.getSellerContact() : ""), normalFont));
        document.add(new Paragraph("电话：" + (contract.getSellerPhone() != null ? contract.getSellerPhone() : ""), normalFont));
        document.add(new Paragraph(" ", normalFont));
        
        // 乙方信息
        document.add(new Paragraph("乙方（买方）：" + (contract.getBuyerName() != null ? contract.getBuyerName() : ""), normalFont));
        document.add(new Paragraph("地址：" + (contract.getBuyerAddress() != null ? contract.getBuyerAddress() : ""), normalFont));
        document.add(new Paragraph("联系人：" + (contract.getBuyerContact() != null ? contract.getBuyerContact() : ""), normalFont));
        document.add(new Paragraph("电话：" + (contract.getBuyerPhone() != null ? contract.getBuyerPhone() : ""), normalFont));
        document.add(new Paragraph(" ", normalFont));
        
        // 商品明细表
        Paragraph detailTitle = new Paragraph("一、商品明细", headerFont);
        detailTitle.setSpacingBefore(10);
        detailTitle.setSpacingAfter(10);
        document.add(detailTitle);
        
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        table.setSpacingAfter(10);
        
        // 表头
        addTableHeader(table, "序号", bfChinese);
        addTableHeader(table, "商品名称", bfChinese);
        addTableHeader(table, "数量", bfChinese);
        addTableHeader(table, "单价（元）", bfChinese);
        addTableHeader(table, "金额（元）", bfChinese);
        
        // 商品行
        List<OrderItem> items = (List<OrderItem>) order.getItems();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            addTableCell(table, String.valueOf(i + 1), bfChinese);
            addTableCell(table, item.getProduct().getName(), bfChinese);
            addTableCell(table, item.getQuantity().toString(), bfChinese);
            addTableCell(table, item.getUnitPrice().toString(), bfChinese);
            addTableCell(table, item.getSubtotal().toString(), bfChinese);
        }
        
        // 合计行
        BigDecimal total = items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        addTableCell(table, "", bfChinese);
        addTableCell(table, "合计", bfChinese);
        addTableCell(table, "", bfChinese);
        addTableCell(table, "", bfChinese);
        addTableCell(table, total.toString(), bfChinese);
        
        document.add(table);
        
        // 合同条款
        document.add(new Paragraph(" ", normalFont));
        document.add(new Paragraph("二、付款方式", headerFont));
        document.add(new Paragraph(contract.getPaymentTerms(), normalFont));
        document.add(new Paragraph(" ", normalFont));
        
        document.add(new Paragraph("三、交货方式", headerFont));
        document.add(new Paragraph(contract.getDeliveryTerms(), normalFont));
        document.add(new Paragraph("交货日期：" + contract.getDeliveryDate().format(DATE_FORMATTER), normalFont));
        document.add(new Paragraph(" ", normalFont));
        
        document.add(new Paragraph("四、特殊条款", headerFont));
        document.add(new Paragraph(contract.getSpecialTerms(), normalFont));
        document.add(new Paragraph(" ", normalFont));
        document.add(new Paragraph(" ", normalFont));
        
        // 签字栏
        document.add(new Paragraph("甲方（盖章）：__________________    日期：__________________", normalFont));
        document.add(new Paragraph(" ", normalFont));
        document.add(new Paragraph("乙方（盖章）：__________________    日期：__________________", normalFont));
        
        document.close();
        
        return out.toByteArray();
    }
    
    /**
     * 获取或创建合同
     */
    private Contract getOrCreateContract(String orderId) {
        Contract contract = getContractByOrderId(orderId);
        if (contract == null) {
            contract = generateContract(orderId);
        }
        return contract;
    }
    
    /**
     * 添加段落
     */
    private void addParagraph(XWPFDocument document, String text, boolean bold) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily("宋体");
        run.setFontSize(12);
        if (bold) {
            run.setBold(true);
        }
    }
    
    /**
     * 添加表格表头
     */
    private void addTableHeader(PdfPTable table, String text, BaseFont font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(font, 10, Font.BOLD)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * 添加表格单元格
     */
    private void addTableCell(PdfPTable table, String text, BaseFont font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(font, 10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        table.addCell(cell);
    }
    
    /**
     * 更新合同状态
     */
    @Transactional
    public Contract updateContractStatus(String contractId, String status) {
        Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new RuntimeException("合同不存在"));
        contract.setStatus(status);
        return contractRepository.save(contract);
    }
    
    /**
     * 确认合同
     */
    @Transactional
    public Contract confirmContract(String orderId) {
        Contract contract = getOrCreateContract(orderId);
        contract.setStatus("CONFIRMED");
        return contractRepository.save(contract);
    }
}

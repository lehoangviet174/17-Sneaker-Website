package com.vuhien.application.controller.admin;

import com.vuhien.application.entity.Order;
import com.vuhien.application.entity.Promotion;
import com.vuhien.application.entity.User;
import com.vuhien.application.exception.BadRequestException;
import com.vuhien.application.model.dto.OrderDetailDTO;
import com.vuhien.application.model.dto.OrderInfoDTO;
import com.vuhien.application.model.dto.ShortProductInfoDTO;
import com.vuhien.application.model.request.CreateOrderRequest;
import com.vuhien.application.model.request.UpdateDetailOrder;
import com.vuhien.application.model.request.UpdateStatusOrderRequest;
import com.vuhien.application.security.CustomUserDetails;
import com.vuhien.application.service.OrderService;
import com.vuhien.application.service.ProductService;
import com.vuhien.application.service.PromotionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static com.vuhien.application.config.Contant.*;

@Controller
@Slf4j
public class OrderController {
    private String xlsx = ".xlsx";
    private static final int BUFFER_SIZE = 4096;
    private static final String TEMP_EXPORT_DATA_DIRECTORY = "\\resources\\reports";
    private static final String EXPORT_DATA_REPORT_FILE_NAME = "Hoa_Don";

    @Autowired
    private ServletContext context;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private PromotionService promotionService;

    @GetMapping("/admin/orders")
    public String getListOrderPage(Model model,
                                   @RequestParam(defaultValue = "1") Integer page,
                                   @RequestParam(defaultValue = "", required = false) String id,
                                   @RequestParam(defaultValue = "", required = false) String name,
                                   @RequestParam(defaultValue = "", required = false) String phone,
                                   @RequestParam(defaultValue = "", required = false) String status,
                                   @RequestParam(defaultValue = "", required = false) String product) {

        //Lấy danh sách sản phẩm
        List<ShortProductInfoDTO> productList = productService.getListProduct();
        model.addAttribute("productList", productList);


        //Lấy danh sách đơn hàng
        Page<Order> orderPage = orderService.adminGetListOrders(id, name, phone, status, product, page);
        model.addAttribute("orderPage", orderPage.getContent());
        model.addAttribute("totalPages", orderPage.getTotalPages());
        model.addAttribute("currentPage", orderPage.getPageable().getPageNumber() + 1);

        return "admin/order/list";
    }

    @GetMapping("/admin/orders/create")
    public String createOrderPage(Model model) {

        //Get list product
        List<ShortProductInfoDTO> products = productService.getAvailableProducts();
        model.addAttribute("products", products);

        // Get list size
        model.addAttribute("sizeVn", SIZE_VN);

//        //Get list valid promotion
        List<Promotion> promotions = promotionService.getAllValidPromotion();
        model.addAttribute("promotions", promotions);
        return "admin/order/create";
    }

    @PostMapping("/api/admin/orders")
    public ResponseEntity<Object> createOrder(@Valid @RequestBody CreateOrderRequest createOrderRequest) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        Order order = orderService.createOrder(createOrderRequest, user.getId());
        return ResponseEntity.ok(order);
    }

    @GetMapping("/admin/orders/update/{id}")
    public String updateOrderPage(Model model, @PathVariable long id) {

        Order order = orderService.findOrderById(id);
        model.addAttribute("order", order);

        if (order.getStatus() == ORDER_STATUS) {
            // Get list product to select
            List<ShortProductInfoDTO> products = productService.getAvailableProducts();
            model.addAttribute("products", products);

            // Get list valid promotion
            List<Promotion> promotions = promotionService.getAllValidPromotion();
            model.addAttribute("promotions", promotions);
            if (order.getPromotion() != null) {
                boolean validPromotion = false;
                for (Promotion promotion : promotions) {
                    if (promotion.getCouponCode().equals(order.getPromotion().getCouponCode())) {
                        validPromotion = true;
                        break;
                    }
                }
                if (!validPromotion) {
                    promotions.add(new Promotion(order.getPromotion()));
                }
            }

            // Check size available
            boolean sizeIsAvailable = productService.checkProductSizeAvailable(order.getProduct().getId(), order.getSize());
            model.addAttribute("sizeIsAvailable", sizeIsAvailable);
        }

        return "admin/order/edit";
    }

    @PutMapping("/api/admin/orders/update-detail/{id}")
    public ResponseEntity<Object> updateDetailOrder(@Valid @RequestBody UpdateDetailOrder detailOrder, @PathVariable long id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        orderService.updateDetailOrder(detailOrder, id, user.getId());
        return ResponseEntity.ok("Cập nhật thành công");
    }

    @PutMapping("/api/admin/orders/update-status/{id}")
    public ResponseEntity<Object> updateStatusOrder(@Valid @RequestBody UpdateStatusOrderRequest updateStatusOrderRequest, @PathVariable long id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        orderService.updateStatusOrder(updateStatusOrderRequest, id, user.getId());
        return ResponseEntity.ok("Cập nhật trạng thái thành công");
    }

    @GetMapping("/tai-khoan/lich-su-giao-dich")
    public String getOrderHistoryPage(Model model){

        //Get list order pending
        User user =((CustomUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        List<OrderInfoDTO> orderList = orderService.getListOrderOfPersonByStatus(ORDER_STATUS,user.getId());
        model.addAttribute("orderList",orderList);

        return "shop/order_history";
    }

    @GetMapping("/api/get-order-list")
    public ResponseEntity<Object> getListOrderByStatus(@RequestParam int status) {
        // Validate status
        if (!LIST_ORDER_STATUS.contains(status)) {
            throw new BadRequestException("Trạng thái đơn hàng không hợp lệ");
        }

        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();
        List<OrderInfoDTO> orders = orderService.getListOrderOfPersonByStatus(status, user.getId());

        return ResponseEntity.ok(orders);
    }

    @GetMapping("/tai-khoan/lich-su-giao-dich/{id}")
    public String getDetailOrderPage(Model model, @PathVariable int id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();

        OrderDetailDTO order = orderService.userGetDetailById(id, user.getId());
        if (order == null) {
            return "error/404";
        }
        model.addAttribute("order", order);

        if (order.getStatus() == ORDER_STATUS) {
            model.addAttribute("canCancel", true);
        } else {
            model.addAttribute("canCancel", false);
        }

        return "shop/order-detail";
    }

    @PostMapping("/api/cancel-order/{id}")
    public ResponseEntity<Object> cancelOrder(@PathVariable long id) {
        User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUser();

        orderService.userCancelOrder(id, user.getId());

        return ResponseEntity.ok("Hủy đơn hàng thành công");
    }

    @GetMapping("/api/orders/export/excel")
    public void exportOrderDataToExcelFile(HttpServletResponse response) {
        List<Order> result = orderService.getAllOrders();
        String fullPath = this.generateProductExcel(result, context, EXPORT_DATA_REPORT_FILE_NAME);
        if (fullPath != null) {
            this.fileDownload(fullPath, response, EXPORT_DATA_REPORT_FILE_NAME, "xlsx");
        }
    }

    private String generateProductExcel(List<Order> orders, ServletContext context, String fileName) {
        String filePath = context.getRealPath(TEMP_EXPORT_DATA_DIRECTORY);
        File file = new File(filePath);
        if (!file.exists()) {
            new File(filePath).mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file + "\\" + fileName + xlsx);
             XSSFWorkbook workbook = new XSSFWorkbook()) {

            XSSFSheet worksheet = workbook.createSheet("Order");
            worksheet.setDefaultColumnWidth(20);

            XSSFRow headerRow = worksheet.createRow(0);

            XSSFCellStyle headerCellStyle = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontName(XSSFFont.DEFAULT_FONT_NAME);
            font.setColor(new XSSFColor(java.awt.Color.WHITE));
            headerCellStyle.setFont(font);
            headerCellStyle.setFillForegroundColor(new XSSFColor(new java.awt.Color(135, 206, 250)));
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            XSSFCell productId = headerRow.createCell(0);
            productId.setCellValue("Mã hoá đơn");
            productId.setCellStyle(headerCellStyle);

            XSSFCell productName = headerRow.createCell(1);
            productName.setCellValue("Ngày mua");
            productName.setCellStyle(headerCellStyle);

            XSSFCell productBrand = headerRow.createCell(2);
            productBrand.setCellValue("Sản phẩm mua");
            productBrand.setCellStyle(headerCellStyle);

            XSSFCell price = headerRow.createCell(3);
            price.setCellValue("Số lượng");
            price.setCellStyle(headerCellStyle);

            XSSFCell priceSell = headerRow.createCell(4);
            priceSell.setCellValue("Tên người nhận");
            priceSell.setCellStyle(headerCellStyle);

            XSSFCell createdAt = headerRow.createCell(5);
            createdAt.setCellValue("Số điện thoại người nhận");
            createdAt.setCellStyle(headerCellStyle);

            XSSFCell modifiedAt = headerRow.createCell(6);
            modifiedAt.setCellValue("Địa chỉ người nhận");
            modifiedAt.setCellStyle(headerCellStyle);

            XSSFCell totalSold = headerRow.createCell(7);
            totalSold.setCellValue("Tổng tiền");
            totalSold.setCellStyle(headerCellStyle);

            XSSFCell status = headerRow.createCell(8);
            status.setCellValue("Trạng thái");
            status.setCellStyle(headerCellStyle);

            XSSFCell note = headerRow.createCell(9);
            note.setCellValue("Lưu ý");
            note.setCellStyle(headerCellStyle);

            if (!orders.isEmpty()) {
                Order order;
                for (int i = 0; i < orders.size(); i++) {
                    order = orders.get(i);
                    XSSFRow bodyRow = worksheet.createRow(i + 1);
                    XSSFCellStyle bodyCellStyle = workbook.createCellStyle();
                    bodyCellStyle.setFillForegroundColor(new XSSFColor(java.awt.Color.WHITE));

                    XSSFCell productIDValue = bodyRow.createCell(0);
                    productIDValue.setCellValue(order.getId());
                    productIDValue.setCellStyle(bodyCellStyle);

                    CreationHelper creationHelper = workbook.getCreationHelper();
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));

                    XSSFCell productNameValue = bodyRow.createCell(1);
                    productNameValue.setCellValue(order.getCreatedAt());
                    productNameValue.setCellStyle(cellStyle);

                    XSSFCell productBrandValue = bodyRow.createCell(2);
                    productBrandValue.setCellValue(order.getProduct().getName());
                    productBrandValue.setCellStyle(bodyCellStyle);

                    XSSFCell priceValue = bodyRow.createCell(3);
                    priceValue.setCellValue(order.getQuantity());
                    priceValue.setCellStyle(bodyCellStyle);

                    XSSFCell priceSellValue = bodyRow.createCell(4);
                    priceSellValue.setCellValue(order.getReceiverName());
                    priceSellValue.setCellStyle(bodyCellStyle);

                    XSSFCell receiverPhone = bodyRow.createCell(5);
                    receiverPhone.setCellValue(order.getReceiverPhone());
                    receiverPhone.setCellStyle(bodyCellStyle);

                    XSSFCell receiverAddress = bodyRow.createCell(6);
                    receiverAddress.setCellValue(order.getReceiverAddress());
                    receiverAddress.setCellStyle(bodyCellStyle);

                    XSSFCell total = bodyRow.createCell(7);
                    total.setCellValue(order.getTotalPrice());
                    total.setCellStyle(bodyCellStyle);

                    XSSFCell statusValue = bodyRow.createCell(8);
                    statusValue.setCellValue(getOrderStatus(order.getStatus()));
                    statusValue.setCellStyle(bodyCellStyle);

                    XSSFCell noteValue = bodyRow.createCell(9);
                    noteValue.setCellValue(order.getNote());
                    noteValue.setCellStyle(bodyCellStyle);
                }
            }
            workbook.write(fos);
            return file + "\\" + fileName + xlsx;
        } catch (Exception e) {
            return null;
        }
    }

    private String getOrderStatus(int status){
        switch (status){
            case 1:
                return "Chờ lấy hàng";
            case 2:
                return "Đang giao hàng";
            case 3:
                return "Đã giao hàng";
            case 4:
                return "Đơn hàng bị trả lại";
            case 5:
                return "Đơn hàng bị hủy";
        }
        return null;
    }
    private void fileDownload(String fullPath, HttpServletResponse response, String fileName, String type) {
        File file = new File(fullPath);
        if (file.exists()) {
            OutputStream os = null;
            try(FileInputStream fis = new FileInputStream(file);) {
                String mimeType = context.getMimeType(fullPath);
                response.setContentType(mimeType);
                response.setHeader("content-disposition", "attachment; filename=" + fileName + "." + type);
                os = response.getOutputStream();
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = -1;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                Files.delete(file.toPath());
            } catch (Exception e) {
                log.error("Can't download file, detail: {}", e.getMessage());
            } finally {
                if(os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

package com.vuhien.application.config;

import com.vuhien.application.entity.User;
import com.vuhien.application.model.dto.PaymentRestDTO;
import com.vuhien.application.model.request.CreateOrderRequest;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Configuration
public class VnpayConfig {
    public static String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    public static String vnp_Returnurl = "http://localhost:80/vnpay/payment-info";
    public static String vnp_TmnCode = "PQPUFQO4";
    public static String vnp_HashSecret = "JOGZYRXSEHZ1A1ZHOHSA9WEGYU1CCWX7";
    public static String vnp_apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    public static String vnp_Version = "2.1.0";
    public static String vnp_Command = "pay";
//    Phương thức tạo mã băm MD5 cho một chuỗi
    public static String md5(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5"); //Tạo một đối tượng
            byte[] hash = md.digest(message.getBytes("UTF-8")); //Chuyển chuỗi message sang dạng byte
            StringBuilder sb = new StringBuilder(2 * hash.length); //Tạo đối tượng
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff)); // Chuyển mỗi byte trong hash sang chuỗi hex, nối vào sb
            }
            digest = sb.toString();
        } catch (UnsupportedEncodingException ex) {
            digest = "";
        } catch (NoSuchAlgorithmException ex) {
            digest = "";
        }
        return digest;
    }
//    Tạo mã băm SHA-256 cho một chuỗi
    public static String Sha256(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(message.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (UnsupportedEncodingException ex) {
            digest = "";
        } catch (NoSuchAlgorithmException ex) {
            digest = "";
        }
        return digest;
    }

    //Util for VNPAY
//    Tạo mã băm cho tất cả các trường trong một bản đồ (map), được sắp xếp theo thứ tự.
    public static String hashAllFields(Map fields) {
        List fieldNames = new ArrayList(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }
        return hmacSHA512(vnp_HashSecret, sb.toString());
    }
//    Tạo mã HMAC SHA-512 sử dụng khóa và dữ liệu đầu vào.
    public static String hmacSHA512(final String key, final String data) {
        try {

            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();

        } catch (Exception ex) {
            return "";
        }
    }
//Lấy địa chỉ IP của người dùng từ yêu cầu HTTP
    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getLocalAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }
//Tạo một chuỗi số ngẫu nhiên với độ dài len
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
//    Phương thức chính để xây dựng yêu cầu thanh toán
    public static PaymentRestDTO getVNPAYResponse(User user, CreateOrderRequest createOrderRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        String vnp_TxnRef = VnpayConfig.getRandomNumber(8);
        String vnp_IpAddr = request.getRemoteAddr();
        String vnp_TmnCode = VnpayConfig.vnp_TmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", VnpayConfig.vnp_Version);
        vnp_Params.put("vnp_Command", VnpayConfig.vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(createOrderRequest.getTotalPrice()*100));
        vnp_Params.put("vnp_CurrCode", "VND");
//        vnp_Params.put("vnp_BankCode", "NCB");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", getOrderInfo(user, createOrderRequest));
        vnp_Params.put("vnp_OrderType", "order");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VnpayConfig.vnp_Returnurl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
//        custom
//        vnp_Params.put("vnp_Email", user.getEmail());
//        vnp_Params.put("vnp_ProductId", createOrderRequest.getProductId());
//        vnp_Params.put("vnp_Size", String.valueOf(createOrderRequest.getSize()));
//        vnp_Params.put("vnp_ReceiverName", createOrderRequest.getReceiverName());
//        vnp_Params.put("vnp_ReceiverPhone", createOrderRequest.getReceiverPhone());
//        vnp_Params.put("vnp_ReceiverAddress", createOrderRequest.getReceiverAddress());
//        vnp_Params.put("vnp_CouponCode", createOrderRequest.getCouponCode());
//        vnp_Params.put("vnp_TotalPrice", String.valueOf(createOrderRequest.getTotalPrice()));
//        vnp_Params.put("vnp_ProductPrice", String.valueOf(createOrderRequest.getProductPrice()));
//        vnp_Params.put("vnp_Note", createOrderRequest.getNote());
//        vnp_Params.put("vnp_Quantity", String.valueOf(createOrderRequest.getQuantity()));
//        vnp_Params.put("vnp_PaymentMethod", createOrderRequest.getPaymentMethod());




        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 30);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                //Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VnpayConfig.hmacSHA512(VnpayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = VnpayConfig.vnp_Url + "?" + queryUrl;


        PaymentRestDTO paymentRestDTO = new PaymentRestDTO();
        paymentRestDTO.setURL(paymentUrl);
        paymentRestDTO.setStatus("OK");
        paymentRestDTO.setMessage("Successfully");

        return paymentRestDTO;
    }
//    Phương thức tạo thông tin đơn hàng từ thông tin người dùng và yêu cầu tạo đơn hàng
    public static String getOrderInfo(User user, CreateOrderRequest createOrderRequest){
        return "email='" + user.getEmail() + '\'' + createOrderRequest.toString();
    }
}

package com.pgeasy.www;

import com.pgeasy.www.module.BasePaymentModule;
import com.pgeasy.www.module.KakaoPayModule;
import com.pgeasy.www.module.TossPaymentsModule;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

sealed interface ApprovePaymentResult {
    default void handle(
            Consumer<SuccessResponse> successHandler,
            Consumer<FailureResponse> failureHandler
    ) {
        if (this instanceof SuccessResponse successResponse) {
            successHandler.accept(successResponse);
        } else if (this instanceof FailureResponse failureResponse) {
            failureHandler.accept(failureResponse);
        }
    }

    record SuccessResponse(String paymentId) implements ApprovePaymentResult {
    }

    record FailureResponse(int errorCode, String errorMessage) implements ApprovePaymentResult {
    }
}

@Slf4j
public class PgPaymentServiceImpl implements PgPaymentService {

    Map<String, BasePaymentModule> map = Map.of(
            PaymentCompany.TOSS.name(), new TossPaymentsModule(),
            PaymentCompany.KAKAO.name(), new KakaoPayModule()
    );


    public PgPaymentService addModule(String key, BasePaymentModule module) {
        map.put(key, module);
        return this;
    }

    // 결제 모듈창
    public JSONObject createModule(JSONObject jsonObject, String secretKey, PaymentCompany paymentCompany) {
        String url = getCreateModule(paymentCompany);
        return sendRequest(jsonObject, secretKey, url, paymentCompany);
    }

    private String getCreateModule(PaymentCompany paymentCompany) {
        switch (paymentCompany) {
            case TOSS:
                return "https://api.tosspayments.com/v1/payments";
            case KAKAO:
                return "https://open-api.kakaopay.com/v1/payment";
            default:
                return "";
        }
    }

    public static void main(String[] args) {
        PgPaymentServiceImpl pgPaymentService = new PgPaymentServiceImpl()
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .addModule("FastCampus", new FastCampusPaymentModule())
                .configure("secretKey", "AAAA")

                ;

        pgPaymentService.approvePayment("", "")
                .handle(
                        successResponse -> {
                            System.out.println("Payment approved: " + successResponse.paymentId());
                        },
                        failureResponse -> {
                            System.out.println("Payment approval failed: " + failureResponse.errorCode() + " " + failureResponse.errorMessage());
                        }
                );
    }

    // 결제 승인
    public ApprovePaymentResult approvePayment(String secretKey, PaymentCompany paymentCompany) {
        String url = getApprovePaymentUrl(paymentCompany);
        try {
            JSONObject jsonObject = sendRequest(new JSONObject(), secretKey, url, paymentCompany);
            jsonObject.get("status");
            return new ApprovePaymentResult.SuccessResponse("paymentId");
        } catch (Exception e) {
            log.error("Error approving payment", e);
            return new ApprovePaymentResult.FailureResponse(500, "Error approving payment");
        }
    }

    private String getApprovePaymentUrl(PaymentCompany paymentCompany) {
        if (PaymentCompany.TOSS == paymentCompany) {
            return "https://api.tosspayments.com/v1/payments/confirm";
        } else if (PaymentCompany.KAKAO == paymentCompany) {
            return "https://open-api.kakaopay.com/online/v1/payment/approve";
        } else {
            return "";
        }
    }

    // ROP: Railway Oriented Programming
    public static void main(String[] args) {
        SuccessResponse jsonObject = approvePayment(..){

        }

        jsonObject.get("status");
        // 성공이면....

        // 실패면.....
    }


    // 예외처리 절대 원칙: 예외는 최대한 일찍 터트리고, 가장 늦게 잡는다.
    private JSONObject sendRequest(JSONObject requestData, String secretKey, String urlString, PaymentCompany paymentCompany) throws IOException, ParseException {
        HttpURLConnection connection = createConnection(secretKey, urlString, paymentCompany);
        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestData.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("Error write response", e);
            throw e;
        }

        try (InputStream responseStream = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
             Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8)) {
            return (JSONObject) new JSONParser().parse(reader);
        } catch (Exception e) {
            log.error("Error reading response", e);
            throw e;
        }
    }

    private HttpURLConnection createConnection(String secretKey, String urlString, PaymentCompany paymentCompany) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Authorization", getAuthorization(secretKey, paymentCompany));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            return connection;
        } catch (IOException e) {
            return null;
        }
    }

    private String getAuthorization(String secretKey, PaymentCompany paymentCompany) {
        if (PaymentCompany.TOSS == paymentCompany) {
            return "Basic " + Base64.getEncoder().encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        } else if (PaymentCompany.KAKAO == paymentCompany) {
            return "SECRET_KEY " + secretKey;
        } else {
            return "";
        }
    }

/*

    // 결제 결과 callback
    public void handlePaymentCallback() {
        // Implementation here
    }

    // 가맹점 승인
    public void approveMerchant() {
        // Implementation here
    }

    // 결제 환불
    public void refundPayment() {
        // Implementation here
    }

    // 결제 상태 확인
    public PaymentStatus checkPaymentStatus() {
        // Implementation here
        return null;
    }
*/
}

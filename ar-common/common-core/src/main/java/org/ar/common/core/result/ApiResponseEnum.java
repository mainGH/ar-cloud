package org.ar.common.core.result;

public enum ApiResponseEnum {
    SUCCESS("SUCCESS", "", "Operation successful"),
    PARAM_VALID_FAIL("FAIL", "1001", "Parameter validation failed"),
    SIGNATURE_ERROR("FAIL", "1002", "Signature error"),
    INVALID_REQUEST("FAIL", "1003", "Merchant does not exist"),
    INVALID_IP("FAIL", "1004", "Illegal IP"),
    DECRYPTION_ERROR("FAIL", "1005", "Invalid ciphertext"),
    INVALID_MERCHANT_PUBLIC_KEY("FAIL", "1006", "Invalid merchant public key"),
    MEMBER_ALREADY_REGISTERED("FAIL", "1007", "Activation failed, the member has already been registered"),
    MEMBER_NOT_FOUND("FAIL", "1008", "Member not found"),
    INSUFFICIENT_BALANCE("FAIL", "1009", "Insufficient balance"),
    DATA_DUPLICATE_SUBMISSION("FAIL", "1010", "Data duplicate submission"),
    ORDER_NOT_FOUND("FAIL", "1011", "Order not found"),
    INSUFFICIENT_MERCHANT_BALANCE("FAIL", "1012", "Insufficient merchant balance"),
    MERCHANT_COLLECTION_STATUS_DISABLED("FAIL", "1013", "Merchant collection status not enabled"),
    MERCHANT_PAYMENT_STATUS_DISABLED("FAIL", "1014", "Merchant payment status not enabled"),
    AMOUNT_EXCEEDS_LIMIT("FAIL", "1015", "Amount exceeds limit"),
    NO_PERMISSION("FAIL", "1016", "Your AR wallet has been disabled, please contact customer service"),
    ALLOWED("FAIL", "1017", "Unable to access AR wallet"),
    MERCHANT_STATUS_DISABLED("FAIL", "1018", "Merchant status not enabled"),

    SYSTEM_EXECUTION_ERROR("FAIL", "9999", "System error");

    private final String status;
    private final String errorCode;
    private final String message;

    ApiResponseEnum(String status, String errorCode, String message) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}

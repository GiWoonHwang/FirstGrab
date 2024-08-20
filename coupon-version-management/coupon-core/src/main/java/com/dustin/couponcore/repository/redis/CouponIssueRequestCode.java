package com.dustin.couponcore.repository.redis;

import com.dustin.couponcore.exception.CouponIssueException;
import com.dustin.couponcore.exception.ErrorCode;

// 쿠폰 발급 요청에 대한 상태 코드를 정의하는 열거형(enum)입니다.
public enum CouponIssueRequestCode {
    // 쿠폰 발급이 성공적으로 처리된 경우의 코드입니다.
    SUCCESS(1),
    // 중복된 쿠폰 발급 요청이 있을 때의 코드입니다.
    DUPLICATED_COUPON_ISSUE(2),
    // 유효하지 않은 쿠폰 발급 수량 요청이 있을 때의 코드입니다.
    INVALID_COUPON_ISSUE_QUANTITY(3);

    // 상태 코드를 나타내는 정수형 변수입니다.
    private final int code;

    // 열거형의 생성자로, 각 상수에 대한 코드 값을 초기화합니다.
    CouponIssueRequestCode(int code) {
        this.code = code;
    }

    // 주어진 문자열 형태의 코드에 해당하는 CouponIssueRequestCode를 반환하는 정적 메서드입니다.
    public static CouponIssueRequestCode find(String code) {
        // 문자열 코드를 정수로 변환합니다.
        int codeValue = Integer.parseInt(code);
        // 코드 값에 따라 적절한 열거형 상수를 반환합니다.
        if (codeValue == 1) return SUCCESS;
        if (codeValue == 2) return DUPLICATED_COUPON_ISSUE;
        if (codeValue == 3) return INVALID_COUPON_ISSUE_QUANTITY;
        // 코드 값이 정의된 상수 중 하나에 해당하지 않으면 예외를 발생시킵니다.
        throw new IllegalArgumentException("존재하지 않는 코드입니다. %s".formatted(code));
    }

    // 주어진 CouponIssueRequestCode에 따라 요청 결과를 검사하고, 예외를 발생시키는 정적 메서드입니다.
    public static void checkRequestResult(CouponIssueRequestCode code) {
        // 요청 코드가 유효하지 않은 쿠폰 발급 수량을 나타내면 예외를 발생시킵니다.
        if (code == INVALID_COUPON_ISSUE_QUANTITY) {
            throw new CouponIssueException(ErrorCode.INVALID_COUPON_ISSUE_QUANTITY, "발급 가능한 수량을 초과합니다");
        }
        // 요청 코드가 중복된 쿠폰 발급을 나타내면 예외를 발생시킵니다.
        if (code == DUPLICATED_COUPON_ISSUE) {
            throw new CouponIssueException(ErrorCode.DUPLICATED_COUPON_ISSUE, "이미 발급된 쿠폰입니다.");
        }
    }
}

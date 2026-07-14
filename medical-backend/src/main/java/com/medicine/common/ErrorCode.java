/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026-2026. All rights reserved.
 */

package com.medicine.common;

public final class ErrorCode {

    public static final int SUCCESS = 20000;
    public static final int INVALID_ARGUMENT = 10000;
    public static final int DUPLICATE_DATA = 10001;
    public static final int LOGIN_FAILED = 10002;
    public static final int FORBIDDEN = 10003;
    public static final int NOT_FOUND = 10004;
    public static final int TOKEN_EXPIRED = 10006;
    public static final int INTERNAL_ERROR = 50000;

    private ErrorCode() {
    }
}

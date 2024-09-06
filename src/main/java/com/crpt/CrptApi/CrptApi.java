package com.crpt.CrptApi;

import java.util.concurrent.TimeUnit;

public class CrptApi {
    private final TimeUnit timeUnit;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
    }

}

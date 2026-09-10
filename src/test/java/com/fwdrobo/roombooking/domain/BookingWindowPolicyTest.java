package com.fwdrobo.roombooking.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingWindowPolicyTest {

    private final BookingWindowPolicy policy = new BookingWindowPolicy();

    //测试上下边界
    @Test
    void shouldBeValidWhenDurationIs30Minutes() {//测试下边界
        LocalDateTime start =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 10, 15, 00);

        assertEquals(
                BookingWindowResult.VALID,
                policy.evaluate(start, end)
        );
    }
    @Test
    void shouldBeValidWhenDurationIs120Minutes() {//测试上边界

        LocalDateTime start =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 10, 16, 30);

        assertEquals(
                BookingWindowResult.VALID,
                policy.evaluate(start, end)
        );
    }


    //测试边界相邻值
    @Test
    void shouldRejectWhenDurationIs29Minutes() {

        LocalDateTime start =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 10, 14, 59);

        assertEquals(
                BookingWindowResult.DURATION_OUT_OF_RANGE,
                policy.evaluate(start, end)
        );
    }
    @Test
    void shouldRejectWhenDurationIs121Minutes() {

        LocalDateTime start =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 10, 16, 31);

        assertEquals(
                BookingWindowResult.DURATION_OUT_OF_RANGE,
                policy.evaluate(start, end)
        );
    }

    //测试同时违反多项规则
    @Test
    void shouldReturnEndNotAfterStartBeforeCheckingDuration() {

        LocalDateTime start =
                LocalDateTime.of(2026, 9, 10, 14, 30);

        LocalDateTime end =
                LocalDateTime.of(2026, 9, 10, 14, 00);

        assertEquals(
                BookingWindowResult.END_NOT_AFTER_START,
                policy.evaluate(start, end)
        );
    }

}
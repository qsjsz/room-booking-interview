package com.fwdrobo.roombooking.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class BookingWindowPolicy {

    public BookingWindowResult evaluate(LocalDateTime start, LocalDateTime end) {
        if(start==null || end==null){
            return BookingWindowResult.MISSING_BOUNDARY;
        }
        if(!end.isAfter(start)){
            return BookingWindowResult.END_NOT_AFTER_START;
        }
        long minutes= Duration.between(start, end).toMinutes();
        if(minutes<30||minutes>120){
            return BookingWindowResult.DURATION_OUT_OF_RANGE;
        }
        return BookingWindowResult.VALID;
        //throw new UnsupportedOperationException("Booking window policy is not implemented");
    }
}

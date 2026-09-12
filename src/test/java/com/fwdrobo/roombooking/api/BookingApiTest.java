package com.fwdrobo.roombooking.api;

import com.fwdrobo.roombooking.repository.InMemoryBookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryBookingRepository bookingRepository;

    @Test
    void returnsExistingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-1011"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("booking-1011"))
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start").value("2030-01-15T09:00:00"))
                .andExpect(jsonPath("$.end").value("2030-01-15T09:30:00"));
    }

    @Test
    void returnsNotFoundForMissingRoom() throws Exception {
        mockMvc.perform(get("/rooms/room-missing/bookings/booking-1011"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/rooms/room-missing/bookings/booking-1011"));
    }

    @Test
    void returnsNotFoundForMissingBooking() throws Exception {
        mockMvc.perform(get("/rooms/room-101/bookings/booking-missing"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"))
                .andExpect(jsonPath("$.path")
                        .value("/rooms/room-101/bookings/booking-missing"));
    }

    // ======================
    // Task 4
    // ======================

    @Test
    void createsBookingSuccessfully() throws Exception {

        mockMvc.perform(post("/rooms/room-101/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                         {
                           "start":"2030-01-15T10:00:00",
                           "end":"2030-01-15T10:30:00"
                         }
                        """))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.roomId").value("room-101"))
                .andExpect(jsonPath("$.start")
                        .value("2030-01-15T10:00:00"))
                .andExpect(jsonPath("$.end")
                        .value("2030-01-15T10:30:00"));
    }

    @Test
    void returns404WhenRoomMissing() throws Exception {

        mockMvc.perform(post("/rooms/room-missing/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:00:00",
                          "end":"2030-01-15T10:30:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ROOM_NOT_FOUND"));
    }

    @Test
    void returns400WhenWindowInvalid() throws Exception {

        mockMvc.perform(post("/rooms/room-101/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:00:00",
                          "end":"2030-01-15T10:10:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_BOOKING_WINDOW"));
    }

    @Test
    void returns409WhenBookingConflicts() throws Exception {

        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:15:00",
                          "end":"2030-01-15T10:45:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("BOOKING_CONFLICT"));
    }

    @Test
    void createsBookingWhenTouchingBoundary() throws Exception {

        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:30:00",
                          "end":"2030-01-15T11:00:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void invalidWindowShouldNotCreateBooking() throws Exception {

        long before = bookingRepository.countByRoomId("room-101");

        mockMvc.perform(post("/rooms/room-101/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:00:00",
                          "end":"2030-01-15T10:10:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isBadRequest());

        long after = bookingRepository.countByRoomId("room-101");

        assertEquals(before, after);
    }

    @Test
    void conflictShouldNotCreateBooking() throws Exception {

        long before = bookingRepository.countByRoomId("room-202");

        mockMvc.perform(post("/rooms/room-202/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "start":"2030-01-15T10:15:00",
                          "end":"2030-01-15T10:45:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isConflict());

        long after = bookingRepository.countByRoomId("room-202");

        assertEquals(before, after);
    }
    @Test
    void canGetCreatedBookingFromLocation() throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/rooms/room-101/bookings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                          "start":"2030-01-15T15:00:00",
                          "end":"2030-01-15T15:30:00"
                        }
                        """))
                .andDo(print())
                .andExpect(status().isCreated())
                .andReturn();

        String location =
                result.getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId")
                        .value("room-101"))
                .andExpect(jsonPath("$.start")
                        .value("2030-01-15T15:00:00"))
                .andExpect(jsonPath("$.end")
                        .value("2030-01-15T15:30:00"));
    }
}
package com.rocketseat.planner.trip;

import com.rocketseat.planner.participant.ParticipantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    private ParticipantService participantService;


    private TripRepository tripRepository;

    public TripController(ParticipantService participantService, TripRepository tripRepository) {
        this.participantService = participantService;
        this.tripRepository = tripRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getOne(@PathVariable UUID id) {
        Optional<Trip> trip = tripRepository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/{id}/confirm")//atributo is_confirmed vai de false p true
    public ResponseEntity<Trip> confirmTrip(@PathVariable UUID id) {
        Optional<Trip> trip = tripRepository.findById(id);

        if (trip.isPresent()) {

            Trip rawTrip = trip.get();
            rawTrip.setIsConfirmed(true);

            tripRepository.save(rawTrip);
            participantService.triggerConfirmationToParticipants(id);//dispara os email de confirmação

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }


    @PutMapping("/{id}")
    public ResponseEntity<Trip> update(@PathVariable UUID id, @RequestBody TripRequestPayload payload) {
        Optional<Trip> trip = tripRepository.findById(id);

        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            rawTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rawTrip.setDestination(payload.destination());
            tripRepository.save(rawTrip);

            return ResponseEntity.ok(rawTrip);
        }

        return ResponseEntity.notFound().build();
    }
    //delete

    @PostMapping
    public ResponseEntity<TripCreateResponse> create(@RequestBody TripRequestPayload payload) {
        Trip newTrip = new Trip(payload);

        this.tripRepository.save(newTrip);
        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip.getId());

        return ResponseEntity.ok().body(new TripCreateResponse(newTrip.getId()));
    }
}

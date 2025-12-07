package tn.fst.eventsproject.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;
import tn.fst.eventsproject.entities.Tache;
import tn.fst.eventsproject.repositories.EventRepository;
import tn.fst.eventsproject.repositories.LogisticsRepository;
import tn.fst.eventsproject.repositories.ParticipantRepository;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServicesImplTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    ParticipantRepository participantRepository;

    @Mock
    LogisticsRepository logisticsRepository;

    @InjectMocks
    EventServicesImpl eventServices;

    @Captor
    ArgumentCaptor<Event> eventCaptor;

    Participant participant;
    Event event;

    @BeforeEach
    void setUp() {
        participant = new Participant();
        participant.setIdPart(1);
        participant.setNom("Tounsi");
        participant.setPrenom("Ahmed");
        participant.setTache(Tache.ORGANISATEUR);

        event = new Event();
        event.setIdEvent(10);
        event.setDescription("My Event");
        event.setDateDebut(LocalDate.now());
        event.setDateFin(LocalDate.now().plusDays(1));
    }

    @Test
    void addParticipant_savesAndReturnsParticipant() {
        when(participantRepository.save(any(Participant.class))).thenAnswer(inv -> inv.getArgument(0));

        Participant saved = eventServices.addParticipant(participant);

        assertNotNull(saved);
        verify(participantRepository, times(1)).save(participant);
    }

    @Test
    void addAffectEvenParticipant_byId_createsEventsSetWhenNullAndSavesEvent() {
        when(participantRepository.findById(1)).thenReturn(Optional.of(participant));
        when(eventRepository.save(event)).thenReturn(event);

        Event res = eventServices.addAffectEvenParticipant(event, 1);

        assertEquals(event, res);
        // participant should have events set now
        assertNotNull(participant.getEvents());
        assertTrue(participant.getEvents().contains(event));
        verify(participantRepository, times(1)).findById(1);
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void addAffectEvenParticipant_withParticipants_callsFindByIdForEachAndSaves() {
        Participant p2 = new Participant();
        p2.setIdPart(2);
        p2.setNom("A");
        Set<Participant> participants = new HashSet<>();
        Participant ref = new Participant();
        ref.setIdPart(1);
        participants.add(ref);
        event.setParticipants(participants);

        when(participantRepository.findById(1)).thenReturn(Optional.of(participant));
        when(eventRepository.save(event)).thenReturn(event);

        Event res = eventServices.addAffectEvenParticipant(event);

        assertEquals(event, res);
        verify(participantRepository, times(1)).findById(1);
        verify(eventRepository, times(1)).save(event);
    }

    @Test
    void addAffectLog_whenEventLogisticsNull_initializesSetAndSaves() {
        Logistics log = new Logistics();
        log.setIdLog(5);
        log.setDescription("L1");
        log.setReserve(true);
        log.setPrixUnit(10f);
        log.setQuantite(2);

        // event with null logistics
        Event e = new Event();
        e.setDescription("Desc1");
        e.setLogistics(null);

        when(eventRepository.findByDescription("Desc1")).thenReturn(e);
        when(logisticsRepository.save(log)).thenReturn(log);
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Logistics res = eventServices.addAffectLog(log, "Desc1");

        assertEquals(log, res);
        assertNotNull(e.getLogistics());
        assertTrue(e.getLogistics().contains(log));
        verify(eventRepository, times(1)).findByDescription("Desc1");
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(logisticsRepository, times(1)).save(log);
    }

    @Test
    void getLogisticsDates_whenEventHasEmptyLogistics_returnsEmptyList() {
        Event e = new Event();
        e.setDescription("Eempty");
        e.setDateDebut(LocalDate.now());
        e.setDateFin(LocalDate.now().plusDays(1));
        e.setLogistics(new HashSet<>());

        when(eventRepository.findByDateDebutBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(e));

        List<Logistics> res = eventServices.getLogisticsDates(LocalDate.now(), LocalDate.now().plusDays(2));

        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    void getLogisticsDates_returnsOnlyReservedLogistics() {
        Logistics l1 = new Logistics();
        l1.setIdLog(1);
        l1.setReserve(true);
        l1.setPrixUnit(5f);
        l1.setQuantite(2);

        Logistics l2 = new Logistics();
        l2.setIdLog(2);
        l2.setReserve(false);
        l2.setPrixUnit(3f);
        l2.setQuantite(1);

        Event e = new Event();
        e.setDescription("E");
        Set<Logistics> logs = new HashSet<>();
        logs.add(l1);
        logs.add(l2);
        e.setLogistics(logs);

        when(eventRepository.findByDateDebutBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(e));

        List<Logistics> res = eventServices.getLogisticsDates(LocalDate.now(), LocalDate.now().plusDays(2));

        assertNotNull(res);
        assertEquals(1, res.size());
        assertTrue(res.contains(l1));
        assertFalse(res.contains(l2));
    }

    @Test
    void calculCout_accumulatesReservedLogisticsAndSavesEvent() {
        Logistics l1 = new Logistics();
        l1.setReserve(true);
        l1.setPrixUnit(10f);
        l1.setQuantite(3);

        Set<Logistics> logs = new HashSet<>();
        logs.add(l1);

        Event e = new Event();
        e.setDescription("Evt");
        e.setLogistics(logs);

        when(eventRepository.findByParticipants_NomAndParticipants_PrenomAndParticipants_Tache(anyString(), anyString(), any(Tache.class)))
                .thenReturn(Collections.singletonList(e));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventServices.calculCout();

        // price = 10 * 3 = 30
        assertEquals(30f, e.getCout());
        verify(eventRepository, atLeastOnce()).save(e);
    }

}

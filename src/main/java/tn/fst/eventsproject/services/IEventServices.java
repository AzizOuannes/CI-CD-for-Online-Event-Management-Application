package tn.fst.eventsproject.services;

import tn.fst.eventsproject.entities.Event;
import tn.fst.eventsproject.entities.Logistics;
import tn.fst.eventsproject.entities.Participant;

import java.time.LocalDate;
import java.util.List;

public interface IEventServices {
    public Participant addParticipant(Participant participant);
    public Event addAffectEvenParticipant(Event event, int idParticipant);
    public Event addAffectEvenParticipant(Event event);
    public Logistics addAffectLog(Logistics logistics, String descriptionEvent);
    public List<Logistics> getLogisticsDates(LocalDate dateDebut, LocalDate dateFin);
    public void calculCout();
}

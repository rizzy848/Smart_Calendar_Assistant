package Framework;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import usecase.CalendarException;
import usecase.CalendarGateway;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Calendar API implementation of CalendarGateway.
 */
public class GoogleCalendarGateway implements CalendarGateway {

    private static final String APPLICATION_NAME = "Smart Calendar Assistant";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    private Calendar service;
    private boolean available = false;

    public GoogleCalendarGateway() {
        try {
            initialize();
            available = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize Google Calendar: " + e.getMessage());
            available = false;
        }
    }

    private void initialize() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = GoogleCalendarGateway.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    @Override
    public entity.Event createEvent(entity.Event event) throws CalendarException {
        if (!available) {
            throw new CalendarException("Calendar service not available", "SERVICE_UNAVAILABLE");
        }

        try {
            Event googleEvent = new Event()
                    .setSummary(event.getTitle())
                    .setDescription(event.getDescription())
                    .setLocation(event.getLocation());

            // Convert our Event times to Google Calendar format
            ZonedDateTime startDateTime = ZonedDateTime.of(event.getDate(), event.getStartTime(), ZoneId.systemDefault());
            ZonedDateTime endDateTime = ZonedDateTime.of(event.getDate(), event.getEndTime(), ZoneId.systemDefault());

            EventDateTime start = new EventDateTime()
                    .setDateTime(new DateTime(startDateTime.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());
            googleEvent.setStart(start);

            EventDateTime end = new EventDateTime()
                    .setDateTime(new DateTime(endDateTime.toInstant().toEpochMilli()))
                    .setTimeZone(ZoneId.systemDefault().getId());
            googleEvent.setEnd(end);

            Event createdEvent = service.events().insert("primary", googleEvent).execute();

            // Convert back to our Event entity
            return convertToEvent(createdEvent);

        } catch (IOException e) {
            throw new CalendarException("Failed to create event: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public List<entity.Event> getEventsForDate(LocalDate date) throws CalendarException {
        ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault());
        return getEventsInRange(date, date);
    }

    @Override
    public List<entity.Event> getEventsInRange(LocalDate startDate, LocalDate endDate)
            throws CalendarException {
        if (!available) {
            throw new CalendarException("Calendar service not available", "SERVICE_UNAVAILABLE");
        }

        try {
            ZonedDateTime start = startDate.atStartOfDay(ZoneId.systemDefault());
            ZonedDateTime end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault());

            Events events = service.events().list("primary")
                    .setTimeMin(new DateTime(start.toInstant().toEpochMilli()))
                    .setTimeMax(new DateTime(end.toInstant().toEpochMilli()))
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            return events.getItems().stream()
                    .map(this::convertToEvent)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            throw new CalendarException("Failed to retrieve events: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public void deleteEvent(String eventId) throws CalendarException {
        if (!available) {
            throw new CalendarException("Calendar service not available", "SERVICE_UNAVAILABLE");
        }

        try {
            service.events().delete("primary", eventId).execute();
        } catch (IOException e) {
            throw new CalendarException("Failed to delete event: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public entity.Event updateEvent(entity.Event event) throws CalendarException {
        if (!available) {
            throw new CalendarException("Calendar service not available", "SERVICE_UNAVAILABLE");
        }

        try {
            Event googleEvent = service.events().get("primary", event.getEventId()).execute();

            googleEvent.setSummary(event.getTitle());
            googleEvent.setDescription(event.getDescription());
            googleEvent.setLocation(event.getLocation());

            Event updatedEvent = service.events().update("primary", event.getEventId(), googleEvent).execute();
            return convertToEvent(updatedEvent);

        } catch (IOException e) {
            throw new CalendarException("Failed to update event: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private entity.Event convertToEvent(Event googleEvent) {
        LocalDate date = LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
                ZoneId.systemDefault()
        );

        LocalTime startTime = LocalTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleEvent.getStart().getDateTime().getValue()),
                ZoneId.systemDefault()
        );

        LocalTime endTime = LocalTime.ofInstant(
                java.time.Instant.ofEpochMilli(googleEvent.getEnd().getDateTime().getValue()),
                ZoneId.systemDefault()
        );

        return new entity.Event(
                googleEvent.getId(),
                googleEvent.getSummary(),
                googleEvent.getDescription(),
                date,
                startTime,
                endTime,
                googleEvent.getLocation()
        );
    }
}

package Framework;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
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

import entity.User;
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
 * Multi-user Google Calendar API implementation with OAuth URL generation support.
 */
public class MultiUserGoogleCalendarGateway implements CalendarGateway {

    private static final String APPLICATION_NAME = "Smart Calendar Assistant";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String REDIRECT_URI = "http://localhost:8888/Callback";

    private Calendar service;
    private User currentUser;
    private boolean available = false;
    private GoogleAuthorizationCodeFlow flow;
    private NetHttpTransport httpTransport;

    public MultiUserGoogleCalendarGateway(User user) {
        this.currentUser = user;
        try {
            initializeFlow();
            Credential credential = loadExistingCredential();

            if (credential != null) {
                // User already authenticated
                initializeService(credential);
                available = true;
                user.setAuthenticated(true);
                System.out.println("✅ Using existing credentials for " + user.getEmail());
            } else {
                // User needs to authenticate
                System.out.println("⚠️  No credentials found for " + user.getEmail());
                System.out.println("💡 OAuth URL needs to be generated");
                available = false;
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize: " + e.getMessage());
            e.printStackTrace();
            available = false;
        }
    }

    private void initializeFlow() throws IOException, GeneralSecurityException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        InputStream in = MultiUserGoogleCalendarGateway.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        String tokensDir = currentUser.getTokensDirectory();

        this.flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDir)))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
    }

    private Credential loadExistingCredential() throws IOException {
        Credential credential = flow.loadCredential(currentUser.getUserId());

        if (credential != null && credential.getRefreshToken() != null) {
            // Refresh if needed
            if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
                System.out.println("🔄 Refreshing access token...");
                credential.refreshToken();
            }
            return credential;
        }

        return null;
    }

    private void initializeService(Credential credential) throws IOException, GeneralSecurityException {
        service = new Calendar.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Generate OAuth authorization URL for frontend to open
     */
    public String getAuthorizationUrl() {
        try {
            if (flow == null) {
                initializeFlow();
            }

            String authUrl = flow.newAuthorizationUrl()
                    .setRedirectUri(REDIRECT_URI)
                    .build();

            System.out.println("🔗 Generated OAuth URL: " + authUrl.substring(0, Math.min(100, authUrl.length())) + "...");
            return authUrl;

        } catch (Exception e) {
            System.err.println("❌ Failed to generate OAuth URL: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Complete OAuth flow with authorization code from frontend
     */
    public boolean completeAuthorization(String authorizationCode) {
        try {
            System.out.println("🔐 Completing OAuth with code: " + authorizationCode.substring(0, Math.min(20, authorizationCode.length())) + "...");

            GoogleTokenResponse response = flow.newTokenRequest(authorizationCode)
                    .setRedirectUri(REDIRECT_URI)
                    .execute();

            Credential credential = flow.createAndStoreCredential(response, currentUser.getUserId());

            initializeService(credential);
            available = true;
            currentUser.setAuthenticated(true);

            System.out.println("✅ OAuth completed successfully for " + currentUser.getEmail());
            return true;

        } catch (Exception e) {
            System.err.println("❌ Failed to complete OAuth: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public entity.Event createEvent(entity.Event event) throws CalendarException {
        if (!available) {
            throw new CalendarException("Calendar service not available - authentication required", "AUTH_REQUIRED");
        }

        try {
            Event googleEvent = new Event()
                    .setSummary(event.getTitle())
                    .setDescription(event.getDescription())
                    .setLocation(event.getLocation());

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

            Event createdEvent = service.events().insert(currentUser.getCalendarId(), googleEvent).execute();

            System.out.println("✅ Event created: " + createdEvent.getHtmlLink());
            return convertToEvent(createdEvent);

        } catch (IOException e) {
            System.err.println("❌ Failed to create event: " + e.getMessage());
            throw new CalendarException("Failed to create event: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public List<entity.Event> getEventsForDate(LocalDate date) throws CalendarException {
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

            Events events = service.events().list(currentUser.getCalendarId())
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
            service.events().delete(currentUser.getCalendarId(), eventId).execute();
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
            Event googleEvent = service.events().get(currentUser.getCalendarId(), event.getEventId()).execute();

            googleEvent.setSummary(event.getTitle());
            googleEvent.setDescription(event.getDescription());
            googleEvent.setLocation(event.getLocation());

            Event updatedEvent = service.events().update(currentUser.getCalendarId(), event.getEventId(), googleEvent).execute();
            return convertToEvent(updatedEvent);

        } catch (IOException e) {
            throw new CalendarException("Failed to update event: " + e.getMessage(), "API_ERROR", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    public User getCurrentUser() {
        return currentUser;
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
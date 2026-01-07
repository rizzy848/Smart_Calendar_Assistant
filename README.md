# Smart Calendar Assistant - AI-Powered Natural Language Scheduling

An intelligent calendar management system that converts natural language commands into structured Google Calendar events using **Google Gemini AI**. Built with **Clean Architecture principles**, this full-stack application features a **Spring Boot backend** and **React frontend** for seamless, conversational event creation.

[![Java](https://img.shields.io/badge/Java-24-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## 🎯 Project Overview

**"Schedule a team meeting tomorrow at 2 PM"** → Instantly creates a Google Calendar event.

This project demonstrates professional full-stack development with AI integration, implementing a production-ready calendar assistant that understands natural language. Users can create events by simply typing conversational commands instead of filling out forms.

**Key Features:**
- 🤖 **AI Natural Language Processing** - Powered by Google Gemini 2.5 Flash for intelligent text parsing
- 📅 **Google Calendar Integration** - Seamless OAuth 2.0 authentication and real-time event creation
- 👥 **Multi-User Support** - Separate calendar access for multiple users with persistent authentication
- 🏗️ **Clean Architecture** - Separation of concerns with clear boundaries between layers
- 🎨 **Modern React UI** - Responsive, professional frontend with real-time feedback
- 🔐 **Secure Authentication** - OAuth 2.0 implementation with secure token management
- ⚡ **RESTful API** - Well-structured backend with proper CORS and error handling

## 🏗️ Architecture & Design

This project follows **Clean Architecture** principles (Uncle Bob Martin) with **SOLID design patterns** for maintainability and testability.

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│              Presentation Layer (React)                 │
│            UI Components, API Calls, State              │
├─────────────────────────────────────────────────────────┤
│         Interface Adapter Layer (Spring Boot)           │
│      Controllers, DTOs, Presenters, API Endpoints       │
├─────────────────────────────────────────────────────────┤
│            Use Case Layer (Business Logic)              │
│         Interactors, Input/Output Boundaries            │
├─────────────────────────────────────────────────────────┤
│         Entity Layer (Core Business Objects)            │
│        Event, User, EventRequest, ActionType            │
├─────────────────────────────────────────────────────────┤
│         Framework Layer (External Services)             │
│   Google Calendar Gateway, Gemini AI Parser, Storage    │
└─────────────────────────────────────────────────────────┘
```

### Design Patterns Implemented

- **Dependency Inversion** - Use cases depend on abstractions (interfaces), not concrete implementations
- **Dependency Injection** - Constructor-based injection throughout all layers
- **Gateway Pattern** - `CalendarGateway` abstracts external Google Calendar API
- **Presenter Pattern** - Separate presenters for console and API responses
- **Factory Pattern** - User management with isolated token directories
- **Repository Pattern** - UserManager handles persistence and data access
- **DTO Pattern** - Clean data transfer between layers with dedicated DTOs

### Component Flow

```
User Input → React Frontend → REST API (Spring Boot)
                                    ↓
                          EventController (Adapter)
                                    ↓
                     CreateEventInteractor (Use Case)
                          ↓                    ↓
            AIEventParser (Gemini)    CalendarGateway (Google)
                          ↓                    ↓
                    EventRequest  →  Google Calendar Event
                                    ↓
                          ApiPresenter (Response)
                                    ↓
                          React Frontend (Display)
```

## 🚀 Getting Started

### Prerequisites

**Backend Requirements:**
- **Java 24** (or Java 17+)
- **Maven 3.8+**
- **Google Cloud Project** with Calendar API enabled
- **Gemini API Key** from Google AI Studio

**Frontend Requirements:**
- **Node.js 16+** and npm
- Modern web browser

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/smart-calendar-assistant.git
cd smart-calendar-assistant
```

#### 2. Set Up Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project
3. Enable **Google Calendar API**
4. Create **OAuth 2.0 credentials** (Desktop app)
5. Download `credentials.json` and place in `src/main/resources/`

#### 3. Set Up Gemini AI

1. Get API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Create `src/main/resources/config/Gemini.properties`:
```properties
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
gemini.model=gemini-2.5-flash
```

#### 4. Build and Run Backend

```bash
# Navigate to project root
mvn clean install

# Run Spring Boot application
mvn spring-boot:run
```

Backend will start on `http://localhost:8080`

#### 5. Set Up and Run Frontend

```bash
# Navigate to frontend directory
cd calendar-frontend

# Install dependencies
npm install

# Start React development server
npm start
```

Frontend will open at `http://localhost:3000`

## 💡 How to Use

### Web Interface (Recommended)

1. **Open** `http://localhost:3000` in your browser
2. **Register** with your name and Google email
3. **Authenticate** with Google Calendar (first-time OAuth flow)
4. **Type natural commands** like:
    - "Schedule team meeting tomorrow at 2 PM"
    - "Add dentist appointment next Monday at 10:30 AM"
    - "Create workout session today at 6 PM for 1 hour"
5. **Confirm** parsed event details
6. **Create** → Event appears in your Google Calendar!

### Command Line Interface (Advanced)

```bash
# Run AI-powered CLI (single user)
java -cp target/classes app.AICalendarMain

# Run multi-user CLI
java -cp target/classes app.MultiUserAICalendarMain
```

## 🎨 User Interface

### Login Screen
- Clean, modern design with gradient background
- User registration and authentication
- Real-time backend connection status

### Main Calendar Interface
- Natural language input with autocomplete
- Live parsing feedback with confirmation step
- Visual event details display
- Success/error messages with icons
- Coming soon features (View, Delete, Update)

### Example Interactions

```
Input:  "Meeting with John tomorrow at 2 PM"
Output: ✓ Parsed Event Details
        Action: CREATE
        Title: Meeting with John
        Date: 2025-01-07
        Start: 14:00
        End: 15:00 (default 1 hour)

Input:  "Team standup next Monday 9:30 AM at Office"
Output: ✓ Parsed Event Details
        Action: CREATE
        Title: Team standup
        Date: 2025-01-13
        Start: 09:30
        Location: Office
```

## 📂 Project Structure

```
smart-calendar-assistant/
├── src/main/java/
│   ├── api/                          # Spring Boot REST API Layer
│   │   ├── controller/               # REST Controllers
│   │   │   ├── EventController.java  # Event management endpoints
│   │   │   └── UserController.java   # User management endpoints
│   │   └── dto/                      # Data Transfer Objects
│   │       ├── CreateEventDTO.java
│   │       ├── ParsedEventDTO.java
│   │       └── UserDTO.java
│   ├── app/                          # Application Entry Points
│   │   ├── AICalendarMain.java      # CLI with AI parsing
│   │   └── MultiUserAICalendarMain.java
│   ├── entity/                       # Core Domain Entities
│   │   ├── Event.java               # Calendar event model
│   │   ├── User.java                # User account model
│   │   ├── EventRequest.java        # Parsed AI request
│   │   ├── EventResponse.java       # Use case response
│   │   └── ActionType.java          # Action enumeration
│   ├── usecase/                      # Business Logic Layer
│   │   ├── create/                   # Create event use case
│   │   │   ├── CreateEventInteractor.java
│   │   │   ├── CreateEventInputBoundary.java
│   │   │   └── CreateEventOutputBoundary.java
│   │   ├── CalendarGateway.java     # Calendar abstraction
│   │   └── CalendarException.java   # Domain exceptions
│   ├── Framework/                    # External Services Layer
│   │   ├── GoogleCalendarGateway.java
│   │   ├── MultiUserGoogleCalendarGateway.java
│   │   ├── AIEventParser.java       # Gemini AI integration
│   │   └── UserManager.java         # User persistence
│   └── interface_adapter/            # Adapter Layer
│       └── presenter/
│           ├── ApiPresenter.java    # REST response presenter
│           └── ConsolePresenter.java
├── calendar-frontend/                # React Frontend
│   ├── src/
│   │   ├── App.js                   # Main application component
│   │   ├── App.css                  # Styling and animations
│   │   └── services/
│   │       └── api.js               # Backend API integration
│   └── public/
└── pom.xml                          # Maven configuration
```

## 🔑 Key Technical Highlights

### AI & Machine Learning
- ✅ **Google Gemini 2.5 Flash** - State-of-the-art NLP for text understanding
- ✅ **Context-Aware Parsing** - Handles relative dates ("tomorrow", "next Monday")
- ✅ **Time Format Conversion** - Automatic 12h ↔ 24h conversion
- ✅ **Default Value Inference** - Smart defaults for missing information

### Backend Architecture
- ✅ **Clean Architecture** - Clear separation of concerns with dependency inversion
- ✅ **SOLID Principles** - Single responsibility, interface segregation, DI
- ✅ **RESTful API Design** - Proper HTTP methods, status codes, CORS
- ✅ **Multi-User Authentication** - Isolated OAuth tokens per user
- ✅ **Error Handling** - Comprehensive exception management with error codes
- ✅ **DTO Pattern** - Clean data transfer with validation

### Frontend Development
- ✅ **Modern React** - Functional components with hooks (useState, useEffect, useRef)
- ✅ **Responsive Design** - Mobile-first approach with flexbox/grid
- ✅ **User Experience** - Loading states, error messages, success feedback
- ✅ **API Integration** - Axios for HTTP requests with error handling
- ✅ **State Management** - Clean state flow with proper prop drilling
- ✅ **Animations** - Smooth transitions and micro-interactions

### Integration & Security
- ✅ **OAuth 2.0 Flow** - Secure Google Calendar authentication
- ✅ **Token Management** - Persistent, user-specific token storage
- ✅ **CORS Configuration** - Proper cross-origin setup for development
- ✅ **Environment Isolation** - Separate configs for dev/production

## 📊 API Endpoints

### User Management

**Register User**
```http
POST /api/users/register
Content-Type: application/json

{
  "username": "John Doe",
  "email": "john@example.com"
}

Response: 200 OK
{
  "userId": "user_abc123",
  "username": "John Doe",
  "email": "john@example.com",
  "authenticated": false
}
```

**Get All Users**
```http
GET /api/users

Response: 200 OK
[
  {
    "userId": "user_abc123",
    "username": "John Doe",
    "email": "john@example.com",
    "authenticated": true
  }
]
```

### Event Management

**Parse Natural Language**
```http
POST /api/events/parse
Content-Type: application/json

{
  "text": "Schedule team meeting tomorrow at 2 PM"
}

Response: 200 OK
{
  "actionType": "CREATE",
  "title": "Team meeting",
  "date": "2025-01-07",
  "startTime": "14:00",
  "endTime": "15:00",
  "location": null,
  "successful": true
}
```

**Create Event**
```http
POST /api/events/create
Content-Type: application/json
User-Id: user_abc123

{
  "title": "Team meeting",
  "date": "2025-01-07",
  "startTime": "14:00",
  "endTime": "15:00",
  "location": "Conference Room A"
}

Response: 200 OK
{
  "success": true,
  "message": "Event created successfully!",
  "errorCode": null
}
```

## 🧪 Testing

### Backend Testing
```bash
# Run all tests
mvn test

# Run with coverage
mvn clean verify

# Run specific test class
mvn test -Dtest=CreateEventInteractorTest
```

### Frontend Testing
```bash
cd calendar-frontend

# Run tests
npm test

# Run with coverage
npm test -- --coverage
```

## 🎓 Learning Outcomes

This project demonstrates proficiency in:

### Software Engineering
- **Clean Architecture** - Proper layer separation and dependency management
- **SOLID Principles** - Professional OOP design
- **Design Patterns** - Gateway, Presenter, Factory, Repository
- **RESTful API Design** - Industry-standard backend architecture
- **Full-Stack Development** - Backend + frontend integration

### Technical Skills
- **Java Backend** - Spring Boot, Maven, dependency injection
- **Frontend Development** - React, modern JavaScript, responsive design
- **AI Integration** - Google Gemini API for NLP tasks
- **OAuth 2.0** - Secure authentication flows
- **API Consumption** - Google Calendar API integration
- **Error Handling** - Comprehensive exception management
- **Version Control** - Git workflow with proper .gitignore

### Professional Practices
- **Code Organization** - Clear project structure
- **Documentation** - Comprehensive README and code comments
- **Security** - Proper secret management, OAuth implementation
- **User Experience** - Intuitive UI with helpful feedback
- **Scalability** - Multi-user support with isolated data

## 🚧 Future Enhancements

- [ ] **View Schedule** - Display upcoming events with filters
- [ ] **Delete Events** - Remove events by title/date
- [ ] **Update Events** - Modify existing event details
- [ ] **Recurring Events** - Support for repeating events
- [ ] **Smart Suggestions** - AI-powered event recommendations
- [ ] **Calendar Export** - Export to .ics format
- [ ] **Email Notifications** - Event reminders via email
- [ ] **Team Calendars** - Shared calendars for groups
- [ ] **Mobile App** - Native iOS/Android applications
- [ ] **Voice Input** - Speech-to-text event creation

## 🤝 Contributing

This is an educational/portfolio project. Feedback and suggestions are welcome!

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Google Gemini AI** - Natural language processing
- **Google Calendar API** - Calendar integration
- **Spring Boot** - Backend framework
- **React** - Frontend framework
- **Clean Architecture principles** - Robert C. Martin

## 📞 Contact

For questions or feedback about this project, please open an issue on GitHub.

---

**Built with ❤️ as a demonstration of full-stack development, AI integration, and clean software architecture.**

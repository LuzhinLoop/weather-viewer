# Weather Viewer 🌦️

**Weather Viewer** is a learning-oriented web application for tracking current weather in user-selected locations.  
It features user registration, cookie-based authentication, and a simple UI for managing favorite cities.  
Under the hood, it follows a classic layered architecture (Controllers → Services → Repositories/DAOs → DB) with Flyway-powered migrations and server-side rendering.

## Features
- User registration and login
- Cookie-based session management
- Add/remove saved locations
- Current weather from the OpenWeather API
- Server-side templates for dynamic pages

## Tech Stack
- Java 17
- Spring MVC
- Hibernate 6
- PostgreSQL (H2 for tests)
- Flyway (database migrations)
- JTE (template engine)
- Gradle
- Tomcat 10+

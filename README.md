# 🌦️ 404_WeatherNotFound: Will It Rain On My Parade? 

> **An intelligent weather prediction platform for seamless outdoor event planning.**  
> *Proudly developed for the NASA Space Apps Challenge 2025.*

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Table of Contents

- [Overview](#-overview)
- [Screenshots](#-screenshots)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [Project Structure](#-project-structure)
- [Prerequisites](#-prerequisites)
- [Installation & Setup](#-installation--setup)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Running the Application](#-running-the-application)
- [API Documentation](#-api-documentation)
- [Environment Configuration](#-environment-configuration)
- [About the Hackathon](#-about-the-hackathon)
- [Contributing](#-contributing)
- [License](#-license)

## 🌟 Overview

"Will It Rain On My Parade?" is an intelligent weather prediction application designed to help users make informed decisions about outdoor events and activities. Built for the NASA Space Apps Challenge 2025, this application leverages advanced weather data and machine learning to provide personalized weather forecasts tailored for outdoor event planning.

The application addresses the critical need for accurate, event-specific weather predictions by combining multiple weather data sources with intelligent analysis to deliver actionable insights for event planners, outdoor enthusiasts, and anyone organizing weather-sensitive activities.

## 📸 Screenshots

| Login Page | Register Page |
| :---: | :---: |
| ![Login Page](https://via.placeholder.com/400x300.png?text=Login+Page) | ![Register Page](https://via.placeholder.com/400x300.png?text=Register+Page) |

| Dashboard | Location Management |
| :---: | :---: |
| ![Dashboard](https://via.placeholder.com/400x300.png?text=Dashboard) | ![Location Management](https://via.placeholder.com/400x300.png?text=Location+Management) |


## ✨ Features

- 🔐 **User Authentication & Profile Management**
- 📍 **Location-Based Weather Services**  
- 📊 **Analytics & Performance Metrics**
- 🌍 **Multi-Source Weather Data Integration**
- 📈 **Real-Time Weather Updates**

## 🛠️ Technology Stack

### Backend
- **Java 21** - Programming language
- **Spring Boot 3.5.5** - Application framework
- **Spring Security** - Authentication & authorization
- **Spring Data JPA** - Data persistence
- **MySQL 8.0+** - Primary database
- **Maven** - Dependency management
- **Swagger/OpenAPI 3** - API documentation

### Frontend
- **React 18.2.0** - UI framework
- **React Router DOM** - Client-side routing
- **Axios** - HTTP client
- **Modern CSS** - Styling

### Development Tools
- **Git** - Version control
- **Maven** - Build tool
- **npm/yarn** - Package management

## � Project Structure

The project is organized into two main parts: a Spring Boot backend and a React frontend.

```
/
├── frontend/              # React frontend application
│   ├── public/
│   └── src/
│       ├── components/    # Reusable React components
│       ├── pages/         # Page components
│       └── services/      # API service clients
├── src/                   # Spring Boot backend application
│   └── main/
│       └── java/
│           └── com/weather_found/weather_app/
│               ├── config/
│               └── modules/ # Business logic modules
└── pom.xml                # Backend dependencies (Maven)
```

## �📋 Prerequisites

Before running this application, make sure you have the following installed on your system:

### Required Software
- **Java 21** or higher ([Download](https://openjdk.org/projects/jdk/21/))
- **Maven 3.6+** ([Download](https://maven.apache.org/download.cgi))
- **Node.js 18+** and **npm** ([Download](https://nodejs.org/))
- **MySQL 8.0+** ([Download](https://dev.mysql.com/downloads/mysql/))
- **Git** ([Download](https://git-scm.com/downloads))

### Verify Installation
```bash
java --version
mvn --version
node --version
npm --version
mysql --version
```

## 🚀 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/Omar-Mega-Byte/404_WeatherNotFound.git
cd 404_WeatherNotFound
```

### 2. Database Setup
Connect to MySQL and run the following command:
```sql
CREATE DATABASE weather_app;
```

## Backend Setup

1.  **Navigate to Backend Directory**: The root of the project is the backend directory.
2.  **Configure Database Connection**: Edit `src/main/resources/application.yml` with your MySQL username and password.
3.  **Install Dependencies**: `mvn clean install`

## Frontend Setup

1.  **Navigate to Frontend Directory**: `cd frontend`
2.  **Install Dependencies**: `npm install`

## 🏃‍♂️ Running the Application

### Start Backend Server
From the project root, run:
```bash
mvn spring-boot:run
```
The backend will be available at `http://localhost:8080`.

### Start Frontend Server
From the `frontend` directory, run:
```bash
npm start
```
The frontend will be available at `http://localhost:3000`.

## 📚 API Documentation

Once the backend is running, interactive API documentation is available via Swagger UI:
- **URL**: http://localhost:8080/swagger-ui/index.html

## ⚙️ Environment Configuration

### Backend Environment Variables
You can configure the application using environment variables or by modifying the `application.yml` file.

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Backend server port |
| `MYSQL_USER` | `root` | MySQL username |
| `MYSQL_PASSWORD` | `your_password` | MySQL password |
| `JWT_SECRET` | `change-this-secret` | JWT signing key |

### Frontend Environment Variables
Create a `.env` file in the `frontend/` directory:
```env
REACT_APP_API_BASE_URL=http://localhost:8080
```

## 🏆 About the Hackathon

This project is a proud submission to the **NASA Space Apps Challenge 2025**. Our team, "404_WeatherNotFound", took on the challenge to harness NASA's vast repository of Earth observation data to solve a real-world problem: the uncertainty of weather for outdoor activities.

Our mission was to create a tool that empowers users to plan events with confidence, transforming complex atmospheric data into simple, actionable insights. We believe that by making weather prediction more personal and precise, we can help people connect more safely and enjoyably with the world around them. This project represents our passion for technology, data science, and the endless possibilities of space exploration.

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to get started.

1.  Fork the repository.
2.  Create a feature branch (`git checkout -b feature/your-feature`).
3.  Commit your changes (`git commit -m 'Add some feature'`).
4.  Push to the branch (`git push origin feature/your-feature`).
5.  Open a Pull Request.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

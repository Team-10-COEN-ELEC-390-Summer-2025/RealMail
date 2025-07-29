
![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/Team-10-COEN-ELEC-390-Summer-2025/RealMail?utm_source=oss&utm_medium=github&utm_campaign=Team-10-COEN-ELEC-390-Summer-2025%2FRealMail&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)



## RealMail 
The mail detector app, henceforth called RealMail, will detect motion and other potential signals of the mailbox to notify the user that the mailbox is opened, or new mail is received; additionally, image processing could make an estimate of what the contents of the mail is based on the general shape of the package.

### Team members 
| **Full Name**           | **Student ID** |
|-------------------------|----------------|
| Katrina Gallardo        | 40157957       |
| Javid Latchman          | 40214562       |
| Bertin Mihigo Sano      | 40157663       |
| Zaree Choudhry Hameed   | 21026488       |
| Daran Guo               | 40127349       |

### Project Structure
```
RealMail/
├── android/                # Android mobile application (Kotlin/Java)
│   ├── app/               # Main application module with source code, resources, and manifests
│   ├── build.gradle.kts   # Kotlin DSL build configuration for the project
│   ├── gradle/            # Gradle wrapper and version catalogs
│   └── libs.versions.toml # Centralized dependency version management
│
├── backend/                # Firebase Cloud Functions backend (Node.js/TypeScript)
│   ├── functions/         # Cloud Functions source code, package.json, and compiled output
│   ├── firebase.json      # Firebase project configuration and hosting rules
│   └── README.md          # Detailed API documentation for all endpoints
│
├── hardware/               # IoT sensor device code and configurations (Python/Node.js)
│   ├── src/               # Hardware interface and sensor communication modules
│   ├── openPIR_1.py       # PIR motion sensor integration script
│   ├── package.json       # Node.js dependencies for hardware communication
│   └── README.md          # Hardware setup and deployment instructions
│
├── WebRTC-server/          # Real-time video streaming server (Python Flask)
│   ├── src/               # WebRTC client-side JavaScript and HTML templates
│   ├── main.py            # Flask application with WebRTC signaling server
│   ├── requirements.txt   # Python dependencies for the streaming server
│   ├── Dockerfile         # Container configuration for cloud deployment
│   ├── cert.pem & key.pem # SSL certificates for HTTPS/WSS connections
│   └── cloudbuild.yaml    # Google Cloud Build configuration
│
├── database/               # Database schemas and migration scripts
│   └── (PostgreSQL table definitions and seed data)
│
├── .github/                # GitHub Actions CI/CD and automation
│   └── workflows/         # Automated testing, building, and deployment pipelines
│
├── .gitignore              # Git ignore rules for all project components
└── README.md               # Project overview, setup instructions, and team information
```

### System Architecture Overview

**RealMail** is a full-stack IoT solution consisting of:

🏠 **Hardware Layer**: Python-based PIR motion sensors that detect mailbox activity  
📱 **Mobile App**: Native Android application for user notifications and device management  
☁️ **Cloud Backend**: Firebase Functions handling data processing, authentication, and push notifications  
🎥 **Video Streaming**: WebRTC server for real-time mailbox monitoring  
🗄️ **Database**: PostgreSQL for sensor data, user accounts, and device registration  
🔄 **DevOps**: Automated CI/CD pipelines for testing and deployment

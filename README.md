# Zim PayPal Clone

A secure, production-ready PayPal clone built with Spring Boot, featuring a clean OOP architecture, Thymeleaf UI, and comprehensive payment functionality.

## Features

- ✅ User registration and authentication
- ✅ Secure account management with Spring Security
- ✅ Deposit money into accounts
- ✅ Link payment cards (Visa, Mastercard, Amex, Discover)
- ✅ Send money to other users
- ✅ Make payments from wallet or card
- ✅ Transaction history and statements
- ✅ Email and SMS notifications
- ✅ PayPal-inspired modern UI/UX
- ✅ RESTful API endpoints
- ✅ Flyway database migrations for easy upgrades

## Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: H2 (development) / PostgreSQL (production)
- **ORM**: Spring Data JPA
- **Security**: Spring Security with BCrypt password encoding
- **UI**: Thymeleaf templates
- **Migrations**: Flyway
- **Email**: Spring Mail
- **SMS**: Twilio integration
- **Build Tool**: Maven

## Architecture

The application follows clean OOP principles with proper separation of concerns:

- **Entities**: Domain models (User, Account, Transaction, Card, Statement, Notification)
- **Repositories**: Data access layer using Spring Data JPA
- **Services**: Business logic layer
- **Controllers**: Web and REST API endpoints
- **DTOs**: Data transfer objects for requests/responses
- **Security**: Custom user details service and security configuration

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- (Optional) PostgreSQL for production
- (Optional) Twilio account for SMS functionality
- (Optional) SMTP server for email functionality

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd zim-paypal
```

2. Build the project:
```bash
mvn clean install
```

3. Configure application properties (optional):
   - Edit `src/main/resources/application.yml`
   - Set email credentials: `MAIL_USERNAME`, `MAIL_PASSWORD`
   - Set Twilio credentials: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`
   - Configure JWT secret: `JWT_SECRET`

4. Run the application:
```bash
mvn spring-boot:run
```

5. Access the application:
   - Web UI: http://localhost:8080
   - H2 Console: http://localhost:8080/h2-console
     - JDBC URL: `jdbc:h2:mem:zimdb`
     - Username: `sa`
     - Password: (empty)

## Usage

### Register a New Account

1. Navigate to http://localhost:8080/register
2. Fill in your details (username, email, password, name)
3. Submit the form
4. Login with your credentials

### Features Overview

- **Dashboard**: View balance, recent transactions, and quick actions
- **Send Money**: Transfer funds to other users by email
- **Deposit**: Add money to your account
- **Link Cards**: Add payment cards for purchases
- **Pay**: Make payments using wallet or linked cards
- **Transactions**: View complete transaction history
- **Statements**: Generate and view account statements

## API Endpoints

### Authentication Required

All API endpoints require authentication. Use the web interface to login first.

### Available Endpoints

- `GET /api/account/balance` - Get account balance
- `POST /api/transfer` - Send money to another user
- `POST /api/deposit` - Deposit money
- `GET /api/transactions` - Get transaction history
- `GET /api/cards` - Get linked cards
- `POST /api/cards` - Link a new card
- `POST /api/pay` - Make a payment
- `GET /api/statements` - Get account statements
- `POST /api/statements/generate` - Generate a statement

## Database Migrations

The application uses Flyway for database versioning. Migrations are located in:
- `src/main/resources/db/migration/`

To add a new migration:
1. Create a new file: `V2__description.sql`
2. Add your SQL statements
3. Flyway will automatically apply it on next startup

## Security Features

- Password encryption with BCrypt
- Session-based authentication
- CSRF protection (can be enabled)
- Account locking after failed login attempts
- Email and phone verification support
- JWT token support for API (future enhancement)

## Configuration

### Email Configuration

Set these environment variables or add to `application.yml`:
```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-app-password
```

### SMS Configuration (Twilio)

Set these environment variables:
```bash
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
TWILIO_PHONE_NUMBER=your_twilio_number
```

### Database Configuration

For PostgreSQL, update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/zimdb
    username: your_username
    password: your_password
```

## Project Structure

```
src/
├── main/
│   ├── java/com/zim/paypal/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # Web and REST controllers
│   │   ├── model/
│   │   │   ├── dto/         # Data transfer objects
│   │   │   └── entity/      # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # Security configuration
│   │   └── service/         # Business logic layer
│   └── resources/
│       ├── db/migration/    # Flyway migrations
│       ├── static/css/      # CSS files
│       └── templates/       # Thymeleaf templates
└── test/                    # Test files
```

## Future Integrations

This application is designed with pure OOP Java principles to support future integrations:

- WordPress WooCommerce plugin
- Custom SDK for third-party integrations
- Payment gateway integrations (Stripe, PayPal, etc.)
- Mobile app API
- Webhook support

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

This project is for educational purposes.

## Support

For issues and questions, please open an issue on the repository.

# Zim PayPal - Comprehensive Payment Platform

A full-featured PayPal clone built with Spring Boot, featuring secure payment processing, multi-currency support, fraud detection, and comprehensive financial management tools.

## 🚀 Features

### Core Payment Features
- ✅ **User Registration & Authentication** - Secure user accounts with role-based access (USER, ADMIN, MERCHANT)
- ✅ **Account Management** - Multiple accounts per user with multi-currency support
- ✅ **Money Transfers** - Send money to other users via email
- ✅ **Deposits & Withdrawals** - Fund accounts and withdraw funds
- ✅ **Card Management** - Link and manage payment cards
- ✅ **Transaction History** - Complete transaction tracking and statements
- ✅ **Payment Links** - Create shareable payment links for easy payments
- ✅ **Invoicing System** - Create, send, and track invoices with line items

### Security & Compliance
- ✅ **Two-Factor Authentication (2FA)** - SMS, Email, and TOTP/App-based 2FA with backup codes
- ✅ **Anti-Money Laundering (AML)** - Automated AML compliance checks and reporting
- ✅ **Fraud Detection** - Real-time fraud detection with configurable rules
- ✅ **Risk Scoring** - Transaction and user risk assessment
- ✅ **Suspicious Activity Monitoring** - Automatic flagging and review workflow
- ✅ **KYC Verification** - Know Your Customer compliance with document verification
- ✅ **Transaction Limits** - Daily, weekly, and monthly transaction limits per user role

### Financial Management
- ✅ **Multi-Currency Support** - Support for multiple currencies with exchange rates
- ✅ **Exchange Rate Management** - Admin-configurable exchange rates with effective dates
- ✅ **Charges & Fees** - Configurable transaction charges (fixed, percentage, tiered)
- ✅ **Tax Management** - Tax rate configuration with regulation tracking
- ✅ **Account Limits** - Configurable limits on accounts per user and transaction amounts
- ✅ **Transaction Reversals** - User-initiated reversals with admin approval workflow

### Business Features
- ✅ **Money Requests** - Request money from other users with approval/decline workflow
- ✅ **Bill Splitting** - Split bills equally, by percentage, or custom amounts
- ✅ **Rewards & Points** - Earn and redeem rewards points with tier system
- ✅ **Service Purchases** - Buy airtime, data bundles, and ZESA tokens
- ✅ **Service Provider Integration** - Extensible provider system (Econet, NetOne, ZESA, etc.)

### Support & Administration
- ✅ **Technical Support System** - Ticket-based support with messaging
- ✅ **Admin Dashboard** - Comprehensive admin panel with statistics
- ✅ **User Management** - Admin user management and role assignment
- ✅ **Transaction Oversight** - Admin view of all transactions
- ✅ **Fraud Management** - Admin tools for reviewing suspicious activities
- ✅ **KYC Management** - Admin approval workflow for KYC verifications

### Notifications
- ✅ **Email Notifications** - Transaction, request, and system notifications
- ✅ **SMS Notifications** - SMS alerts for important transactions (Twilio integration)

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0
- **Database**: PostgreSQL (production) / H2 (development)
- **UI**: Thymeleaf templates with PayPal-inspired design
- **Security**: Spring Security with BCrypt password encoding
- **Migrations**: Flyway for database versioning
- **ORM**: Spring Data JPA / Hibernate
- **Email**: Spring Mail
- **SMS**: Twilio SDK
- **Build Tool**: Maven
- **Language**: Java (Pure OOP principles)

## 📋 Roadmap & Priorities

### High Priority (Next Features)
1. **Payment Gateway Integration** - Real payment processing with Stripe/PayPal APIs
2. **Recurring Payments/Subscriptions** - Auto-pay and subscription management
3. **QR Code Payments** - Generate QR codes for payment links
4. **Mobile App API** - RESTful API endpoints for mobile applications
5. **Webhooks** - Event webhooks for third-party integrations
6. **Payment Disputes** - Dispute management and resolution workflow

### Medium Priority
7. **Advanced Reporting** - Financial reports, analytics, and exports
8. **Bulk Payments** - Batch payment processing
9. **Payment Plans** - Installment payment plans
10. **Merchant Tools** - Payment buttons, checkout integration
11. **International Transfers** - Cross-border payment routing
12. **Account Statements** - PDF statement generation

### Low Priority (Future Enhancements)
13. **Cryptocurrency Support** - Bitcoin, Ethereum integration
14. **Investment Features** - Savings accounts, investment products
15. **Loyalty Programs** - Advanced loyalty and rewards
16. **Marketplace Integration** - E-commerce platform integration
17. **API Rate Limiting** - API usage limits and throttling
18. **Advanced Analytics** - Business intelligence and insights

## 🗄️ Database Schema

The application uses Flyway for database migrations. Key tables include:

- `users` - User accounts and authentication
- `accounts` - User wallet accounts (multi-currency)
- `transactions` - All financial transactions
- `cards` - Linked payment cards
- `currencies` - Supported currencies
- `exchange_rates` - Currency exchange rates
- `charges` - Transaction charges/fees
- `taxes` - Tax rates
- `account_limits` - Transaction and account limits
- `payment_links` - Shareable payment links
- `invoices` - Invoice management
- `invoice_items` - Invoice line items
- `two_factor_auth` - 2FA settings
- `risk_scores` - Risk assessments
- `suspicious_activities` - Flagged activities
- `fraud_rules` - Fraud detection rules
- `kyc_verifications` - KYC compliance
- `money_requests` - Money request workflow
- `bill_splits` - Bill splitting
- `rewards` - Rewards and points
- `support_tickets` - Support tickets
- `transaction_reversals` - Reversal requests
- `service_providers` - Service provider integration
- `service_purchases` - Service purchase tracking

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (for production)
- H2 Database (for development - embedded)

### Installation

1. Clone the repository:
```bash
git clone https://github.com/DexterWura/zim-paypal.git
cd zim-paypal
```

2. Configure application properties:
```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # For development
    # url: jdbc:postgresql://localhost:5432/zimpaypal  # For production
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-email@gmail.com
    password: your-password
  twilio:
    account-sid: your-account-sid
    auth-token: your-auth-token
    phone-number: your-phone-number
```

3. Build and run:
```bash
mvn clean install
mvn spring-boot:run
```

4. Access the application:
- Web UI: http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (development only)

### Default Admin Account
- Username: `admin`
- Password: `admin123` (change in production!)

## 📝 API Documentation

The application provides RESTful APIs for:
- User management
- Account operations
- Transaction processing
- Payment link creation
- Invoice management

API endpoints are available at `/api/**` (authentication required).

## 🔒 Security Features

- Password encryption with BCrypt
- Role-based access control (RBAC)
- Two-factor authentication (2FA)
- Fraud detection and risk scoring
- AML compliance checks
- Transaction limits and monitoring
- Suspicious activity flagging

## 🌍 Multi-Currency Support

- Support for multiple currencies (USD, ZWL, EUR, GBP, ZAR, etc.)
- Admin-configurable exchange rates
- Real-time currency conversion
- Multiple accounts per currency per user
- Currency-specific transaction limits

## 📊 Admin Features

- User management and role assignment
- Transaction monitoring and oversight
- Fraud rule configuration
- Suspicious activity review
- KYC verification approval
- Currency and exchange rate management
- Charge and tax configuration
- Account limit management
- Support ticket management
- Transaction reversal processing

## 🎯 Use Cases

- **Personal Payments**: Send/receive money, split bills, request payments
- **Business Payments**: Invoicing, payment links, merchant transactions
- **Service Purchases**: Airtime, data bundles, utility payments
- **International Transfers**: Multi-currency support with exchange rates
- **Compliance**: KYC verification, AML checks, fraud prevention

## 🤝 Contributing

This is a learning project. Contributions and suggestions are welcome!

## 📄 License

This project is for educational purposes.

## 👨‍💻 Author

Zim Development Team

---

**Note**: This is a comprehensive payment platform clone. For production use, ensure proper security hardening, compliance with financial regulations, and integration with real payment gateways.
